package com.example.mipet.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.mipet.data.remote.VoiceCallConfig
import com.google.gson.Gson

private const val retellLogTag = "MiPetRetell"

enum class VoiceAgentState {
    IDLE,
    REQUESTING_PERMISSION,
    CONNECTING,
    CONNECTED,
    ENDED,
    ERROR
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VoiceAgentComponent(
    connection: VoiceCallConfig?,
    configurationError: String?,
    onCloseCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val connectionJson = remember(connection) {
        Gson().toJson(
            connection?.let {
                mapOf(
                    "callId" to it.callId,
                    "accessToken" to it.accessToken,
                    "transport" to it.transport,
                    "iceServers" to it.iceServers
                )
            }
        )
    }

    val hasValidConnection = !connection?.callId.isNullOrBlank() &&
        !connection?.accessToken.isNullOrBlank() &&
        !connection?.transport.isNullOrBlank()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var callState by remember { mutableStateOf(VoiceAgentState.IDLE) }
    var diagnosticMessage by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var startRequested by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (!granted) {
            callState = VoiceAgentState.ERROR
            diagnosticMessage = "El permiso de micrófono es necesario para realizar la llamada."
        }
    }

    LaunchedEffect(connection, hasMicPermission) {
        startRequested = false
        diagnosticMessage = if (hasValidConnection) null else configurationError
        callState = when {
            !hasValidConnection -> VoiceAgentState.ERROR
            !hasMicPermission -> VoiceAgentState.REQUESTING_PERMISSION
            else -> VoiceAgentState.IDLE
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.evaluateJavascript("window.stopRetellCall?.()", null)
            webViewRef?.destroy()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micPulseScale"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (callState) {
                VoiceAgentState.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
                VoiceAgentState.ERROR -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (callState) {
                VoiceAgentState.IDLE -> {
                    Text("Asistente de voz", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Toca para iniciar y activar el audio del asistente.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        startRequested = true
                        callState = VoiceAgentState.CONNECTING
                        diagnosticMessage = "Preparando conexión de audio…"
                    }) {
                        Icon(Icons.Default.Mic, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Iniciar llamada")
                    }
                }

                VoiceAgentState.REQUESTING_PERMISSION -> {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Solicitando permiso de micrófono…")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }) {
                        Text("Conceder permiso de micrófono")
                    }
                }

                VoiceAgentState.CONNECTING -> {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Conectando con el asistente…")
                    diagnosticMessage?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }

                VoiceAgentState.CONNECTED -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .scale(pulseScale)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                CircleShape
                            )
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Mic, "Micrófono activo", tint = Color.White)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Asistente de voz activo", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "La pista de audio del asistente está lista.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = {
                        webViewRef?.evaluateJavascript("window.resumeRetellAudio?.()", null)
                    }) {
                        Text("Activar audio")
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            webViewRef?.evaluateJavascript("window.stopRetellCall?.()", null)
                            callState = VoiceAgentState.ENDED
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Finalizar llamada")
                    }
                }

                VoiceAgentState.ENDED -> {
                    Text("Llamada finalizada", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onCloseCall) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Nueva conversación")
                    }
                }

                VoiceAgentState.ERROR -> {
                    Text(
                        "No fue posible conectar con el asistente de voz.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    diagnosticMessage?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onCloseCall) { Text("Cerrar") }
                }
            }

            if (startRequested && hasMicPermission && hasValidConnection &&
                callState != VoiceAgentState.ENDED && callState != VoiceAgentState.ERROR
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(24.dp),
                    factory = { webContext ->
                        WebView(webContext).apply {
                            webViewRef = this
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false

                            addJavascriptInterface(object {
                                @android.webkit.JavascriptInterface
                                fun onTransportConnected() = mainHandler.post {
                                    diagnosticMessage = "Conexión WebRTC establecida; esperando audio del asistente…"
                                }

                                @android.webkit.JavascriptInterface
                                fun onAgentAudioReady() = mainHandler.post {
                                    callState = VoiceAgentState.CONNECTED
                                    diagnosticMessage = "Audio del asistente disponible."
                                }

                                @android.webkit.JavascriptInterface
                                fun onCallEnded() = mainHandler.post {
                                    callState = VoiceAgentState.ENDED
                                }

                                @android.webkit.JavascriptInterface
                                fun onError(message: String) = mainHandler.post {
                                    Log.e(retellLogTag, "Retell error: " + message)
                                    callState = VoiceAgentState.ERROR
                                    diagnosticMessage = message
                                }

                                @android.webkit.JavascriptInterface
                                fun onDiagnostic(message: String) = mainHandler.post {
                                    Log.d(retellLogTag, message)
                                    diagnosticMessage = message
                                }

                                @android.webkit.JavascriptInterface
                                fun onSdkReady() = mainHandler.post {
                                    webViewRef?.evaluateJavascript(
                                        "window.startRetellCall(" + connectionJson + ");",
                                        null
                                    )
                                }
                            }, "AndroidInterface")

                            webChromeClient = object : WebChromeClient() {
                                override fun onPermissionRequest(request: PermissionRequest) {
                                    val audioRequests = request.resources.filter {
                                        it == PermissionRequest.RESOURCE_AUDIO_CAPTURE
                                    }.toTypedArray()
                                    if (hasMicPermission && audioRequests.isNotEmpty()) {
                                        request.grant(audioRequests)
                                    } else {
                                        request.deny()
                                    }
                                }

                                override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
                                    Log.d(
                                        retellLogTag,
                                        "JS " + message?.messageLevel() + ": " + message?.message()
                                    )
                                    return true
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    Log.d(retellLogTag, "Página cargada; esperando SDK de Retell")
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        mainHandler.post {
                                            callState = VoiceAgentState.ERROR
                                            diagnosticMessage = "No se pudo cargar el motor de llamada: " +
                                                (error?.description ?: "error de WebView")
                                        }
                                    }
                                }
                            }

                            loadDataWithBaseURL(
                                "https://localhost",
                                retellWebPage(),
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    }
                )
            }
        }
    }
}

private fun retellWebPage(): String = """
    <!DOCTYPE html>
    <html>
    <head><meta charset="UTF-8"></head>
    <body style="margin:0;background:transparent;">
      <script type="module">
        import * as RetellSDK from 'https://esm.sh/retell-client-js-sdk@3.0.1';

        const originalConsoleError = console.error.bind(console);

        console.error = (...args) => {
            const details = args.map(value => {
                if (value instanceof Error) {
                    return `${'$'}{value.name}: ${'$'}{value.message}\n${'$'}{value.stack || ""}`;
                }
                return String(value);
            }).join(' ');

            window.AndroidInterface?.onDiagnostic(
                ('Detalle interno: ' + details).slice(0, 500)
            );

            originalConsoleError(...args);
        };

        const report = (message) => {
          console.log('Retell: ' + message);
          window.AndroidInterface?.onDiagnostic(String(message).slice(0, 240));
        };

        window.startRetellCall = async function(config) {
          try {
            if (window.retellWebClientInstance) return;
            const ClientClass = RetellSDK.RetellWebClient ||
              RetellSDK.default?.RetellWebClient;
            if (!ClientClass) throw new Error('RetellWebClient no está disponible.');

            report('SDK 3.0.1 cargado; transporte=' + config.transport);
            const client = new ClientClass();
            window.retellWebClientInstance = client;

            client.on('call_started', () => {
              report('Transporte WebRTC conectado.');
              window.AndroidInterface?.onTransportConnected();
            });
            client.on('call_ready', () => {
              report('Pista de audio del agente recibida.');
              window.AndroidInterface?.onAgentAudioReady();
            });
            client.on('call_ended', () => {
              report('Llamada finalizada.');
              window.AndroidInterface?.onCallEnded();
            });
            client.on('error', (error) => {
              const message = error?.message || String(error || 'Error de Retell');
              console.error('Retell error:', message);
              window.AndroidInterface?.onError(message.slice(0, 240));
            });
            report(
                'Validación SDK: callId=' + !!config.callId +
                ', accessToken=' + !!config.accessToken +
                ', transport=' + config.transport +
                ', iceServers=' + (config.iceServers?.length || 0)
            );
            await client.startCall({
              accessToken: config.accessToken,
              transport: config.transport,
              callId: config.callId,
              iceServers: config.iceServers
            });
          } catch (error) {
            const message = error?.message || String(error || 'No se pudo iniciar la llamada');
            console.error('Retell start error:', message);
            window.AndroidInterface?.onError(message.slice(0, 240));
          }
        };

        window.resumeRetellAudio = async function() {
          try {
            await window.retellWebClientInstance?.startAudioPlayback();
            report('Se solicitó la reproducción de audio.');
          } catch (error) {
            const message = error?.message || String(error || 'No se pudo activar el audio');
            window.AndroidInterface?.onError(message.slice(0, 240));
          }
        };

        window.stopRetellCall = function() {
          window.retellWebClientInstance?.stopCall();
          window.retellWebClientInstance = null;
        };

        window.AndroidInterface?.onSdkReady();
      </script>
    </body>
    </html>
""".trimIndent()
