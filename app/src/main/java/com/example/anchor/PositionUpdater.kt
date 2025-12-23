package com.example.anchor



import java.util.ArrayDeque
import kotlin.math.*

class PositionUpdater {

    private var previousStepCount: Int = -1
    private var stepDifference: Int = 0

    // Position {X, Y}
    private val position = FloatArray(2) { 0.0f }

    companion object {
        private const val WINDOW_SIZE = 10
    }

    // Acceleration buffer for stride estimation
    private val accelerationBuffer: ArrayDeque<Float> = ArrayDeque()
    private var accelerationSum: Float = 0.0f

    // Orientation in degrees (0–360)
    private var orientation: Float = 0.0f

    /**
     * Update step count from Step Counter sensor
     */
    fun updateStepCount(newStepCount: Int) {
        if (previousStepCount != -1) {
            stepDifference = newStepCount - previousStepCount
            if (stepDifference > 0) {
                val strideLength = calculateStrideLength()
                updatePosition(stepDifference, strideLength)
            }
        }
        previousStepCount = newStepCount
    }

    /**
     * Add acceleration magnitude sample
     */
    fun addAccelerationSample(accelerationMagnitude: Float) {
        if (accelerationBuffer.size >= WINDOW_SIZE) {
            accelerationSum -= accelerationBuffer.removeFirst()
        }
        accelerationBuffer.addLast(accelerationMagnitude)
        accelerationSum += accelerationMagnitude
    }

    /**
     * Estimate stride length using mean acceleration
     */
    private fun calculateStrideLength(): Float {
        if (accelerationBuffer.isEmpty()) {
            return 0.75f // default stride length (meters)
        }
        val meanAcceleration = accelerationSum / accelerationBuffer.size
        return (0.98f * cbrt(meanAcceleration))
    }

    /**
     * Update position based on orientation angle
     */
    private fun updatePosition(steps: Int, strideLength: Float) {

        val distance = steps * strideLength

        val x0 = position[0]
        val y0 = position[1]
        val theta = orientation

        val radians = Math.toRadians(theta.toDouble())

        val x1 = x0 + (distance * sin(radians)).toFloat()
        val y1 = y0 + (distance * cos(radians)).toFloat()

        position[0] = x1
        position[1] = y1
    }

    /**
     * Update orientation from sensor fusion (degrees)
     */
    fun updateOrientation(newOrientation: Float) {
        orientation = (newOrientation + 360f) % 360f
    }

    /**
     * Get current position
     */
    fun getPosition(): FloatArray = position

    /**
     * Get last step difference
     */
    fun getStepCount(): Int = stepDifference
}
