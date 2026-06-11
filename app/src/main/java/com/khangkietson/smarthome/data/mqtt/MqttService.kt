package com.khangkietson.smarthome.data.mqtt

import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.MqttClient
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer

class MqttService {
    private var client: Mqtt3AsyncClient? = null
    
    enum class ConnectionStatus { DISCONNECTED, CONNECTING, CONNECTED, FAILED }
    
    private val _connectionState = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionState: StateFlow<ConnectionStatus> = _connectionState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun connect(brokerUrl: String, clientId: String) {
        if (_connectionState.value == ConnectionStatus.CONNECTED) return
        
        // Ép buộc JVM/Android Runtime ưu tiên sử dụng ngăn xếp IPv4 thay vì IPv6 để tránh lỗi Connection refused
        System.setProperty("java.net.preferIPv4Stack", "true")
        System.setProperty("java.net.preferIPv6Addresses", "false")
        
        _connectionState.value = ConnectionStatus.CONNECTING
        _errorMessage.value = null
        Log.d("MqttService", "Initiating connection to broker: $brokerUrl with client ID prefix: $clientId")
        try {
            val uniqueClientId = "${clientId}_${java.util.UUID.randomUUID().toString().take(8)}"
            client = MqttClient.builder()
                .useMqttVersion3()
                .identifier(uniqueClientId)
                .serverHost(brokerUrl)
                .serverPort(1883)
                .addDisconnectedListener { context ->
                    val cause = context.cause
                    Log.d("MqttService", "MQTT disconnected. Cause: ${cause?.message}")
                    _connectionState.value = ConnectionStatus.DISCONNECTED
                }
                .buildAsync()

            client?.connect()?.whenComplete { connAck, throwable ->
                if (throwable != null) {
                    val errorDesc = throwable.cause?.message ?: throwable.message ?: "Unknown MQTT protocol error"
                    Log.e("MqttService", "MQTT connection failed: $errorDesc", throwable)
                    _errorMessage.value = errorDesc
                    _connectionState.value = ConnectionStatus.FAILED
                } else {
                    Log.d("MqttService", "Connected successfully to MQTT Broker. Ack: $connAck")
                    _errorMessage.value = null
                    _connectionState.value = ConnectionStatus.CONNECTED
                }
            }
        } catch (e: Exception) {
            val errorDesc = e.cause?.message ?: e.message ?: "Exception during initialization"
            Log.e("MqttService", "Error during MQTT client initialization: $errorDesc", e)
            _errorMessage.value = errorDesc
            _connectionState.value = ConnectionStatus.FAILED
        }
    }

    fun setOfflineMode() {
        _connectionState.value = ConnectionStatus.CONNECTED
    }

    fun disconnect() {
        client?.disconnect()
        _connectionState.value = ConnectionStatus.DISCONNECTED
    }

    fun publish(topic: String, payload: String) {
        val currentClient = client
        if (currentClient != null && _connectionState.value == ConnectionStatus.CONNECTED) {
            currentClient.publishWith()
                .topic(topic)
                .payload(payload.toByteArray())
                .send()
        }
    }

    fun subscribe(topic: String, callback: (String) -> Unit) {
        val currentClient = client
        if (currentClient != null) {
            currentClient.subscribeWith()
                .topicFilter(topic)
                .callback { publish ->
                    val payload = publish.payload.map { byteBuffer ->
                        val bytes = ByteArray(byteBuffer.remaining())
                        byteBuffer.duplicate().get(bytes)
                        String(bytes)
                    }.orElse("")
                    callback(payload)
                }
                .send()
        }
    }
}
