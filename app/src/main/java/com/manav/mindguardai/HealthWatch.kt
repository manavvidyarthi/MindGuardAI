package com.manav.mindguardai

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.CancellationException
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.reflect.KClass

/*
 * Smartwatch data via Android Health Connect.
 *
 * Galaxy Watch -> Samsung Health -> Health Connect -> MindGuard
 * Pixel Watch / Fitbit / many others also write to Health Connect.
 *
 * Read-only, and only the types the user approves in the system permission screen.
 * Nothing leaves the phone.
 */

internal enum class WatchAvailability { AVAILABLE, NEEDS_INSTALL, UNSUPPORTED }

internal data class WatchSnapshot(
    val heartRate: Long? = null,
    val heartRateAt: Instant? = null,
    val restingHeartRate: Long? = null,
    val hrvMs: Double? = null,
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val sleepHours: Double? = null,
    val stepsToday: Long? = null,
    val syncedAt: Instant = Instant.now()
) {
    val hasAnyData: Boolean
        get() = listOf(heartRate, restingHeartRate, hrvMs, systolic, sleepHours, stepsToday)
            .any { it != null }

    /**
     * Rough body-signal index 1..10 from HRV and heart rate above resting.
     * Lower HRV and higher HR-over-resting usually go with stress, but exercise,
     * caffeine or illness do the same. Not a diagnosis.
     */
    val bodyIndex: Double?
        get() {
            val parts = mutableListOf<Double>()
            hrvMs?.let { parts += ((60.0 - it) / 40.0).coerceIn(0.0, 1.0) }
            val hr = heartRate
            val rest = restingHeartRate
            if (hr != null && rest != null && rest > 0) {
                parts += ((hr - rest).toDouble() / (rest * 0.4)).coerceIn(0.0, 1.0)
            }
            return if (parts.isEmpty()) null else 1.0 + 9.0 * parts.average()
        }
}

internal data class WatchUiState(
    val availability: WatchAvailability,
    val connected: Boolean = false,
    val loading: Boolean = false,
    val snapshot: WatchSnapshot? = null,
    val error: String? = null
)

internal class HealthWatch(private val context: Context) {

    companion object {
        private const val PROVIDER = "com.google.android.apps.healthdata"

        val PERMISSIONS: Set<String> = setOf(
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(RestingHeartRateRecord::class),
            HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
            HealthPermission.getReadPermission(BloodPressureRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(StepsRecord::class)
        )

        fun permissionContract(): ActivityResultContract<Set<String>, Set<String>> =
            PermissionController.createRequestPermissionResultContract()
    }

    private val client: HealthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    fun availability(): WatchAvailability =
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> WatchAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> WatchAvailability.NEEDS_INSTALL
            else -> WatchAvailability.UNSUPPORTED
        }

    /** Play Store page for Health Connect (needed on Android 13 and below). */
    fun installIntent(): Intent =
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$PROVIDER&url=healthconnect%3A%2F%2Fonboarding")
        ).apply {
            setPackage("com.android.vending")
            putExtra("overlay", true)
            putExtra("callerId", context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    suspend fun grantedCount(): Int =
        client.permissionController.getGrantedPermissions().intersect(PERMISSIONS).size

    /** Reads the latest value of each type. A type without permission or data comes back null. */
    suspend fun read(): WatchSnapshot {
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant()

        val hrSample = safe {
            latest(HeartRateRecord::class, now.minus(Duration.ofHours(24)), now)
                ?.samples
                ?.maxByOrNull { it.time }
        }

        val resting = safe {
            latest(RestingHeartRateRecord::class, now.minus(Duration.ofDays(7)), now)
                ?.beatsPerMinute
        }

        val hrv = safe {
            latest(HeartRateVariabilityRmssdRecord::class, now.minus(Duration.ofHours(48)), now)
                ?.heartRateVariabilityMillis
        }

        val bp = safe {
            latest(BloodPressureRecord::class, now.minus(Duration.ofDays(30)), now)
        }

        val sleep = safe {
            latest(SleepSessionRecord::class, now.minus(Duration.ofHours(36)), now)
                ?.let { Duration.between(it.startTime, it.endTime).toMinutes() / 60.0 }
        }

        val steps = safe {
            client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )
            )[StepsRecord.COUNT_TOTAL]
        }

        return WatchSnapshot(
            heartRate = hrSample?.beatsPerMinute,
            heartRateAt = hrSample?.time,
            restingHeartRate = resting,
            hrvMs = hrv,
            systolic = bp?.systolic?.inMillimetersOfMercury?.toInt(),
            diastolic = bp?.diastolic?.inMillimetersOfMercury?.toInt(),
            sleepHours = sleep,
            stepsToday = steps,
            syncedAt = now
        )
    }

    private suspend fun <T : Record> latest(type: KClass<T>, start: Instant, end: Instant): T? =
        client.readRecords(
            ReadRecordsRequest(
                recordType = type,
                timeRangeFilter = TimeRangeFilter.between(start, end),
                ascendingOrder = false,
                pageSize = 1
            )
        ).records.firstOrNull()

    private suspend fun <T> safe(block: suspend () -> T?): T? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null // no permission for this type, or no data
        }
}