package com.example.anchor


import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import java.util.Timer
import java.util.TimerTask
import kotlin.math.*

class SensorFusionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager

    private val gyro = FloatArray(3)
    private val gyroMatrix = FloatArray(9)
    private val gyroOrientation = FloatArray(3)

    private val magnet = FloatArray(3)
    private val accel = FloatArray(3)
    private val accMagOrientation = FloatArray(3)

    private val rotationMatrix = FloatArray(9)
    private val fusedOrientation = FloatArray(3)

    private var initState = true
    private var timestamp: Long = 0L

    private val fuseTimer = Timer()

    private val positionUpdater = PositionUpdater()

    companion object {
        const val EPSILON = 1e-9f
        const val NS2S = 1.0f / 1_000_000_000.0f
        const val TIME_CONSTANT = 30L
        const val TAG = "SensorFusionService"
    }

    override fun onCreate() {
        super.onCreate()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        gyroMatrix.fill(0f)
        gyroMatrix[0] = 1f
        gyroMatrix[4] = 1f
        gyroMatrix[8] = 1f

        initListeners()

        fuseTimer.scheduleAtFixedRate(
            CalculateFusedOrientationTask(),
            5000,
            TIME_CONSTANT
        )

        Log.d(TAG, "Service started")
    }

    private fun initListeners() {
        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_FASTEST
        )

        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
            SensorManager.SENSOR_DELAY_FASTEST
        )

        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
            SensorManager.SENSOR_DELAY_FASTEST
        )

        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER),
            SensorManager.SENSOR_DELAY_FASTEST
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        fuseTimer.cancel()
        Log.d(TAG, "Service stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent) {

        when (event.sensor.type) {

            Sensor.TYPE_STEP_COUNTER -> {
                positionUpdater.updateStepCount(event.values[0].toInt())
            }

            Sensor.TYPE_ACCELEROMETER -> {
                val ax = event.values[0]
                val ay = event.values[1]
                val az = event.values[2]

                val magnitude = sqrt(ax * ax + ay * ay + az * az)
                positionUpdater.addAccelerationSample(magnitude)

                System.arraycopy(event.values, 0, accel, 0, 3)
                calculateAccMagOrientation()
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnet, 0, 3)
            }

            Sensor.TYPE_GYROSCOPE -> {
                gyroFunction(event)
            }
        }

        val yawDegrees =
            if (fusedOrientation[2] < 0)
                fusedOrientation[2] * 180f / Math.PI.toFloat() + 360f
            else
                fusedOrientation[2] * 180f / Math.PI.toFloat()

        positionUpdater.updateOrientation(yawDegrees)

        sendSensorData()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun calculateAccMagOrientation() {
        if (SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet)) {
            SensorManager.getOrientation(rotationMatrix, accMagOrientation)
        }
    }

    private fun sendSensorData() {
        val intent = Intent("SensorDataUpdate")
        intent.putExtra("orientation", positionUpdater.getStepCount().toDouble())
        intent.putExtra("position", positionUpdater.getPosition())
        intent.putExtra("step", positionUpdater.getStepCount())

        sendBroadcast(intent)
    }

    private fun gyroFunction(event: SensorEvent) {

        if (initState) {
            val initMatrix = getRotationMatrixFromOrientation(accMagOrientation)
            gyroMatrix.indices.forEach { gyroMatrix[it] = initMatrix[it] }
            initState = false
        }

        if (timestamp != 0L) {
            val dt = (event.timestamp - timestamp) * NS2S
            System.arraycopy(event.values, 0, gyro, 0, 3)

            val deltaVector = FloatArray(4)
            getRotationVectorFromGyro(gyro, deltaVector, dt / 2f)

            val deltaMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(deltaMatrix, deltaVector)
            multiplyMatrices(gyroMatrix, deltaMatrix, gyroMatrix)
        }

        timestamp = event.timestamp
        SensorManager.getOrientation(gyroMatrix, gyroOrientation)
    }

    private fun getRotationVectorFromGyro(
        gyroValues: FloatArray,
        deltaRotationVector: FloatArray,
        timeFactor: Float
    ) {
        val omegaMagnitude =
            sqrt(gyroValues[0].pow(2) + gyroValues[1].pow(2) + gyroValues[2].pow(2))

        val norm = FloatArray(3)
        if (omegaMagnitude > EPSILON) {
            norm[0] = gyroValues[0] / omegaMagnitude
            norm[1] = gyroValues[1] / omegaMagnitude
            norm[2] = gyroValues[2] / omegaMagnitude
        }

        val thetaOverTwo = omegaMagnitude * timeFactor
        val sinT = sin(thetaOverTwo)
        val cosT = cos(thetaOverTwo)

        deltaRotationVector[0] = sinT * norm[0]
        deltaRotationVector[1] = sinT * norm[1]
        deltaRotationVector[2] = sinT * norm[2]
        deltaRotationVector[3] = cosT
    }

    private fun getRotationMatrixFromOrientation(o: FloatArray): FloatArray {
        val xM = FloatArray(9)
        val yM = FloatArray(9)
        val zM = FloatArray(9)

        val sinX = sin(o[1])
        val cosX = cos(o[1])
        val sinY = sin(o[2])
        val cosY = cos(o[2])
        val sinZ = sin(o[0])
        val cosZ = cos(o[0])

        xM[0] = 1f; xM[4] = cosX; xM[5] = sinX; xM[7] = -sinX; xM[8] = cosX
        yM[0] = cosY; yM[2] = sinY; yM[4] = 1f; yM[6] = -sinY; yM[8] = cosY
        zM[0] = cosZ; zM[1] = sinZ; zM[3] = -sinZ; zM[4] = cosZ; zM[8] = 1f

        val result = FloatArray(9)
        multiplyMatrices(xM, yM, result)
        multiplyMatrices(zM, result, result)
        return result
    }

    private fun multiplyMatrices(a: FloatArray, b: FloatArray, out: FloatArray) {
        for (i in 0..2) {
            for (j in 0..2) {
                out[i * 3 + j] =
                    a[i * 3] * b[j] +
                            a[i * 3 + 1] * b[3 + j] +
                            a[i * 3 + 2] * b[6 + j]
            }
        }
    }

    inner class CalculateFusedOrientationTask : TimerTask() {
        private val kalmanFilters = Array(3) { KalmanFilter() }

        override fun run() {
            val dt = 0.03f
            for (i in 0..2) {
                fusedOrientation[i] =
                    kalmanFilters[i].update(accMagOrientation[i], gyroOrientation[i], dt)
            }
        }
    }
}
