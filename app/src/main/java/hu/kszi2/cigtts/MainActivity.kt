package hu.kszi2.cigtts

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import hu.kszi2.cigtts.service.TtsWebSocketService
import hu.kszi2.cigtts.tts.TtsManager
import hu.kszi2.cigtts.ui.theme.CigTTSTheme

class MainActivity : ComponentActivity() {
    private lateinit var ttsManager: TtsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ttsManager = TtsManager(this)
        
        enableEdgeToEdge()
        setContent {
            CigTTSTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TtsTestingScreen(
                        onSpeak = { text -> ttsManager.speak(text) },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
    }
}

@Composable
fun TtsTestingScreen(onSpeak: (String) -> Unit, modifier: Modifier = Modifier) {
    var textToSpeak by remember { mutableStateOf("") }
    val context = LocalContext.current
    var isServiceRunning by remember { mutableStateOf(false) }
    val isConnected by TtsWebSocketService.isConnected.collectAsState()

    LaunchedEffect(isConnected) {
        if (isConnected) {
            isServiceRunning = true
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { _ ->
            // Handle permission result if needed
        }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "CigTTS (100% vibe coded)",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        OutlinedTextField(
            value = textToSpeak,
            onValueChange = { textToSpeak = it },
            label = { Text("Enter text to speak") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { 
                if (textToSpeak.isNotBlank()) {
                    onSpeak(textToSpeak)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Test TTS")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val intent = Intent(context, TtsWebSocketService::class.java)
                isServiceRunning = if (isServiceRunning) {
                    context.stopService(intent)
                    false
                } else {
                    context.startForegroundService(intent)
                    true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isConnected) Color(0xFF4CAF50) 
                                 else if (isServiceRunning) MaterialTheme.colorScheme.error 
                                 else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                if (isConnected) "Disconnect (Online)" 
                else if (isServiceRunning) "Stop Service (Connecting...)" 
                else "Connect (Offline)"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TtsTestingScreenPreview() {
    CigTTSTheme {
        TtsTestingScreen(onSpeak = {})
    }
}
