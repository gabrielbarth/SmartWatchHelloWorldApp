package br.utfpr.myapplication.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.math.sqrt

class WearMainActivity : ComponentActivity() {

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var isMoving = false
    private var message: String = ""
    private lateinit var sensorListener: SensorEventListener


    private fun checkAndRequestBodySensorsPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BODY_SENSORS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BODY_SENSORS),
                1001 // request code
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestBodySensorsPermission()

        setContent {
            var messageReceived by remember { mutableStateOf("Aguardando mensagem...") }

            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = messageReceived)
                    Button(onClick = { fetchAndSendWeather() }) {
                        Text(text = "Obter clima e enviar")
                    }
                }
            }
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val sensorListener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val x = it.values[0]
                    val y = it.values[1]
                    val z = it.values[2]
                    val acceleration = sqrt((x * x + y * y + z * z).toDouble())
                    Log.d("Wear", "acceleration: $acceleration") // just for debugging
                    if (acceleration > 10 && !isMoving) { // threshold for movement
                        isMoving = true
                        message = "usuário está em movimento"
                        sendMovementMessage(message = message)
                    } else if (acceleration < 10) {
                        if (message.isNotEmpty()) {
                            message = ""
                            sendMovementMessage(message = message)
                        }
                        isMoving = false
                    }
                }
            }
        }

        sensorManager?.registerListener(
            sensorListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    private fun sendMovementMessage(message: String) {
        val context = this
        val scope = lifecycleScope

        Log.d("Wear", "sendMovementMessage: $message")

        scope.launch {
            try {
                val nodes = Wearable.getNodeClient(context).connectedNodes.await()
                for (node in nodes) {
                    Wearable.getMessageClient(context)
                        .sendMessage(node.id, "/msg", message.toByteArray())
                        .await()
                }
            } catch (e: Exception) {
                Log.e("Wear", "Error sending movement message: ${e.message}")
            }
        }
    }

    private fun fetchAndSendWeather() {
        // estou obtendo o clima de Brasilia apenas como exemplo
        // brasilia: latitude=-15.79, longitude=-47.88
        lifecycleScope.launch {
            val weatherInfo = withContext(Dispatchers.IO) {
                val client = OkHttpClient()
                val url =
                    "https://api.open-meteo.com/v1/forecast?latitude=-15.79&longitude=-47.88&current=temperature_2m,wind_speed_10m&hourly=temperature_2m,relative_humidity_2m,wind_speed_10m"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                if (body != null) {
                    val json = JSONObject(body)
                    Log.d("Wear", "Weather JSON: $json")
                    val temp = json.getJSONObject("current").getDouble("temperature_2m")
                    "Clima: $temp°C"
                } else {
                    "Erro ao obter clima"
                }
            }
            sendMovementMessage(weatherInfo)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager?.unregisterListener(sensorListener)
    }
}