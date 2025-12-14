package pt.ipp.estg.readytomove.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * A Composable function that registers a listener for the device's ambient light sensor
 * and returns the current light level in lux.
 *
 * This can be used to automatically toggle the app's theme (Dark/Light) based on the environment.
 *
 * @return The current ambient light level in lux. Defaults to 100f if the sensor is unavailable.
 */
@Composable
fun rememberAmbientLightLevel(): Float {
    val context = LocalContext.current
    val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    // State to hold the current light level value
    var lightLevel by remember { mutableFloatStateOf(100f) }

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                // value[0] holds the ambient light level in SI lux units
                lightLevel = event?.values?.get(0) ?: 100f
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            }
        }

        // Register the listener with the sensor manager
        lightSensor?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        // Unregister the listener when the Composable is disposed to save battery
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return lightLevel
}