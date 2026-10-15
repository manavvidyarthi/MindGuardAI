package com.manav.mindguardai.data

import kotlin.math.exp
import kotlin.math.ln

/**
 * A genuine, tiny logistic-regression + FedAvg exercise over synthetic examples.
 * It runs in this app process; it is not device-to-device production federated learning.
 */
data class FederatedState(
    val rounds: Int = 0,
    val globalWeights: List<Double> = listOf(0.0, 0.0, 0.0, 0.0),
    val testAccuracy: Double? = null,
    val testLoss: Double? = null,
    val clientAWeights: List<Double>? = null,
    val clientBWeights: List<Double>? = null
)

private data class Example(
    val stress: Double,
    val sleepDebt: Double,
    val lowEnergy: Double,
    val label: Double
) {
    fun features() = doubleArrayOf(1.0, stress, sleepDebt, lowEnergy)
}

object FederatedLearningEngine {
    // Synthetic teaching examples only. Label 1 means "reflection may be useful", not a diagnosis.
    private val clientA = listOf(
        Example(0.10, 0.10, 0.10, 0.0),
        Example(0.20, 0.10, 0.20, 0.0),
        Example(0.25, 0.25, 0.10, 0.0),
        Example(0.35, 0.10, 0.25, 0.0),
        Example(0.65, 0.45, 0.60, 1.0),
        Example(0.80, 0.50, 0.70, 1.0),
        Example(0.75, 0.80, 0.55, 1.0),
        Example(0.60, 0.30, 0.80, 1.0)
    )
    private val clientB = listOf(
        Example(0.10, 0.05, 0.10, 0.0),
        Example(0.15, 0.25, 0.10, 0.0),
        Example(0.30, 0.20, 0.30, 0.0),
        Example(0.40, 0.15, 0.20, 0.0),
        Example(0.60, 0.65, 0.55, 1.0),
        Example(0.70, 0.70, 0.65, 1.0),
        Example(0.85, 0.45, 0.75, 1.0),
        Example(0.65, 0.35, 0.90, 1.0)
    )
    private val validation = listOf(
        Example(0.12, 0.10, 0.08, 0.0),
        Example(0.25, 0.22, 0.20, 0.0),
        Example(0.28, 0.12, 0.25, 0.0),
        Example(0.38, 0.18, 0.28, 0.0),
        Example(0.64, 0.52, 0.65, 1.0),
        Example(0.74, 0.62, 0.65, 1.0),
        Example(0.78, 0.40, 0.82, 1.0),
        Example(0.58, 0.46, 0.72, 1.0)
    )

    fun runRound(previous: FederatedState): FederatedState {
        val base = previous.globalWeights
        val localA = trainLocally(base, clientA)
        val localB = trainLocally(base, clientB)
        val totalExamples = clientA.size + clientB.size
        val global = base.indices.map { index ->
            (localA[index] * clientA.size + localB[index] * clientB.size) / totalExamples.toDouble()
        }
        val metrics = evaluate(global, validation)
        return FederatedState(
            rounds = previous.rounds + 1,
            globalWeights = global,
            testAccuracy = metrics.first,
            testLoss = metrics.second,
            clientAWeights = localA,
            clientBWeights = localB
        )
    }

    fun reset(): FederatedState = FederatedState()

    private fun trainLocally(initial: List<Double>, examples: List<Example>): List<Double> {
        val weights = initial.toDoubleArray()
        val learningRate = 0.32
        repeat(32) {
            val gradient = DoubleArray(weights.size)
            examples.forEach { example ->
                val features = example.features()
                val probability = sigmoid(dot(weights, features))
                val error = probability - example.label
                for (i in gradient.indices) gradient[i] += error * features[i]
            }
            for (i in weights.indices) {
                val averageGradient = gradient[i] / examples.size.toDouble()
                val regularization = if (i == 0) 0.0 else 0.008 * weights[i]
                weights[i] -= learningRate * (averageGradient + regularization)
            }
        }
        return weights.toList()
    }

    private fun evaluate(weights: List<Double>, examples: List<Example>): Pair<Double, Double> {
        var correct = 0
        var loss = 0.0
        examples.forEach { example ->
            val probability = sigmoid(dot(weights.toDoubleArray(), example.features())).coerceIn(1e-7, 1.0 - 1e-7)
            val prediction = if (probability >= 0.5) 1.0 else 0.0
            if (prediction == example.label) correct++
            loss += -(example.label * ln(probability) + (1.0 - example.label) * ln(1.0 - probability))
        }
        return correct.toDouble() / examples.size to loss / examples.size
    }

    private fun dot(weights: DoubleArray, features: DoubleArray): Double =
        weights.indices.sumOf { weights[it] * features[it] }

    private fun sigmoid(value: Double): Double {
        val safe = value.coerceIn(-30.0, 30.0)
        return 1.0 / (1.0 + exp(-safe))
    }
}
