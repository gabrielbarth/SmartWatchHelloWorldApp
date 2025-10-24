package br.utfpr.myapplication

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val messageClient: MessageClient = Wearable.getMessageClient(application)

    // Expõe a mensagem recebida como um StateFlow para a UI observar
    private val _receivedMessage = MutableStateFlow("Aguardando mensagem...")
    val receivedMessage: StateFlow<String> = _receivedMessage.asStateFlow()

    private val messageListener = MessageClient.OnMessageReceivedListener { messageEvent ->
        if (messageEvent.path == "/msg") {
            val received = String(messageEvent.data)
            Log.d("MobileViewModel", "Recebido: $received")
            _receivedMessage.value = received
        }
    }

    init {
        messageClient.addListener(messageListener)
        Log.d("MobileViewModel", "Listener de mensagens adicionado.")
    }

    fun sendMessageToWatch(message: String) {
        viewModelScope.launch {
            try {
                val nodes = Wearable.getNodeClient(getApplication()).connectedNodes.await()
                Log.d("MobileViewModel", "Nós (relógios) encontrados: ${nodes.size}")
                for (node in nodes) {
                    Log.d("MobileViewModel", "Enviando '$message' para o nó: ${node.displayName} (${node.id})")
                    Wearable.getMessageClient(getApplication())
                        .sendMessage(node.id, "/msg", message.toByteArray())
                        .await()
                    Log.d("MobileViewModel", "Mensagem enviada com sucesso para ${node.id}")
                }
            } catch (e: Exception) {
                Log.e("MobileViewModel", "Erro enviando mensagem: ${e.message}")
            }
        }
    }

    // Limpa o listener quando o ViewModel é destruído
    override fun onCleared() {
        super.onCleared()
        messageClient.removeListener(messageListener)
        Log.d("MobileViewModel", "Listener de mensagens removido.")
    }
}
