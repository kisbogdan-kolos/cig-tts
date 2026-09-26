package hu.kszi2.cigtts.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import hu.kszi2.cigtts.MainActivity
import hu.kszi2.cigtts.R
import hu.kszi2.cigtts.tts.TtsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.*
import okio.ByteString

class TtsWebSocketService : Service() {

    private lateinit var ttsManager: TtsManager
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()
    
    private var wakeLock: PowerManager.WakeLock? = null
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isDestroyed = false
    private val reconnectDelayMs = 3000L

    override fun onCreate() {
        super.onCreate()
        
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CigTTS::WebSocketWakeLock")
        wakeLock?.acquire()
        
        ttsManager = TtsManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        connectWebSocket()

        return START_STICKY
    }

    private fun connectWebSocket() {
        if (isDestroyed) return
        
        val request = Request.Builder()
            .url("wss://cigtts.kszi2.hu/tts") // Using a public test websocket URL
            .build()
            
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                isConnected.value = true
                webSocket.send("WebSocket connection established. TTS is ready.")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Received message: $text")
                ttsManager.speak(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.d(TAG, "Received bytes: ${bytes.hex()}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $reason")
                isConnected.value = false
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                isConnected.value = false
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (isDestroyed) return
        serviceScope.launch {
            Log.d(TAG, "Reconnecting in ${reconnectDelayMs / 1000} seconds...")
            delay(reconnectDelayMs)
            if (!isDestroyed) {
                connectWebSocket()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isConnected.value = false
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        isDestroyed = true
        serviceScope.cancel()
        webSocket?.close(1000, "Service destroyed")
        client.dispatcher.executorService.shutdown()
        ttsManager.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // We don't provide binding for this service
    }

    private fun createNotificationChannel() {
        val name = "TTS WebSocket Service"
        val descriptionText = "Service for receiving TTS messages via WebSocket"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(
                    this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CigTTS")
            .setContentText("Listening for messages...")
            .setSmallIcon(R.mipmap.ic_launcher) // Use app icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val TAG = "TtsWebSocketService"
        private const val CHANNEL_ID = "TtsWebSocketChannel"
        private const val NOTIFICATION_ID = 1
        
        val isConnected = MutableStateFlow(false)
    }
}
