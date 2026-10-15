package com.manav.mindguardai

import android.content.Context
import java.util.Random
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/*
 * MindGuard federated learning engine.
 *
 * Model: logistic regression with soft labels -> predicts a stress score (1-10)
 * from mood, sleep and energy, and explains which signal pushes it up.
 *
 * Clients in every round:
 *  - "Your phone"  -> REAL client, trains on your own saved check-ins (data never leaves the device)
 *  - 3 simulated devices with synthetic data (stand-ins for other users)
 *
 * One round = local training on every client
 *           -> optional clip + Gaussian noise on each update (DP-style demo)
 *           -> FedAvg (sample-weighted average of weights)
 *           -> FedProx-style personalization of the global model on your data
 */

/** One training example: x = features from a check-in, y = stress scaled to 0..1 */
internal class StressSample(val x: DoubleArray, val y: Double)

internal object StressFeatures {
    val names = listOf(
        "Bias",
        "Low mood",
        "Sleep debt",
        "Low energy",
        "Low mood + poor sleep"
    )

    val size: Int get() = names.size

    fun of(mood: Int, sleep: Int, energy: Int): DoubleArray {
        val lowMood = (5 - mood).coerceIn(0, 4) / 4.0
        val sleepDebt = (8 - sleep).coerceIn(0, 8) / 8.0
        val lowEnergy = (10 - energy).coerceIn(0, 9) / 9.0
        return doubleArrayOf(1.0, lowMood, sleepDebt, lowEnergy, lowMood * sleepDebt)
    }

    /** Self-reported stress 1..10 -> 0..1 */
    fun label(stress: Int): Double = (stress - 1).coerceIn(0, 9) / 9.0
}

internal data class FedClientInfo(
    val id: String,
    val name: String,
    val subtitle: String,
    val isReal: Boolean,
    val samples: Int
)

internal data class ClientUpdate(
    val client: FedClientInfo,
    val weights: List<Double>,
    val localLoss: Double
)

internal data class RoundReport(
    val round: Int,
    val updates: List<ClientUpdate>,
    /** Mean absolute error in stress points on the held-out set */
    val mae: Double,
    /** Fraction of held-out predictions within ±1.5 stress points */
    val within: Double,
    /** Error of the (personalized) model on the user's own entries */
    val userMae: Double?,
    val privacyNoise: Boolean
)

internal data class FedModel(
    val global: List<Double> = List(StressFeatures.size) { 0.0 },
    val personal: List<Double>? = null,
    val rounds: Int = 0,
    val maeHistory: List<Double> = emptyList(),
    val lastReport: RoundReport? = null
) {
    val trained: Boolean get() = rounds > 0
}

internal data class Factor(val name: String, val impact: Double)

internal object FederatedEngine {
    private const val LOCAL_EPOCHS = 15
    private const val LOCAL_LR = 0.9
    private const val L2 = 0.002
    private const val CLIP = 1.0
    private const val NOISE_STD = 0.06

    private class Client(val info: FedClientInfo, val data: List<StressSample>)

    private val simulated: List<Client> by lazy {
        listOf(
            // Three demo users with different (synthetic) habits, so the data is not identical:
            // User 1 sleeps a bit less, User 2 much less, User 3 sleeps well and is calmer.
            synth("user1", "User 1", "Demo data", 11L, 48, 0.35, -1, 0),
            synth("user2", "User 2", "Demo data", 23L, 36, 0.15, -2, 0),
            synth("user3", "User 3", "Demo data", 37L, 40, -0.45, 1, 1)
        )
    }

    private val validation: List<StressSample> by lazy {
        synth("val", "Validation", "", 99L, 60, 0.0, 0, 0).data
    }

    val simulatedClients: List<FedClientInfo>
        get() = simulated.map { it.info }

    private fun userInfo(samples: Int) = FedClientInfo(
        id = "you",
        name = "Your phone",
        subtitle = "Real check-ins, stays on device",
        isReal = true,
        samples = samples
    )

    /** One full federated round. */
    fun runRound(
        model: FedModel,
        userData: List<StressSample>,
        privacyNoise: Boolean
    ): FedModel {
        val global = model.global.toDoubleArray()

        val clients = buildList {
            if (userData.isNotEmpty()) add(Client(userInfo(userData.size), userData))
            addAll(simulated)
        }

        val rnd = Random(1000L + model.rounds)

        // 1) Local training on each device, starting from the current global model
        val localWeights = clients.map { c ->
            val w = localTrain(global, c.data, LOCAL_EPOCHS, LOCAL_LR)
            if (privacyNoise) privatize(global, w, rnd) else w
        }

        val updates = clients.mapIndexed { i, c ->
            ClientUpdate(c.info, localWeights[i].toList(), loss(localWeights[i], c.data))
        }

        // 2) FedAvg: weighted by number of samples on each device
        val total = clients.sumOf { it.data.size }.toDouble()
        val newGlobal = DoubleArray(global.size) { i ->
            clients.indices.sumOf { k -> localWeights[k][i] * clients[k].data.size } / total
        }

        // 3) Personalization on the user's own data (stays on the phone)
        val personal =
            if (userData.size >= 2) {
                localTrain(newGlobal, userData, 40, 0.5, anchor = newGlobal, mu = 0.15)
            } else {
                null
            }

        val (mae, within) = evaluate(newGlobal, validation)
        val userMae =
            if (userData.isNotEmpty()) evaluate(personal ?: newGlobal, userData).first else null

        val round = model.rounds + 1

        return FedModel(
            global = newGlobal.toList(),
            personal = personal?.toList(),
            rounds = round,
            maeHistory = (model.maeHistory + mae).takeLast(40),
            lastReport = RoundReport(round, updates, mae, within, userMae, privacyNoise)
        )
    }

    fun train(
        model: FedModel,
        userData: List<StressSample>,
        rounds: Int,
        privacyNoise: Boolean = false
    ): FedModel {
        var m = model
        repeat(rounds) { m = runRound(m, userData, privacyNoise) }
        return m
    }

    /** Estimated stress 1..10, or null if the model has not been trained yet. */
    fun predictStress(model: FedModel, mood: Int, sleep: Int, energy: Int): Double? {
        if (!model.trained) return null
        val w = (model.personal ?: model.global).toDoubleArray()
        return 1.0 + 9.0 * sigmoid(dot(w, StressFeatures.of(mood, sleep, energy)))
    }

    /** Contribution of each signal (weight × feature) for one check-in, biggest first. */
    fun explain(model: FedModel, mood: Int, sleep: Int, energy: Int): List<Factor> {
        val w = model.personal ?: model.global
        val x = StressFeatures.of(mood, sleep, energy)
        return (1 until x.size)
            .map { Factor(StressFeatures.names[it], w[it] * x[it]) }
            .sortedByDescending { it.impact }
    }

    /** Learned global weights (how strongly each signal raises predicted stress). */
    fun importance(model: FedModel): List<Factor> =
        (1 until StressFeatures.size).map {
            Factor(StressFeatures.names[it], model.global[it])
        }

    // ---------------- internals ----------------

    private fun synth(
        id: String,
        name: String,
        subtitle: String,
        seed: Long,
        n: Int,
        bias: Double,
        sleepShift: Int,
        moodShift: Int
    ): Client {
        val r = Random(seed)
        val data = List(n) {
            val mood = (1 + r.nextInt(5) + moodShift).coerceIn(1, 5)
            val sleep = (4 + r.nextInt(6) + sleepShift).coerceIn(0, 12)
            val energy = (1 + r.nextInt(10) + (sleep - 6) / 2).coerceIn(1, 10)
            val x = StressFeatures.of(mood, sleep, energy)
            val logit = -2.4 + 2.1 * x[1] + 2.4 * x[2] + 1.6 * x[3] + 1.2 * x[4] +
                    bias + r.nextGaussian() * 0.45
            StressSample(x, sigmoid(logit))
        }
        return Client(FedClientInfo(id, name, subtitle, false, n), data)
    }

    private fun localTrain(
        start: DoubleArray,
        data: List<StressSample>,
        epochs: Int,
        lr: Double,
        anchor: DoubleArray? = null,
        mu: Double = 0.0
    ): DoubleArray {
        val w = start.copyOf()
        repeat(epochs) {
            val g = DoubleArray(w.size)
            for (s in data) {
                val err = sigmoid(dot(w, s.x)) - s.y
                for (i in w.indices) g[i] += err * s.x[i]
            }
            for (i in w.indices) {
                val l2 = if (i == 0) 0.0 else L2 * w[i]
                val prox = if (anchor != null) mu * (w[i] - anchor[i]) else 0.0
                w[i] -= lr * (g[i] / data.size + l2 + prox)
            }
        }
        return w
    }

    /** Clip the update's L2 norm and add Gaussian noise (illustrative, no formal DP guarantee). */
    private fun privatize(global: DoubleArray, local: DoubleArray, rnd: Random): DoubleArray {
        val delta = DoubleArray(local.size) { local[it] - global[it] }
        val norm = sqrt(delta.sumOf { it * it })
        val scale = if (norm > CLIP) CLIP / norm else 1.0
        return DoubleArray(local.size) {
            global[it] + delta[it] * scale + rnd.nextGaussian() * NOISE_STD
        }
    }

    private fun loss(w: DoubleArray, data: List<StressSample>): Double =
        data.map { s ->
            val p = sigmoid(dot(w, s.x)).coerceIn(1e-7, 1 - 1e-7)
            -(s.y * ln(p) + (1 - s.y) * ln(1 - p))
        }.average()

    private fun evaluate(w: DoubleArray, data: List<StressSample>): Pair<Double, Double> {
        var absSum = 0.0
        var within = 0
        for (s in data) {
            val e = abs(9.0 * (sigmoid(dot(w, s.x)) - s.y))
            absSum += e
            if (e <= 1.5) within++
        }
        return absSum / data.size to within.toDouble() / data.size
    }

    private fun dot(w: DoubleArray, x: DoubleArray): Double =
        w.indices.sumOf { w[it] * x[it] }

    private fun sigmoid(v: Double): Double =
        1.0 / (1.0 + exp(-v.coerceIn(-30.0, 30.0)))
}

/** Saves the trained model locally so the stress insight survives app restarts. */
internal object ModelStore {
    private const val PREFS = "mindguard_demo"

    fun load(context: Context): FedModel {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val global = p.getString("fl_global", null)?.toDoubles()
        if (global == null || global.size != StressFeatures.size) return FedModel()

        return FedModel(
            global = global,
            personal = p.getString("fl_personal", null)?.toDoubles()
                ?.takeIf { it.size == StressFeatures.size },
            rounds = p.getInt("fl_rounds", 0),
            maeHistory = p.getString("fl_mae", null)?.toDoubles() ?: emptyList()
        )
    }

    fun save(context: Context, model: FedModel) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            putString("fl_global", model.global.joinToString(","))
            if (model.personal != null) {
                putString("fl_personal", model.personal.joinToString(","))
            } else {
                remove("fl_personal")
            }
            putInt("fl_rounds", model.rounds)
            putString("fl_mae", model.maeHistory.joinToString(","))
        }.apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove("fl_global")
            .remove("fl_personal")
            .remove("fl_rounds")
            .remove("fl_mae")
            .apply()
    }

    private fun String.toDoubles(): List<Double>? =
        if (isBlank()) emptyList()
        else runCatching { split(',').map { it.toDouble() } }.getOrNull()
}