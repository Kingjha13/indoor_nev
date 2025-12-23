package com.example.anchor


class KalmanFilter {

    // Estimated angle (θ)
    private var theta: Float = 0.0f

    // Bias in angular rate (θ̇b)
    private var thetaDotBias: Float = 0.0f

    // Error covariance matrix
    private var p00: Float = 1.0f
    private var p01: Float = 0.0f
    private var p10: Float = 0.0f
    private var p11: Float = 1.0f

    /**
     * Update the Kalman filter
     *
     * @param accMagAngle Angle from accelerometer + magnetometer (degrees or radians)
     * @param gyroAngle Angular velocity from gyroscope
     * @param dt Time delta in seconds
     * @return Filtered angle (θ)
     */
    fun update(
        accMagAngle: Float,
        gyroAngle: Float,
        dt: Float
    ): Float {

        // =========================
        // Prediction step
        // =========================
        theta += gyroAngle * dt
        theta -= thetaDotBias * dt

        // Process noise variance (angle)
        val qAngle = 0.01f
        p00 += dt * (dt * p11 - p01 - p10 + qAngle)
        p01 -= dt * p11
        p10 -= dt * p11

        // Process noise variance (gyro bias)
        val qBias = 0.003f
        p11 += qBias * dt

        // =========================
        // Update step
        // =========================
        val z = accMagAngle - theta

        // Measurement noise variance
        val rMeasure = 0.01f

        val s = p00 + rMeasure
        val k0 = p00 / s
        val k1 = p10 / s

        theta += k0 * z
        thetaDotBias += k1 * z

        // =========================
        // Covariance update
        // =========================
        val tempP00 = p00
        val tempP01 = p01

        p00 -= k0 * tempP00
        p01 -= k0 * tempP01
        p10 -= k1 * tempP00
        p11 -= k1 * tempP01

        return theta
    }
}
