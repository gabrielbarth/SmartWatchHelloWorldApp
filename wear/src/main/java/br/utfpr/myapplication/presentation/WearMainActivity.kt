package br.utfpr.myapplication.presentation

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class WearMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            var messageReceived by remember { mutableStateOf("Aguardando mensagem...") }
            val scope = rememberCoroutineScope()

            DisposableEffect(Unit) {
                val messageClient = Wearable.getMessageClient(context)
                val listener = MessageClient.OnMessageReceivedListener { messageEvent: MessageEvent ->
                    if (messageEvent.path == "/msg") {
                        val received = String(messageEvent.data)
                        Log.d("Wear", "Recebido: $received")
                        messageReceived = received
                    }
                }
                messageClient.addListener(listener)
                onDispose {
                    messageClient.removeListener(listener)
                }
            }

            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Recebido: $messageReceived",
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(onClick = {
                        scope.launch {
                            val formattedTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                            val message = "Olá do relógio ⌚ às $formattedTime"
                            enviarMensagemAoMobile(message, context)
                        }
                    }) {
                        Text("Enviar ao celular")
                    }
                }
            }
        }
    }

    private suspend fun enviarMensagemAoMobile(mensagem: String, context: Context) {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            for (node in nodes) {
                Wearable.getMessageClient(context)
                    .sendMessage(node.id, "/msg", mensagem.toByteArray())
                    .await()
            }
        } catch (e: Exception) {
            Log.e("Wear", "Erro enviando mensagem: ${e.message}")
        }
    }
}
