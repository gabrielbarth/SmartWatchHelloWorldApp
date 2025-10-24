package br.utfpr.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Obtém uma instância do nosso ViewModel
            val viewModel: MainViewModel = viewModel()

            // Observa a mensagem recebida a partir do ViewModel
            val receivedMessage by viewModel.receivedMessage.collectAsState()

            // Estado para o campo de texto
            var textToSend by remember { mutableStateOf("") }

            // UI principal
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFEFEFEF)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Campo de texto para digitar a mensagem
                    OutlinedTextField(
                        value = textToSend,
                        onValueChange = { textToSend = it },
                        label = { Text("Digite sua mensagem") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (textToSend.isNotBlank()) {
                                // Chama a função do ViewModel para enviar a mensagem
                                viewModel.sendMessageToWatch(textToSend)
                                textToSend = "" // Limpa o campo após o envio
                            }
                        },
                        modifier = Modifier
                            .padding(8.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
                    ) {
                        Text("Enviar ao relógio", color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .padding(16.dp)
                    ) {
                        Text("Recebido: $receivedMessage", color = Color.Black)
                    }
                }
            }
        }
    }
}
