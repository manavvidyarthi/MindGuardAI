package com.manav.mindguardai

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/*
 * Google sign-in + family circle.
 *
 * Adding a member: I enter their Gmail -> the app creates a 6-digit OTP -> I send it to them.
 * They sign in with THAT Gmail, open Family and type the OTP -> they join.
 * Firestore rules check both the signed-in email and the OTP, so nobody else can join.
 *
 * Firestore (collections prefixed mg_):
 *   mg_users/{uid}               name, email, circleId, share{...}, live{...}
 *   mg_circles/{id}              owner, members[uid], lastJoinOtp
 *   mg_member_invites/{email}    circleId, otp, invitedBy, inviterName, expiresAt (24 h)
 */

/** Firebase console -> Authentication -> Sign-in method -> Google -> Web SDK configuration -> Web client ID */
internal const val WEB_CLIENT_ID =
    "778610427008-tpf03dh3su1b2dscjlliu4fju1vrtg7b.apps.googleusercontent.com"

internal data class Account(val uid: String, val name: String, val email: String)

internal data class ShareSettings(
    val stress: Boolean = true,
    val heart: Boolean = true,
    val sleep: Boolean = true,
    val steps: Boolean = true,
    val bp: Boolean = false
) {
    fun toMap(): Map<String, Boolean> = mapOf(
        "stress" to stress,
        "heart" to heart,
        "sleep" to sleep,
        "steps" to steps,
        "bp" to bp
    )

    companion object {
        fun from(raw: Any?): ShareSettings {
            val m = raw as? Map<*, *> ?: return ShareSettings()
            fun b(k: String, d: Boolean) = m[k] as? Boolean ?: d
            return ShareSettings(
                stress = b("stress", true),
                heart = b("heart", true),
                sleep = b("sleep", true),
                steps = b("steps", true),
                bp = b("bp", false)
            )
        }
    }
}

internal data class FamilyMember(
    val uid: String,
    val name: String,
    val email: String?,
    val isMe: Boolean,
    val stress: Double?,
    val heartRate: Long?,
    val restingHr: Long?,
    val hrv: Double?,
    val sleepHours: Double?,
    val steps: Long?,
    val systolic: Int?,
    val diastolic: Int?,
    /** epoch millis of their last update */
    val updatedAt: Long?
)

/** Someone invited me (shown so I can type the OTP). */
internal data class PendingInvite(val inviterName: String, val circleId: String)

/** An invite I just sent (shown so I can pass the OTP on). */
internal data class SentInvite(val email: String, val otp: String)

internal data class FamilyState(
    val ready: Boolean = false,
    val myName: String = "",
    val circleId: String? = null,
    val share: ShareSettings = ShareSettings(),
    val members: List<FamilyMember> = emptyList(),
    val pending: PendingInvite? = null,
    val sent: SentInvite? = null,
    val busy: Boolean = false,
    val error: String? = null
)

/** Turns sign-in exceptions into something a person can act on. */
internal fun friendlySignInError(e: Exception): String = when {
    e is GetCredentialCancellationException -> "Sign-in cancelled."
    e is NoCredentialException ->
        "No Google account found on this phone. Add one in Settings > Accounts, then try again."
    WEB_CLIENT_ID.startsWith("PASTE") ->
        "Google sign-in isn't set up yet: the Web client ID is missing in FamilyCircle.kt."
    (e.message ?: "").contains("10:") || (e.message ?: "").contains("DEVELOPER_ERROR") ->
        "Google sign-in isn't set up yet: add this computer's SHA-1 to the Firebase Android app."
    else -> e.message ?: "Sign-in failed. Check your internet and try again."
}

internal class FamilyCircle {
    // lazy: if Firebase isn't configured, the app still opens and shows an error instead of crashing
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val users by lazy { db.collection("mg_users") }
    private val circles by lazy { db.collection("mg_circles") }
    private val memberInvites by lazy { db.collection("mg_member_invites") }

    // ---------------- account ----------------

    /** The signed-in Google account, or null (anonymous test accounts don't count). */
    fun account(): Account? {
        val u = auth.currentUser ?: return null
        val email = u.email ?: return null
        if (u.isAnonymous) return null
        return Account(
            uid = u.uid,
            name = u.displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore('@'),
            email = email.lowercase()
        )
    }

    /** Shows the Google account picker and signs in to Firebase with the chosen account. */
    suspend fun signInWithGoogle(activity: Context): Account {
        val option = GetSignInWithGoogleOption.Builder(WEB_CLIENT_ID).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = CredentialManager.create(activity).getCredential(activity, request).credential

        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            throw IllegalStateException("Google sign-in returned an unexpected credential.")
        }

        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()

        val acc = account() ?: throw IllegalStateException("Google sign-in failed.")
        users.document(acc.uid).set(
            mapOf("name" to acc.name, "email" to acc.email),
            SetOptions.merge()
        ).await()
        return acc
    }

    suspend fun signOut(activity: Context) {
        auth.signOut()
        runCatching {
            CredentialManager.create(activity).clearCredentialState(ClearCredentialStateRequest())
        }
    }

    // ---------------- circle ----------------

    /** Restores circle, share settings and any invite waiting for me. */
    suspend fun restore(acc: Account): FamilyState {
        val doc = users.document(acc.uid).get().await()
        val circleId = doc.getString("circleId")
        return FamilyState(
            ready = true,
            myName = doc.getString("name")?.takeIf { it.isNotBlank() } ?: acc.name,
            circleId = circleId,
            share = ShareSettings.from(doc.get("share")),
            pending = pendingInvite(acc, circleId)
        )
    }

    private suspend fun pendingInvite(acc: Account, myCircle: String?): PendingInvite? {
        val inv = memberInvites.document(acc.email).get().await()
        if (!inv.exists()) return null
        val cid = inv.getString("circleId") ?: return null
        if ((inv.getLong("expiresAt") ?: 0L) < System.currentTimeMillis()) return null
        if (cid == myCircle) return null
        return PendingInvite(inv.getString("inviterName") ?: "Someone", cid)
    }

    /** My circle id; creates the circle the first time I add someone. */
    private suspend fun ensureCircle(acc: Account): String {
        users.document(acc.uid).get().await().getString("circleId")?.let { return it }

        val ref = circles.document()
        ref.set(
            mapOf(
                "owner" to acc.uid,
                "members" to listOf(acc.uid),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
        users.document(acc.uid).set(
            mapOf(
                "name" to acc.name,
                "email" to acc.email,
                "circleId" to ref.id,
                "share" to ShareSettings().toMap()
            ),
            SetOptions.merge()
        ).await()
        return ref.id
    }

    /** Creates a 24-hour OTP for this Gmail. Returns my circle id and the invite to pass on. */
    suspend fun inviteMember(acc: Account, email: String): Pair<String, SentInvite> {
        val target = email.trim().lowercase()
        if (!target.contains("@") || !target.contains(".")) {
            throw IllegalArgumentException("Enter a full Gmail address, like mummy@gmail.com")
        }
        if (target == acc.email) throw IllegalArgumentException("That's your own email.")

        val circleId = ensureCircle(acc)
        val otp = (100000..999999).random().toString()

        memberInvites.document(target).set(
            mapOf(
                "circleId" to circleId,
                "otp" to otp,
                "invitedBy" to acc.uid,
                "inviterName" to acc.name,
                "expiresAt" to System.currentTimeMillis() + 24L * 3600 * 1000
            )
        ).await()

        return circleId to SentInvite(target, otp)
    }

    /** Joins the circle I was invited to, if the OTP matches. */
    suspend fun acceptInvite(acc: Account, otp: String): String {
        val ref = memberInvites.document(acc.email)
        val inv = ref.get().await()
        if (!inv.exists()) throw IllegalArgumentException("No invite found for ${acc.email}.")
        if ((inv.getLong("expiresAt") ?: 0L) < System.currentTimeMillis()) {
            throw IllegalArgumentException("This invite has expired. Ask for a new one.")
        }
        if (inv.getString("otp") != otp.trim()) throw IllegalArgumentException("Wrong OTP. Check the 6 digits.")
        val circleId = inv.getString("circleId") ?: throw IllegalArgumentException("Invite is broken. Ask for a new one.")

        // leave my old circle first, if I had one
        val old = users.document(acc.uid).get().await().getString("circleId")
        if (old != null && old != circleId) {
            runCatching { circles.document(old).update("members", FieldValue.arrayRemove(acc.uid)).await() }
        }

        circles.document(circleId).update(
            mapOf(
                "members" to FieldValue.arrayUnion(acc.uid),
                "lastJoinOtp" to otp.trim()
            )
        ).await()

        users.document(acc.uid).set(
            mapOf(
                "name" to acc.name,
                "email" to acc.email,
                "circleId" to circleId,
                "share" to ShareSettings().toMap()
            ),
            SetOptions.merge()
        ).await()

        runCatching { ref.delete().await() }
        return circleId
    }

    /** Removes someone from my circle. They immediately lose access (rules check circle membership). */
    suspend fun removeMember(circleId: String, uid: String) {
        circles.document(circleId).update("members", FieldValue.arrayRemove(uid)).await()
    }

    /** Called on a phone that was removed from a circle: forget it and stop sharing. */
    suspend fun clearMyCircle() {
        val me = auth.currentUser?.uid ?: return
        users.document(me).set(mapOf("circleId" to null, "live" to null), SetOptions.merge()).await()
    }

    suspend fun leave(circleId: String) {
        val me = auth.currentUser?.uid ?: return
        circles.document(circleId).update("members", FieldValue.arrayRemove(me)).await()
        users.document(me).set(mapOf("circleId" to null, "live" to null), SetOptions.merge()).await()
    }

    suspend fun saveShare(share: ShareSettings) {
        val me = auth.currentUser?.uid ?: return
        users.document(me).set(mapOf("share" to share.toMap()), SetOptions.merge()).await()
    }

    /** Pushes my latest readings. Anything I don't share is written as null. */
    suspend fun pushLive(share: ShareSettings, stress: Double?, snap: WatchSnapshot?) {
        val me = auth.currentUser?.uid ?: return
        val live = mapOf(
            "stress" to if (share.stress) stress else null,
            "heartRate" to if (share.heart) snap?.heartRate else null,
            "restingHr" to if (share.heart) snap?.restingHeartRate else null,
            "hrv" to if (share.heart) snap?.hrvMs else null,
            "sleepHours" to if (share.sleep) snap?.sleepHours else null,
            "steps" to if (share.steps) snap?.stepsToday else null,
            "systolic" to if (share.bp) snap?.systolic else null,
            "diastolic" to if (share.bp) snap?.diastolic else null,
            "updatedAt" to System.currentTimeMillis()
        )
        users.document(me).set(mapOf("live" to live), SetOptions.merge()).await()
    }

    /** Live list of everyone in the circle; updates the moment anyone's watch syncs. */
    fun members(circleId: String): Flow<List<FamilyMember>> = callbackFlow {
        val me = auth.currentUser?.uid
        val userListeners = mutableMapOf<String, ListenerRegistration>()
        val latest = mutableMapOf<String, FamilyMember>()
        var order = emptyList<String>()

        fun publish() {
            trySend(order.mapNotNull { latest[it] })
        }

        val circleListener = circles.document(circleId).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
                return@addSnapshotListener
            }
            val ids = (snap?.get("members") as? List<*>)?.filterIsInstance<String>().orEmpty()
            order = ids

            (userListeners.keys - ids.toSet()).forEach { gone ->
                userListeners.remove(gone)?.remove()
                latest.remove(gone)
            }

            ids.filter { it !in userListeners }.forEach { id ->
                userListeners[id] = users.document(id).addSnapshotListener { doc, _ ->
                    if (doc != null && doc.exists()) {
                        latest[id] = doc.toMember(id == me)
                        publish()
                    }
                }
            }
            publish()
        }

        awaitClose {
            circleListener.remove()
            userListeners.values.forEach { it.remove() }
        }
    }
}

private fun DocumentSnapshot.toMember(isMe: Boolean): FamilyMember {
    val live = get("live") as? Map<*, *>
    fun num(key: String) = live?.get(key) as? Number

    return FamilyMember(
        uid = id,
        name = getString("name")?.takeIf { it.isNotBlank() } ?: "Member",
        email = getString("email"),
        isMe = isMe,
        stress = num("stress")?.toDouble(),
        heartRate = num("heartRate")?.toLong(),
        restingHr = num("restingHr")?.toLong(),
        hrv = num("hrv")?.toDouble(),
        sleepHours = num("sleepHours")?.toDouble(),
        steps = num("steps")?.toLong(),
        systolic = num("systolic")?.toInt(),
        diastolic = num("diastolic")?.toInt(),
        updatedAt = num("updatedAt")?.toLong()
    )
}

/** Small await for Play-services Tasks, so no extra coroutines dependency is needed. */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
}