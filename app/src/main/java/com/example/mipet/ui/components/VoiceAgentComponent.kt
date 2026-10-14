package com.example.mipet.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.webkit.ConsoleMessage
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.json.JSONObject

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
    accessToken: String?,
    onCloseCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
    }

    var callState by remember {
        mutableStateOf(
            if (!hasMicPermission) VoiceAgentState.REQUESTING_PERMISSION else VoiceAgentState.CONNECTING
        )
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(Unit) {
        if (!hasMicPermission) {
            callState = VoiceAgentState.REQUESTING_PERMISSION
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    LaunchedEffect(accessToken, hasMicPermission) {
        if (accessToken.isNullOrBlank()) {
            callState = VoiceAgentState.ERROR
            errorMessage = "No se recibió el token del asistente de voz."
        } else if (!hasMicPermission) {
            callState = VoiceAgentState.REQUESTING_PERMISSION
        } else {
            callState = VoiceAgentState.CONNECTING
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.let { webView ->
                webView.evaluateJavascript("stopRetellCall()", null)
                webView.destroy()
            }
        }
    }

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
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (callState) {
                VoiceAgentState.REQUESTING_PERMISSION -> {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Solicitando permiso de micrófono...",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }) {
                        Text("Conceder permiso de micrófono")
                    }
                }

                VoiceAgentState.CONNECTING -> {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Conectando con el asistente...",
                        style = MaterialTheme.typography.titleMedium
                    )
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
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Micrófono activo",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Asistente de voz activo",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Puedes hablar directamente con el asistente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            webViewRef?.evaluateJavascript("stopRetellCall()", null)
                            callState = VoiceAgentState.ENDED
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Finalizar llamada")
                    }
                }

                VoiceAgentState.ENDED -> {
                    Text(
                        "Llamada finalizada",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = onCloseCall) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Nueva conversación")
                    }
                }

                VoiceAgentState.ERROR -> {
                    Text(
                        "No fue posible conectar con el asistente de voz.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    errorMessage?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = onCloseCall) {
                        Text("Cerrar")
                    }
                }

                VoiceAgentState.IDLE -> {}
            }

            if (!accessToken.isNullOrBlank() && hasMicPermission && callState != VoiceAgentState.ENDED && callState != VoiceAgentState.ERROR) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewRef = this
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.javaScriptCanOpenWindowsAutomatically = true

                            webChromeClient = object : WebChromeClient() {
                                override fun onPermissionRequest(request: PermissionRequest) {
                                    request.grant(request.resources)
                                }

                                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                    println("Retell WebView Console: [${consoleMessage?.messageLevel()}] ${consoleMessage?.message()} (at ${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()})")
                                    return true
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    val safeTokenJson = JSONObject.quote(accessToken)
                                    val script = """
                                        (function poll() {
                                            if (typeof window.startRetellCall === 'function') {
                                                window.startRetellCall($safeTokenJson);
                                            } else {
                                                setTimeout(poll, 150);
                                            }
                                        })();
                                    """.trimIndent()
                                    view?.evaluateJavascript(script, null)
                                }
                            }

                            addJavascriptInterface(object {
                                @android.webkit.JavascriptInterface
                                fun onCallStarted() {
                                    callState = VoiceAgentState.CONNECTED
                                }

                                @android.webkit.JavascriptInterface
                                fun onCallEnded() {
                                    callState = VoiceAgentState.ENDED
                                }

                                @android.webkit.JavascriptInterface
                                fun onError(msg: String) {
                                    callState = VoiceAgentState.ERROR
                                    errorMessage = msg
                                }
                            }, "AndroidInterface")

                            val htmlContent = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta charset="UTF-8">
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                </head>
                                <body style="margin:0; padding:0; background:transparent;">
                                    <script type="module">
                                        import * as RetellSDK from 'https://esm.sh/retell-client-js-sdk@2.0.1';

                                        const ClientClass = RetellSDK.RetellWebClient || RetellSDK.RetellClient || (RetellSDK.default && (RetellSDK.default.RetellWebClient || RetellSDK.default.RetellClient)) || RetellSDK.default;

                                        window.startRetellCall = function(token) {
                                            try {
                                                console.log("Retell JS: Iniciando llamada WebRTC...");

                                                if (!ClientClass) {
                                                    console.error("Retell JS: ClientClass no disponible");
                                                    if (window.AndroidInterface) {
                                                        window.AndroidInterface.onError("No se pudo instanciar RetellWebClient.");
                                                    }
                                                    return;
                                                }

                                                const client = new ClientClass();
                                                window.retellWebClientInstance = client;

                                                const handleStart = () => {
                                                    console.log("Retell JS: Evento/Promesa de inicio de llamada recibido");
                                                    if (window.AndroidInterface) window.AndroidInterface.onCallStarted();
                                                };

                                                const handleEnd = () => {
                                                    console.log("Retell JS: Evento de cierre recibido");
                                                    if (window.AndroidInterface) window.AndroidInterface.onCallEnded();
                                                };

                                                const handleError = (err) => {
                                                    const msg = err ? (err.message || String(err)) : "Error en la llamada de voz.";
                                                    console.error("Retell JS Error:", msg);
                                                    if (window.AndroidInterface) window.AndroidInterface.onError(msg);
                                                };

                                                if (typeof client.on === 'function') {
                                                    client.on("call_started", handleStart);
                                                    client.on("call_connected", handleStart);
                                                    client.on("conversation_started", handleStart);

                                                    client.on("call_ended", handleEnd);
                                                    client.on("call_closed", handleEnd);

                                                    client.on("error", handleError);
                                                }

                                                console.log("Retell JS: Ejecutando startCall...");
                                                const result = client.startCall({ accessToken: token });

                                                if (result && typeof result.then === 'function') {
                                                    result.then(() => {
                                                        console.log("Retell JS: Promesa startCall resuelta exitosamente");
                                                        handleStart();
                                                    }).catch((err) => {
                                                        console.error("Retell JS: Promesa startCall rechazada:", err);
                                                        handleError(err);
                                                    });
                                                }
                                            } catch(e) {
                                                console.error("Retell JS Excepcion:", e);
                                                if (window.AndroidInterface) window.AndroidInterface.onError(e.message || String(e));
                                            }
                                        };

                                        window.stopRetellCall = function() {
                                            if (window.retellWebClientInstance && typeof window.retellWebClientInstance.stopCall === 'function') {
                                                window.retellWebClientInstance.stopCall();
                                            }
                                        };
                                    </script>
                                </body>
                                </html>
                            """.trimIndent()

                            loadDataWithBaseURL(
                                "https://localhost",
                                htmlContent,
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
