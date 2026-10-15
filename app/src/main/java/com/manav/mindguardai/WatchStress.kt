package com.manav.mindguardai

import android.content.Context
import java.time.ZoneId
import java.util.Random
import kotlin.math.exp

/*
 * Watch-only stress model.
 *
 * Reads only smartwatch signals (sleep, HRV, heart rate vs resting, activity),
 * so it can estimate stress automatically, without a check-in.
 *
 * Trained with federated averaging:
 *  - 3 simulated devices with synthetic watch data (pre-trains it so it works on day one)
 *  - "Your phone": every time you save a check-in while wearing the watch, that day's watch
 *    signals + your stress rating become one real training example, stored only on this phone.
 */

internal object WatchFeatures {
    val names = listOf(
        "Bias",
        "Short sleep",
        "Low HRV",
        "Heart rate above resting",
        "Low activity"
    )
    const val SIZE = 5

    /** Value used for a signal the watch didn't provide. */
    private const val NEUTRAL = 0.35

    /** Feature vector + number of real signals used. Null if fewer than 2 signals are available. */
    fun of(s: WatchSnapshot): Pair<DoubleArray, Int>? {
        var used = 0

        fun pick(v: Double?): Double =
            if (v != null) {
                used++
                v.coerceIn(0.0, 1.0)
            } else {
                NEUTRAL
            }

        val sleep = pick(s.sleepHours?.let { (8.0 - it) / 8.0 })
        val hrv = pick(s.hrvMs?.let { (60.0 - it) / 40.0 })

        val hr = s.heartRate
        val rest = s.restingHeartRate
        val hrOver = pick(
            if (hr != null && rest != null && rest > 0) (hr - rest).toDouble() / (rest * 0.4) else null
        )

        // Steps are judged against how much of the day has passed
        val activity = pick(s.stepsToday?.let { steps ->
            val hour = s.syncedAt.atZone(ZoneId.systemDefault()).hour
            val expected = 8000.0 * ((hour - 7).coerceIn(2, 15) / 15.0)
            1.0 - steps / expected
        })

        if (used < 2) return null
        return doubleArrayOf(1.0, sleep, hrv, hrOver, activity) to used
    }
}

internal data class WatchModel(
    val weights: List<Double> = List(WatchFeatures.SIZE) { 0.0 },
    val rounds: Int = 0
)

internal data class WatchEstimate(
    /** Estimated stress 1..10 */
    val stress: Double,
    /** What pushes it up, biggest first */
    val factors: List<Factor>,
    /** How many of the 4 watch signals were available */
    val signals: Int
)

internal object WatchStress {

    private val simulated: List<List<StressSample>> by lazy {
        listOf(
            synth(51L, 60, 0.30),   // busy student
            synth(67L, 45, 0.10),   // shift worker
            synth(83L, 50, -0.40)   // calm routine
        )
    }

    fun estimate(model: WatchModel, snap: WatchSnapshot): WatchEstimate? {
        if (model.rounds == 0) return null
        val (x, used) = WatchFeatures.of(snap) ?: return null
        val w = model.weights

        val stress = 1.0 + 9.0 * sigmoid(dot(w.toDoubleArray(), x))
        val factors = (1 until WatchFeatures.SIZE)
            .map { Factor(WatchFeatures.names[it], w[it] * x[it]) }
            .sortedByDescending { it.impact }

        return WatchEstimate(stress, factors, used)
    }

    /** Runs federated rounds: local training on every device, then sample-weighted FedAvg. */
    fun train(model: WatchModel, userData: List<StressSample>, rounds: Int): WatchModel {
        var w = model.weights.toDoubleArray()
        val clients = if (userData.isNotEmpty()) simulated + listOf(userData) else simulated

        repeat(rounds) {
            val locals = clients.map { localTrain(w, it) }
            val total = clients.sumOf { it.size }.toDouble()
            w = DoubleArray(w.size) { i ->
                clients.indices.sumOf { k -> locals[k][i] * clients[k].size } / total
            }
        }

        return WatchModel(w.toList(), model.rounds + rounds)
    }

    private fun synth(seed: Long, n: Int, bias: Double): List<StressSample> {
        val r = Random(seed)
        return List(n) {
            val sleepH = 4.0 + r.nextDouble() * 5.5
            val hrv = 15.0 + r.nextDouble() * 75.0
            val rest = 55 + r.nextInt(20)
            val hr = rest + r.nextInt(35)
            val stepsFrac = r.nextDouble() * 1.2

            val x = doubleArrayOf(
                1.0,
                ((8.0 - sleepH) / 8.0).coerceIn(0.0, 1.0),
                ((60.0 - hrv) / 40.0).coerceIn(0.0, 1.0),
                ((hr - rest).toDouble() / (rest * 0.4)).coerceIn(0.0, 1.0),
                (1.0 - stepsFrac).coerceIn(0.0, 1.0)
            )

            val logit = -2.2 + 2.2 * x[1] + 2.0 * x[2] + 1.4 * x[3] + 0.9 * x[4] +
                    bias + r.nextGaussian() * 0.45

            StressSample(x, sigmoid(logit))
        }
    }

    private fun localTrain(start: DoubleArray, data: List<StressSample>): DoubleArray {
        val w = start.copyOf()
        repeat(15) {
            val g = DoubleArray(w.size)
            for (s in data) {
                val err = sigmoid(dot(w, s.x)) - s.y
                for (i in w.indices) g[i] += err * s.x[i]
            }
            for (i in w.indices) {
                val l2 = if (i == 0) 0.0 else 0.002 * w[i]
                w[i] -= 0.9 * (g[i] / data.size + l2)
            }
        }
        return w
    }

    private fun dot(w: DoubleArray, x: DoubleArray): Double =
        w.indices.sumOf { w[it] * x[it] }

    private fun sigmoid(v: Double): Double =
        1.0 / (1.0 + exp(-v.coerceIn(-30.0, 30.0)))
}

/** Saves the watch model and your own watch-labelled examples on this phone only. */
internal object WatchStressStore {
    private const val PREFS = "mindguard_demo"
    private const val MAX_SAMPLES = 60

    /** Loads the model; on first launch it is pre-trained with 8 federated rounds. */
    fun loadOrPretrain(context: Context): WatchModel {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val weights = p.getString("w_weights", null)
            ?.split(',')
            ?.mapNotNull { it.toDoubleOrNull() }
            ?.takeIf { it.size == WatchFeatures.SIZE }

        if (weights != null) return WatchModel(weights, p.getInt("w_rounds", 0))

        val model = WatchStress.train(WatchModel(), samples(context), rounds = 8)
        save(context, model)
        return model
    }

    fun save(context: Context, model: WatchModel) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("w_weights", model.weights.joinToString(","))
            .putInt("w_rounds", model.rounds)
            .apply()
    }

    fun addSample(context: Context, x: DoubleArray, y: Double) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val rows = p.getString("w_samples", "").orEmpty()
            .split(';')
            .filter { it.isNotBlank() } + (x.joinToString(",") + "|" + y)

        p.edit().putString("w_samples", rows.takeLast(MAX_SAMPLES).joinToString(";")).apply()
    }

    fun samples(context: Context): List<StressSample> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("w_samples", "").orEmpty()
            .split(';')
            .mapNotNull { row ->
                val parts = row.split('|')
                if (parts.size != 2) return@mapNotNull null
                val x = parts[0].split(',').mapNotNull { it.toDoubleOrNull() }
                val y = parts[1].toDoubleOrNull()
                if (x.size != WatchFeatures.SIZE || y == null) null
                else StressSample(x.toDoubleArray(), y)
            }
}