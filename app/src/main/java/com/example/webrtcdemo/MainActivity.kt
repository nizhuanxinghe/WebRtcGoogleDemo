package com.example.webrtcdemo

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.webrtcdemo.ui.theme.WebRTCDemoTheme
import org.webrtc.SurfaceViewRenderer

class MainActivity : ComponentActivity() {

    private lateinit var webRTCManager: WebRTCManager
    private var localRenderer: SurfaceViewRenderer? = null
    private var remoteRenderer: SurfaceViewRenderer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webRTCManager = WebRTCManager(this)

        setContent {
            WebRTCDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WebRTCScreen(
                        modifier = Modifier.padding(innerPadding),
                        webRTCManager = webRTCManager,
                        onLocalRendererCreated = { renderer -> localRenderer = renderer },
                        onRemoteRendererCreated = { renderer -> remoteRenderer = renderer }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        localRenderer?.release()
        remoteRenderer?.release()
        localRenderer = null
        remoteRenderer = null
        if (::webRTCManager.isInitialized) {
            webRTCManager.release()
        }
        super.onDestroy()
    }
}

@Composable
fun WebRTCScreen(
    modifier: Modifier = Modifier,
    webRTCManager: WebRTCManager,
    onLocalRendererCreated: (SurfaceViewRenderer) -> Unit,
    onRemoteRendererCreated: (SurfaceViewRenderer) -> Unit
) {
    val context = LocalContext.current
    var serverUrl by remember { mutableStateOf("ws://172.20.10.10:8080/ws") }
    var isConnected by remember { mutableStateOf(false) }

    // Permission handling
    val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.all { it.value }) {
            // Permissions granted
        } else {
            Toast.makeText(context, "Permissions required for WebRTC", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(permissions)
    }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        TextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text("Signaling Server URL") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    if (serverUrl.isNotBlank()) {
                        webRTCManager.startCall(serverUrl)
                        isConnected = true
                    } else {
                        Toast.makeText(context, "Please enter a valid URL", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !isConnected,
                modifier = Modifier.weight(1f)
            ) {
                Text("Start Call")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    webRTCManager.stopCall()
                    isConnected = false
                },
                enabled = isConnected,
                modifier = Modifier.weight(1f)
            ) {
                Text("Stop Call")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Video Renderers
        Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Text("Remote Video")
            AndroidView(
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).apply {
                        onRemoteRendererCreated(this)
                        webRTCManager.initRenderer(this)
                    }
                },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            Text("Local Video")
            AndroidView(
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).apply {
                        onLocalRendererCreated(this)
                        webRTCManager.startCapture(this)
                    }
                },
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
    }
}
