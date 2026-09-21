package com.iplate.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.iplate.app.BuildConfig
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val PREFS_NAME = "iplate_prefs"
private const val PREF_SERVER_URL = "server_url"
private const val PREF_RESERVATION_ENDPOINT = "reservation_endpoint"
private const val PREF_EXPRESSO_TOKEN_ENDPOINT = "expresso_token_endpoint"
private const val PREF_EXPRESSO_ATTACHMENT_ENDPOINT = "expresso_attachment_endpoint"
private const val PREF_PLATE_WEB_URL = "plate_web_url"
private const val PREF_PLATE_WEB_AUTO_CLOSE_SECONDS = "plate_web_auto_close_seconds"
private const val PREF_EXPRESSO_USER = "expresso_user"
private const val PREF_EXPRESSO_PASSWORD = "expresso_password"
private const val PREF_COMPRESS_UPLOADS = "compress_uploads"
private const val PREF_AUTH_TOKEN = "auth_token"
private const val PREF_USER_NAME = "user_name"
private const val PREF_USERNAME = "username"
private const val DEFAULT_SERVER_URL = "https://vale.expresso.app/iplate/backend/api/vehicle-entry-create.php"
private const val DEFAULT_RESERVATION_ENDPOINT = "https://vale.expresso.app/api/reserva"
private const val DEFAULT_EXPRESSO_TOKEN_ENDPOINT = "https://vale.expresso.app/api/obter_token"
private const val DEFAULT_EXPRESSO_ATTACHMENT_ENDPOINT = "https://vale.expresso.app/api/anexo"
private const val DEFAULT_PLATE_WEB_URL = "http://prodatastelecom.com.br/palio.html"
private const val DEFAULT_PLATE_WEB_AUTO_CLOSE_SECONDS = 5
private const val DEFAULT_EXPRESSO_USER = ""
private const val DEFAULT_EXPRESSO_PASSWORD = ""
private const val SETTINGS_ACCESS_PASSWORD = "CHANGE_ME"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    IPlateApp()
                }
            }
        }
    }
}

private enum class PhotoType {
    PLATE,
    FRONT,
    DRIVER_SIDE,
    REAR,
    PASSENGER_SIDE,
}

private data class ApiResult(
    val success: Boolean,
    val message: String,
)

private data class LoginResult(
    val success: Boolean,
    val message: String,
    val token: String = "",
    val userName: String = "",
    val username: String = "",
)

private data class FipeOption(
    val code: String,
    val name: String,
)

private data class ReservationOption(
    val reservationId: Int,
    val reservationCode: String,
    val guestName: String,
    val status: String,
    val checkinDate: String,
) {
    val displayLabel: String
        get() = buildString {
            append(reservationCode)
            if (guestName.isNotBlank()) {
                append(" - ")
                append(guestName)
            }
        }
}

private data class ReservationLookupResult(
    val reservationId: String,
    val reservationCode: String,
    val guestName: String,
    val guestCpf: String,
    val checkinDate: String,
    val adults: String,
    val children: String,
    val uh: String,
) {
    val isOutOfDate: Boolean
        get() = !isToday(checkinDate)
}

private data class ReservationFetchResult(
    val details: ReservationLookupResult? = null,
    val message: String = "",
)

private data class ImageCompressionProfile(
    val maxDimension: Int,
    val jpegQuality: Int,
)

private data class AttachmentPhoto(
    val fieldName: String,
    val uri: Uri,
    val fallbackName: String,
    val details: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IPlateApp() {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val preferences = remember {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var serverUrl by rememberSaveable {
        mutableStateOf(preferences.getString(PREF_SERVER_URL, DEFAULT_SERVER_URL).orEmpty())
    }
    var reservationEndpoint by rememberSaveable {
        mutableStateOf(preferences.getString(PREF_RESERVATION_ENDPOINT, DEFAULT_RESERVATION_ENDPOINT).orEmpty())
    }
    var expressoTokenEndpoint by rememberSaveable {
        mutableStateOf(preferences.getString(PREF_EXPRESSO_TOKEN_ENDPOINT, DEFAULT_EXPRESSO_TOKEN_ENDPOINT).orEmpty())
    }
    var expressoAttachmentEndpoint by rememberSaveable {
        mutableStateOf(preferences.getString(PREF_EXPRESSO_ATTACHMENT_ENDPOINT, DEFAULT_EXPRESSO_ATTACHMENT_ENDPOINT).orEmpty())
    }
    var plateWebUrl by rememberSaveable {
        mutableStateOf(preferences.getString(PREF_PLATE_WEB_URL, DEFAULT_PLATE_WEB_URL).orEmpty().ifBlank { DEFAULT_PLATE_WEB_URL })
    }
    var plateWebAutoCloseSeconds by rememberSaveable {
        mutableStateOf(preferences.getInt(PREF_PLATE_WEB_AUTO_CLOSE_SECONDS, DEFAULT_PLATE_WEB_AUTO_CLOSE_SECONDS))
    }
    var expressoUser by rememberSaveable {
        mutableStateOf(preferences.getString(PREF_EXPRESSO_USER, DEFAULT_EXPRESSO_USER).orEmpty().ifBlank { DEFAULT_EXPRESSO_USER })
    }
    var expressoPassword by rememberSaveable {
        mutableStateOf(preferences.getString(PREF_EXPRESSO_PASSWORD, DEFAULT_EXPRESSO_PASSWORD).orEmpty().ifBlank { DEFAULT_EXPRESSO_PASSWORD })
    }
    var compressUploads by rememberSaveable {
        mutableStateOf(preferences.getBoolean(PREF_COMPRESS_UPLOADS, false))
    }
    var authToken by rememberSaveable {
        mutableStateOf(preferences.getString(PREF_AUTH_TOKEN, "").orEmpty())
    }
    var operatorName by rememberSaveable {
        mutableStateOf(preferences.getString(PREF_USER_NAME, "").orEmpty())
    }
    var usernameSaved by rememberSaveable {
        mutableStateOf(preferences.getString(PREF_USERNAME, "").orEmpty())
    }
    var plate by rememberSaveable { mutableStateOf("") }
    var reservation by rememberSaveable { mutableStateOf("") }
    var selectedReservationId by rememberSaveable { mutableStateOf(0) }
    var selectedExpressoReservationId by rememberSaveable { mutableStateOf("") }
    var brand by rememberSaveable { mutableStateOf("") }
    var model by rememberSaveable { mutableStateOf("") }
    var selectedBrandCode by rememberSaveable { mutableStateOf("") }
    var selectedModelCode by rememberSaveable { mutableStateOf("") }
    var reservationSuggestions by remember { mutableStateOf<List<ReservationOption>>(emptyList()) }
    var reservationExpanded by remember { mutableStateOf(false) }
    var brandSuggestions by remember { mutableStateOf<List<FipeOption>>(emptyList()) }
    var modelSuggestions by remember { mutableStateOf<List<FipeOption>>(emptyList()) }
    var allBrandsCache by remember { mutableStateOf<List<FipeOption>>(emptyList()) }
    var modelCache by remember { mutableStateOf<Map<String, List<FipeOption>>>(emptyMap()) }
    var brandExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }
    var platePhotoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var frontPhotoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var driverSidePhotoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var rearPhotoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var passengerSidePhotoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var platePhotoDebug by remember { mutableStateOf("") }
    var plateFromOcr by rememberSaveable { mutableStateOf("") }
    var pendingPhotoType by remember { mutableStateOf<PhotoType?>(null) }
    var pendingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPhotoFilePath by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var isFetchingReservation by remember { mutableStateOf(false) }
    var isProcessingPlate by remember { mutableStateOf(false) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSettingsPasswordDialog by remember { mutableStateOf(false) }
    var settingsPasswordInput by rememberSaveable { mutableStateOf("") }
    var settingsPasswordError by rememberSaveable { mutableStateOf("") }
    var showPlateWebDialog by remember { mutableStateOf(false) }
    var plateWebDialogPlate by rememberSaveable { mutableStateOf("") }
    var draftUrl by rememberSaveable { mutableStateOf(serverUrl) }
    var draftReservationEndpoint by rememberSaveable { mutableStateOf(reservationEndpoint) }
    var draftExpressoTokenEndpoint by rememberSaveable { mutableStateOf(expressoTokenEndpoint) }
    var draftExpressoAttachmentEndpoint by rememberSaveable { mutableStateOf(expressoAttachmentEndpoint) }
    var draftPlateWebUrl by rememberSaveable { mutableStateOf(plateWebUrl) }
    var draftPlateWebAutoCloseSeconds by rememberSaveable { mutableStateOf(plateWebAutoCloseSeconds.toString()) }
    var draftExpressoUser by rememberSaveable { mutableStateOf(expressoUser) }
    var draftExpressoPassword by rememberSaveable { mutableStateOf(expressoPassword) }
    var draftCompressUploads by rememberSaveable { mutableStateOf(compressUploads) }
    var selectedReservationDetails by remember { mutableStateOf<ReservationLookupResult?>(null) }
    var lastReservationDetailsLookupCode by rememberSaveable { mutableStateOf("") }
    var loginUsername by rememberSaveable { mutableStateOf(usernameSaved) }
    var loginPassword by rememberSaveable { mutableStateOf("") }
    val showMessage: (String) -> Unit = { message ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }
    val lookupReservationDetails: (String, Boolean) -> Unit = { rawReservation, forceDialog ->
        val reservationCode = rawReservation.onlyDigits()
        if (reservationCode.length >= 3 && reservationEndpoint.isBlank()) {
            showMessage("Configure o endpoint de reservas para consultar os dados.")
        } else if (reservationCode.length >= 3 && (forceDialog || (!isFetchingReservation && reservationCode != lastReservationDetailsLookupCode))) {
            scope.launch {
                isFetchingReservation = true
                val lookupResult = fetchReservationDetails(
                    tokenEndpointUrl = expressoTokenEndpoint,
                    reservationEndpointUrl = reservationEndpoint,
                    apiUser = expressoUser,
                    apiPassword = expressoPassword,
                    reservationCode = reservationCode
                )
                lastReservationDetailsLookupCode = reservationCode
                if (lookupResult.details == null) {
                    showMessage(lookupResult.message.ifBlank { "Nao foi possivel consultar a reserva." })
                } else {
                    selectedExpressoReservationId = lookupResult.details.reservationId
                    selectedReservationDetails = lookupResult.details
                }
                isFetchingReservation = false
            }
        }
    }

    LaunchedEffect(reservation) {
        val reservationCode = reservation.onlyDigits()
        if (reservationCode.length < 3 || reservationCode == lastReservationDetailsLookupCode) {
            return@LaunchedEffect
        }

        delay(2000)

        val stableReservationCode = reservation.onlyDigits()
        if (stableReservationCode == reservationCode && !isFetchingReservation) {
            lookupReservationDetails(stableReservationCode, false)
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            when (pendingPhotoType) {
                PhotoType.PLATE -> {
                    pendingPhotoFilePath?.let { filePath ->
                        scope.launch {
                            val photoFile = File(filePath)
                            if (!photoFile.exists() || !photoFile.canRead()) {
                                showMessage("Nao foi possivel acessar a foto da placa apos captura.")
                                return@launch
                            }
                            val photoUri = Uri.fromFile(photoFile)
                            platePhotoUri = photoUri.toString()
                            platePhotoDebug = diagnosePhotoUri(context, photoUri)
                            isProcessingPlate = true
                            val detectedPlate = runCatching {
                                detectPlateFromImage(context, photoUri)
                            }.getOrNull()
                            isProcessingPlate = false
                            if (detectedPlate != null) {
                                plate = detectedPlate
                                plateFromOcr = detectedPlate
                                plateWebDialogPlate = detectedPlate
                                showPlateWebDialog = true
                                showMessage("Placa reconhecida: $detectedPlate")
                            } else {
                                showMessage("Nao foi possivel reconhecer a placa automaticamente.")
                            }
                        }
                    }
                }
                PhotoType.FRONT -> frontPhotoUri = pendingPhotoFilePath?.let { path -> Uri.fromFile(File(path)).toString() }
                PhotoType.DRIVER_SIDE -> driverSidePhotoUri = pendingPhotoFilePath?.let { path -> Uri.fromFile(File(path)).toString() }
                PhotoType.REAR -> rearPhotoUri = pendingPhotoFilePath?.let { path -> Uri.fromFile(File(path)).toString() }
                PhotoType.PASSENGER_SIDE -> passengerSidePhotoUri = pendingPhotoFilePath?.let { path -> Uri.fromFile(File(path)).toString() }
                null -> Unit
            }
        }
        pendingPhotoType = null
        pendingPhotoUri = null
        pendingPhotoFilePath = null
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingPhotoType?.let { type ->
                val output = context.createImageTarget(type)
                pendingPhotoUri = output.uri
                pendingPhotoFilePath = output.file.absolutePath
                takePictureLauncher.launch(output.uri)
            }
        } else {
            showMessage("Permissao da camera negada.")
            pendingPhotoType = null
            pendingPhotoUri = null
            pendingPhotoFilePath = null
        }
    }

    val qrScanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result ->
        val contents = result.contents?.trim().orEmpty()
        if (contents.isNotEmpty()) {
            val digitsOnly = contents.onlyDigits()
            if (digitsOnly.isEmpty()) {
                showMessage("QR Code lido, mas sem numeros validos para a reserva.")
            } else {
                reservation = digitsOnly
                lookupReservationDetails(digitsOnly, true)
            }
        }
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Servidor") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Versao da compilacao: ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Informe a URL completa da API de envio. O login usa automaticamente o endpoint /api/login.php.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = draftUrl,
                        onValueChange = { draftUrl = it.trim() },
                        label = { Text("URL do servidor") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draftReservationEndpoint,
                        onValueChange = { draftReservationEndpoint = it.trim() },
                        label = { Text("Endpoint de reservas") },
                        placeholder = { Text("https://.../{reservation}") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draftExpressoTokenEndpoint,
                        onValueChange = { draftExpressoTokenEndpoint = it.trim() },
                        label = { Text("Endpoint token Expresso") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draftExpressoAttachmentEndpoint,
                        onValueChange = { draftExpressoAttachmentEndpoint = it.trim() },
                        label = { Text("Endpoint anexos Expresso") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draftPlateWebUrl,
                        onValueChange = { draftPlateWebUrl = it.trim() },
                        label = { Text("URL consulta placa WebView") },
                        placeholder = { Text("https://.../placa?placa-fipe={plate}") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draftPlateWebAutoCloseSeconds,
                        onValueChange = { value ->
                            draftPlateWebAutoCloseSeconds = value.filter { it.isDigit() }.take(3)
                        },
                        label = { Text("Fechar WebView apos segundos") },
                        placeholder = { Text("0 para nao fechar automaticamente") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = draftExpressoUser,
                        onValueChange = { draftExpressoUser = it.trim() },
                        label = { Text("Usuario API Expresso") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draftExpressoPassword,
                        onValueChange = { draftExpressoPassword = it },
                        label = { Text("Senha API Expresso") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Comprimir imagens")
                            Text(
                                text = "Reduz o tamanho das fotos antes do upload.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF68584A)
                            )
                        }
                        Switch(
                            checked = draftCompressUploads,
                            onCheckedChange = { draftCompressUploads = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val autoCloseSeconds = draftPlateWebAutoCloseSeconds.toIntOrNull()?.coerceIn(0, 300)
                        ?: DEFAULT_PLATE_WEB_AUTO_CLOSE_SECONDS
                    serverUrl = draftUrl
                    reservationEndpoint = draftReservationEndpoint
                    expressoTokenEndpoint = draftExpressoTokenEndpoint
                    expressoAttachmentEndpoint = draftExpressoAttachmentEndpoint
                    plateWebUrl = draftPlateWebUrl.ifBlank { DEFAULT_PLATE_WEB_URL }
                    plateWebAutoCloseSeconds = autoCloseSeconds
                    expressoUser = draftExpressoUser
                    expressoPassword = draftExpressoPassword
                    compressUploads = draftCompressUploads
                    authToken = ""
                    operatorName = ""
                    preferences.edit()
                        .putString(PREF_SERVER_URL, draftUrl)
                        .putString(PREF_RESERVATION_ENDPOINT, draftReservationEndpoint)
                        .putString(PREF_EXPRESSO_TOKEN_ENDPOINT, draftExpressoTokenEndpoint)
                        .putString(PREF_EXPRESSO_ATTACHMENT_ENDPOINT, draftExpressoAttachmentEndpoint)
                        .putString(PREF_PLATE_WEB_URL, draftPlateWebUrl.ifBlank { DEFAULT_PLATE_WEB_URL })
                        .putInt(PREF_PLATE_WEB_AUTO_CLOSE_SECONDS, autoCloseSeconds)
                        .putString(PREF_EXPRESSO_USER, draftExpressoUser)
                        .putString(PREF_EXPRESSO_PASSWORD, draftExpressoPassword)
                        .putBoolean(PREF_COMPRESS_UPLOADS, draftCompressUploads)
                        .remove(PREF_AUTH_TOKEN)
                        .remove(PREF_USER_NAME)
                        .apply()
                    showSettingsDialog = false
                }) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showSettingsPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showSettingsPasswordDialog = false
                settingsPasswordInput = ""
                settingsPasswordError = ""
            },
            title = { Text("Acesso restrito") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = settingsPasswordInput,
                        onValueChange = {
                            settingsPasswordInput = it
                            settingsPasswordError = ""
                        },
                        label = { Text("Senha de configuracoes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (settingsPasswordError.isNotBlank()) {
                        Text(
                            text = settingsPasswordError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (settingsPasswordInput == SETTINGS_ACCESS_PASSWORD) {
                        draftUrl = serverUrl
                        draftReservationEndpoint = reservationEndpoint
                        draftExpressoTokenEndpoint = expressoTokenEndpoint
                        draftExpressoAttachmentEndpoint = expressoAttachmentEndpoint
                        draftPlateWebUrl = plateWebUrl
                        draftPlateWebAutoCloseSeconds = plateWebAutoCloseSeconds.toString()
                        draftExpressoUser = expressoUser
                        draftExpressoPassword = expressoPassword
                        draftCompressUploads = compressUploads
                        settingsPasswordInput = ""
                        settingsPasswordError = ""
                        showSettingsPasswordDialog = false
                        showSettingsDialog = true
                    } else {
                        settingsPasswordError = "Senha invalida."
                    }
                }) {
                    Text("Entrar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSettingsPasswordDialog = false
                    settingsPasswordInput = ""
                    settingsPasswordError = ""
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (isFetchingReservation) {
        LoadingDialog(
            title = "Consultando reserva",
            message = "Buscando dados da reserva no endpoint configurado."
        )
    }

    if (isSending) {
        LoadingDialog(
            title = "Enviando fotos",
            message = "Aguarde enquanto as imagens e os dados sao enviados."
        )
    }

    if (isProcessingPlate) {
        LoadingDialog(
            title = "Lendo placa",
            message = "Processando a foto da placa com OCR."
        )
    }

    if (showPlateWebDialog) {
        val normalizedPlate = plateWebDialogPlate.ifBlank { plate }.uppercase().replace(Regex("[^A-Z0-9]"), "").take(7)
        PlateWebDialog(
            plate = normalizedPlate,
            configuredUrl = plateWebUrl,
            autoCloseSeconds = plateWebAutoCloseSeconds,
            onVehicleFound = { vehicleBrand, vehicleModel ->
                plate = normalizedPlate
                brand = vehicleBrand
                model = vehicleModel
                selectedBrandCode = ""
                selectedModelCode = ""
                brandSuggestions = emptyList()
                modelSuggestions = emptyList()
                showPlateWebDialog = false
                plateWebDialogPlate = ""
                showMessage("Marca/modelo preenchidos pelo WebView.")
            },
            onDismiss = {
                showPlateWebDialog = false
                plateWebDialogPlate = ""
            }
        )
    }

    val startCapture: (PhotoType) -> Unit = { type ->
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCameraPermission) {
            pendingPhotoType = type
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            val output = context.createImageTarget(type)
            pendingPhotoType = type
            pendingPhotoUri = output.uri
            pendingPhotoFilePath = output.file.absolutePath
            takePictureLauncher.launch(output.uri)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (authToken.isBlank()) "iPlate | Login"
                        else "iPlate - $operatorName"
                    )
                },
                actions = {
                    IconButton(onClick = {
                        settingsPasswordInput = ""
                        settingsPasswordError = ""
                        showSettingsPasswordDialog = true
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Configurar servidor")
                    }
                    if (authToken.isNotBlank()) {
                        IconButton(onClick = {
                            authToken = ""
                            operatorName = ""
                            preferences.edit()
                                .remove(PREF_AUTH_TOKEN)
                                .remove(PREF_USER_NAME)
                                .apply()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sair")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (authToken.isBlank()) {
            LoginScreen(
                padding = padding,
                username = loginUsername,
                password = loginPassword,
                isLoading = isLoggingIn,
                onUsernameChange = { loginUsername = it },
                onPasswordChange = { loginPassword = it },
                onLogin = {
                    scope.launch {
                        isLoggingIn = true
                        val result = loginOperator(serverUrl, loginUsername, loginPassword)
                        isLoggingIn = false
                        if (result.success) {
                            authToken = result.token
                            operatorName = result.userName
                            usernameSaved = result.username
                            preferences.edit()
                                .putString(PREF_AUTH_TOKEN, result.token)
                                .putString(PREF_USER_NAME, result.userName)
                                .putString(PREF_USERNAME, result.username)
                                .apply()
                            loginPassword = ""
                        }
                        showMessage(result.message)
                    }
                }
            )
        } else {
            EntryScreen(
                padding = padding,
                plate = plate,
                reservation = reservation,
                reservationDetails = selectedReservationDetails,
                reservationSuggestions = reservationSuggestions,
                reservationExpanded = reservationExpanded,
                brand = brand,
                model = model,
                platePhotoUri = platePhotoUri,
                platePhotoDebug = platePhotoDebug,
                frontPhotoUri = frontPhotoUri,
                driverSidePhotoUri = driverSidePhotoUri,
                rearPhotoUri = rearPhotoUri,
                passengerSidePhotoUri = passengerSidePhotoUri,
                isSending = isSending,
                onPlateChange = { plate = it.uppercase().take(10) },
                onReservationExpandedChange = { reservationExpanded = it },
                onReservationChange = { value ->
                    reservation = value
                    selectedReservationId = 0
                    selectedExpressoReservationId = ""
                    selectedReservationDetails = null
                    scope.launch {
                        if (value.trim().length < 3) {
                            reservationSuggestions = emptyList()
                            reservationExpanded = false
                            return@launch
                        }

                        val result = searchReservations(serverUrl, authToken, value)
                        reservationSuggestions = result
                        reservationExpanded = result.isNotEmpty()
                    }
                },
                onReservationSubmit = {
                    scope.launch {
                        if (reservation.trim().length >= 3) {
                            val result = searchReservations(serverUrl, authToken, reservation)
                            reservationSuggestions = result
                            reservationExpanded = result.isNotEmpty()
                        }
                    }
                    lookupReservationDetails(reservation, true)
                },
                onReservationSelect = { option ->
                    reservation = option.reservationCode
                    selectedReservationId = option.reservationId
                    selectedExpressoReservationId = ""
                    selectedReservationDetails = null
                    reservationSuggestions = emptyList()
                    reservationExpanded = false
                },
                onReservationScan = {
                    qrScanLauncher.launch(
                        ScanOptions()
                            .setPrompt("Aponte a camera para o QR Code da reserva")
                            .setBeepEnabled(true)
                            .setOrientationLocked(false)
                            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    )
                },
                brandSuggestions = brandSuggestions,
                modelSuggestions = modelSuggestions,
                brandExpanded = brandExpanded,
                modelExpanded = modelExpanded,
                selectedBrandCode = selectedBrandCode,
                onBrandExpandedChange = { brandExpanded = it },
                onModelExpandedChange = { modelExpanded = it },
                onBrandChange = { value ->
                    brand = value
                    selectedBrandCode = ""
                    model = ""
                    selectedModelCode = ""
                    modelSuggestions = emptyList()
                    modelExpanded = false

                    scope.launch {
                        if (value.trim().length < 3) {
                            brandSuggestions = emptyList()
                            brandExpanded = false
                            return@launch
                        }

                        val brands = if (allBrandsCache.isEmpty()) {
                            fetchFipeBrands().also { allBrandsCache = it }
                        } else {
                            allBrandsCache
                        }

                        brandSuggestions = brands
                            .filter { it.name.contains(value.trim(), ignoreCase = true) }
                            .take(20)
                        brandExpanded = brandSuggestions.isNotEmpty()
                    }
                },
                onBrandSelect = { option ->
                    brand = option.name
                    selectedBrandCode = option.code
                    brandSuggestions = emptyList()
                    brandExpanded = false
                    model = ""
                    selectedModelCode = ""
                    modelSuggestions = emptyList()
                },
                onModelChange = { value ->
                    model = value
                    selectedModelCode = ""

                    scope.launch {
                        if (selectedBrandCode.isBlank() || value.trim().length < 3) {
                            modelSuggestions = emptyList()
                            modelExpanded = false
                            return@launch
                        }

                        val models = modelCache[selectedBrandCode] ?: fetchFipeModels(selectedBrandCode).also {
                            modelCache = modelCache + (selectedBrandCode to it)
                        }

                        modelSuggestions = models
                            .filter { it.name.contains(value.trim(), ignoreCase = true) }
                            .take(20)
                        modelExpanded = modelSuggestions.isNotEmpty()
                    }
                },
                onModelSelect = { option ->
                    model = option.name
                    selectedModelCode = option.code
                    modelSuggestions = emptyList()
                    modelExpanded = false
                },
                onPlateCapture = { startCapture(PhotoType.PLATE) },
                onPlateWebLookup = {
                    val normalizedPlate = plate.uppercase().replace(Regex("[^A-Z0-9]"), "").take(7)
                    if (normalizedPlate.length < 7) {
                        showMessage("Digite uma placa valida para consultar na web.")
                    } else {
                        plateWebDialogPlate = normalizedPlate
                        showPlateWebDialog = true
                    }
                },
                onFrontCapture = { startCapture(PhotoType.FRONT) },
                onDriverSideCapture = { startCapture(PhotoType.DRIVER_SIDE) },
                onRearCapture = { startCapture(PhotoType.REAR) },
                onPassengerSideCapture = { startCapture(PhotoType.PASSENGER_SIDE) },
                onSubmit = {
                    scope.launch {
                        isSending = true
                        val result = uploadEntry(
                            context = context,
                            serverUrl = serverUrl,
                            authToken = authToken,
                            plate = plate,
                            reservation = reservation,
                            brand = brand,
                            model = model,
                            compressUploads = compressUploads,
                            expressoTokenEndpoint = expressoTokenEndpoint,
                            expressoAttachmentEndpoint = expressoAttachmentEndpoint,
                            expressoUser = expressoUser,
                            expressoPassword = expressoPassword,
                            expressoReservationId = selectedExpressoReservationId,
                            plateFromOcr = plateFromOcr,
                            platePhoto = platePhotoUri,
                            frontPhoto = frontPhotoUri,
                            driverSidePhoto = driverSidePhotoUri,
                            rearPhoto = rearPhotoUri,
                            passengerSidePhoto = passengerSidePhotoUri
                        )
                        isSending = false

                        if (result.success) {
                            plate = ""
                            plateFromOcr = ""
                            reservation = ""
                            selectedReservationId = 0
                            selectedExpressoReservationId = ""
                            selectedReservationDetails = null
                            brand = ""
                            model = ""
                            selectedBrandCode = ""
                            selectedModelCode = ""
                            brandSuggestions = emptyList()
                            modelSuggestions = emptyList()
                            platePhotoUri = null
                            platePhotoDebug = ""
                            frontPhotoUri = null
                            driverSidePhotoUri = null
                            rearPhotoUri = null
                            passengerSidePhotoUri = null
                        } else if (result.message.contains("Sessao", ignoreCase = true)) {
                            authToken = ""
                            operatorName = ""
                            preferences.edit()
                                .remove(PREF_AUTH_TOKEN)
                                .remove(PREF_USER_NAME)
                                .apply()
                        }
                        showMessage(result.message)
                    }
                }
            )
        }
    }
}

@Composable
private fun LoginScreen(
    padding: PaddingValues,
    username: String,
    password: String,
    isLoading: Boolean,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFF7F3EC))
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF7)),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Console do operador",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Cada entrada enviada pelo app fica vinculada ao usuario autenticado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF68584A)
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = onUsernameChange,
                    label = { Text("Usuario") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Senha") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                Button(
                    onClick = onLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading
                ) {
                    Text(if (isLoading) "Entrando..." else "Entrar")
                }
            }
        }
    }
}

@Composable
private fun LoadingDialog(title: String, message: String) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(title) },
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                Text(message)
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF68584A)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private class PlateWebBridge(
    private val onVehicleFound: (String, String) -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var delivered = false

    @JavascriptInterface
    fun setVehicle(brand: String, model: String) {
        if (delivered) {
            return
        }
        delivered = true
        mainHandler.post {
            onVehicleFound(brand, model)
        }
    }
}

@Composable
private fun PlateWebDialog(
    plate: String,
    configuredUrl: String,
    autoCloseSeconds: Int,
    onVehicleFound: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val targetUrl = configuredUrl.ifBlank { DEFAULT_PLATE_WEB_URL }.replace("{plate}", plate)
    LaunchedEffect(targetUrl, autoCloseSeconds) {
        if (autoCloseSeconds > 0) {
            delay(autoCloseSeconds * 1000L)
            onDismiss()
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Consulta web $plate") },
        text = {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(560.dp),
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView, url: String?) {
                                super.onPageFinished(view, url)
                                view.evaluateJavascript(plateReaderModeScript(plate), null)
                            }
                        }
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        addJavascriptInterface(
                            PlateWebBridge(onVehicleFound),
                            "IPlate"
                        )
                        loadUrl(targetUrl)
                    }
                },
                update = { webView ->
                    if (webView.url != targetUrl) {
                        webView.loadUrl(targetUrl)
                    }
                }
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Fechar")
            }
        }
    )
}

private fun plateReaderModeScript(plate: String): String = """
(function () {
  const requestedPlate = '${plate.replace("'", "")}';
  const labels = ['Marca', 'Modelo', 'Importado', 'Ano', 'Ano Modelo', 'Cor', 'Cilindrada', 'Potencia', 'Combustivel', 'Chassi', 'Motor', 'Passageiros', 'UF', 'Municipio', 'Segmento', 'Especie Veiculo', 'Especie'];
  const labelMap = new Map(labels.map(label => [normalize(label), label]));
  let attempts = 0;

  function normalize(value) {
    return (value || '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(':', '')
      .replace(/\s+/g, ' ')
      .trim()
      .toLowerCase();
  }

  function escapeHtml(value) {
    return String(value || '').replace(/[&<>"']/g, function (char) {
      return ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char];
    });
  }

  function cleanValue(value) {
    return String(value || '').replace(/\s+/g, ' ').trim();
  }

  function addRow(rows, seen, rawLabel, rawValue) {
    const label = labelMap.get(normalize(rawLabel));
    const value = cleanValue(rawValue);
    if (label && value && !seen.has(label)) {
      rows.push([label, value]);
      seen.add(label);
    }
  }

  function collectRowsFromTables() {
    const rows = [];
    const seen = new Set();
    const tableRows = Array.from(document.querySelectorAll('table.fipeTablePriceDetail tr, table tr'));

    tableRows.forEach(function (tr) {
      const cells = Array.from(tr.querySelectorAll('th,td'));
      if (cells.length < 2) return;
      addRow(rows, seen, cells[0].innerText, cells[1].innerText);
    });

    return rows;
  }

  function collectRowsFromText() {
    const rows = [];
    const seen = new Set();
    const lines = (document.body.innerText || '').split('\n').map(line => line.trim()).filter(Boolean);
    const wanted = new Set(labels.map(label => normalize(label)));

    for (const line of lines) {
      const inlineMatch = line.match(/^([^:]{2,40}):\s*(.+)$/);
      if (inlineMatch) {
        addRow(rows, seen, inlineMatch[1], inlineMatch[2]);
      }
    }

    for (let i = 0; i < lines.length; i++) {
      const label = labelMap.get(normalize(lines[i]));
      if (!label || seen.has(label)) continue;

      for (let j = i + 1; j < Math.min(lines.length, i + 6); j++) {
        const candidate = lines[j].trim();
        if (!candidate || wanted.has(normalize(candidate))) break;
        if (/^(publicidade|anuncio|buscar|consultar|compartilhar)$/i.test(normalize(candidate))) continue;
        addRow(rows, seen, label, candidate);
        break;
      }
    }

    return rows;
  }

  function collectRows() {
    const tableRows = collectRowsFromTables();
    if (tableRows.some(row => row[0] === 'Marca') && tableRows.some(row => row[0] === 'Modelo')) {
      return tableRows;
    }
    const textRows = collectRowsFromText();
    if (textRows.some(row => row[0] === 'Marca') && textRows.some(row => row[0] === 'Modelo')) {
      return textRows;
    }

    const metaDescription = document.querySelector('meta[name="description"]')?.content || '';
    const metaMatch = metaDescription.match(/carro\s+([A-Z0-9]+)\s+(.+?)\s+\d{4}/i);
    if (metaMatch) {
      return [['Marca', metaMatch[1].trim()], ['Modelo', metaMatch[2].trim()]];
    }

    const title = document.title || '';
    const titleMatch = title.match(/-\s*([A-Z0-9]+)\s+(.+?)\s+\d{4}/i);
    if (titleMatch) {
      return [['Marca', titleMatch[1].trim()], ['Modelo', titleMatch[2].trim()]];
    }

    return textRows;
  }

  function renderOnlyTable(rows) {
    const brandRow = rows.find(row => row[0] === 'Marca');
    const modelRow = rows.find(row => row[0] === 'Modelo');
    if (brandRow && modelRow && window.IPlate) {
      window.IPlate.setVehicle(String(brandRow[1] || ''), String(modelRow[1] || ''));
    }

    const tableRows = rows.map(row => `<tr><th>${'$'}{escapeHtml(row[0])}</th><td>${'$'}{escapeHtml(row[1])}</td></tr>`).join('');
    document.documentElement.innerHTML = `
      <head>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
          * { box-sizing: border-box !important; }
          html, body { margin: 0 !important; padding: 0 !important; background: #fff !important; color: #111 !important; width: 100% !important; min-height: 100% !important; overflow-x: hidden !important; }
          body { font-family: Arial, sans-serif !important; }
          main { padding: 12px !important; width: 100% !important; }
          table { width: 100% !important; border-collapse: collapse !important; table-layout: fixed !important; }
          th, td { border-bottom: 1px solid #e5e5e5 !important; padding: 10px 6px !important; text-align: left !important; vertical-align: top !important; font-size: 18px !important; line-height: 1.25 !important; }
          th { width: 42% !important; font-weight: 700 !important; color: #111 !important; }
          td { width: 58% !important; overflow-wrap: anywhere !important; }
        </style>
      </head>
      <body><main><table>${'$'}{tableRows}</table></main></body>
    `;
  }

  function run() {
    const rows = collectRows();
    if (rows.length >= 2) {
      renderOnlyTable(rows);
      return;
    }

    attempts += 1;
    if (attempts < 20) {
      setTimeout(run, 500);
    }
  }

  run();
})();
""".trimIndent()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryScreen(
    padding: PaddingValues,
    plate: String,
    reservation: String,
    reservationDetails: ReservationLookupResult?,
    reservationSuggestions: List<ReservationOption>,
    reservationExpanded: Boolean,
    brand: String,
    model: String,
    platePhotoUri: String?,
    platePhotoDebug: String,
    brandSuggestions: List<FipeOption>,
    modelSuggestions: List<FipeOption>,
    brandExpanded: Boolean,
    modelExpanded: Boolean,
    selectedBrandCode: String,
    frontPhotoUri: String?,
    driverSidePhotoUri: String?,
    rearPhotoUri: String?,
    passengerSidePhotoUri: String?,
    isSending: Boolean,
    onPlateChange: (String) -> Unit,
    onReservationExpandedChange: (Boolean) -> Unit,
    onReservationChange: (String) -> Unit,
    onReservationSubmit: () -> Unit,
    onReservationSelect: (ReservationOption) -> Unit,
    onReservationScan: () -> Unit,
    onBrandExpandedChange: (Boolean) -> Unit,
    onModelExpandedChange: (Boolean) -> Unit,
    onBrandChange: (String) -> Unit,
    onBrandSelect: (FipeOption) -> Unit,
    onModelChange: (String) -> Unit,
    onModelSelect: (FipeOption) -> Unit,
    onPlateCapture: () -> Unit,
    onPlateWebLookup: () -> Unit,
    onFrontCapture: () -> Unit,
    onDriverSideCapture: () -> Unit,
    onRearCapture: () -> Unit,
    onPassengerSideCapture: () -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .background(Color(0xFFF7F3EC))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = reservationExpanded,
                    onExpandedChange = { onReservationExpandedChange(it) }
                ) {
                    OutlinedTextField(
                        value = reservation,
                        onValueChange = onReservationChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyUp && (event.key == Key.Enter || event.key == Key.NumPadEnter)) {
                                    onReservationSubmit()
                                    true
                                } else {
                                    false
                                }
                            },
                        label = { Text("Reserva") },
                        placeholder = { Text("Reserva / Nome") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = { onReservationSubmit() },
                            onDone = { onReservationSubmit() }
                        ),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = reservationExpanded)
                        },
                        singleLine = true
                    )
                    DropdownMenu(
                        expanded = reservationExpanded && reservationSuggestions.isNotEmpty(),
                        onDismissRequest = { onReservationExpandedChange(false) },
                        properties = PopupProperties(focusable = false)
                    ) {
                        reservationSuggestions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(option.displayLabel)
                                        Text(
                                            text = "Check-in: ${option.checkinDate} | ${option.status}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF68584A)
                                        )
                                    }
                                },
                                onClick = { onReservationSelect(option) }
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onReservationScan,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan QR Code")
                    }
                }
                reservationDetails?.let { details ->
                    ReservationDetailsTable(details = details)
                }
                PhotoCard(
                    title = "Foto da placa",
                    uri = platePhotoUri,
                    onClick = onPlateCapture
                )
                if (platePhotoDebug.isNotBlank()) {
                    Text(
                        text = platePhotoDebug,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF68584A)
                    )
                }
                OutlinedTextField(
                    value = plate,
                    onValueChange = onPlateChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Placa") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Characters
                    ),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onPlateCapture,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Foto")
                    }
                    FilledTonalButton(
                        onClick = onPlateWebLookup,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Web")
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = brandExpanded,
                    onExpandedChange = { onBrandExpandedChange(it) }
                ) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = onBrandChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        label = { Text("Marca") },
                        placeholder = { Text("Digite 3 letras para buscar") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandExpanded)
                        },
                        singleLine = true
                    )

                    DropdownMenu(
                        expanded = brandExpanded && brandSuggestions.isNotEmpty(),
                        onDismissRequest = { onBrandExpandedChange(false) },
                        properties = PopupProperties(focusable = false)
                    ) {
                        brandSuggestions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name) },
                                onClick = { onBrandSelect(option) }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = modelExpanded,
                    onExpandedChange = { onModelExpandedChange(it) }
                ) {
                    OutlinedTextField(
                        value = model,
                        onValueChange = onModelChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        label = { Text("Modelo") },
                        placeholder = {
                            Text(
                                if (selectedBrandCode.isBlank()) "Selecione a marca primeiro"
                                else "Digite 3 letras para buscar"
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelExpanded)
                        },
                        singleLine = true,
                        enabled = selectedBrandCode.isNotBlank()
                    )

                    DropdownMenu(
                        expanded = modelExpanded && modelSuggestions.isNotEmpty(),
                        onDismissRequest = { onModelExpandedChange(false) },
                        properties = PopupProperties(focusable = false)
                    ) {
                        modelSuggestions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name) },
                                onClick = { onModelSelect(option) }
                            )
                        }
                    }
                }
            }
        }

        PhotoCard(
            title = "1. Foto da frente",
            uri = frontPhotoUri,
            onClick = onFrontCapture
        )
        PhotoCard(
            title = "2. Foto da lateral do motorista",
            uri = driverSidePhotoUri,
            onClick = onDriverSideCapture
        )
        PhotoCard(
            title = "3. Foto da traseira",
            uri = rearPhotoUri,
            onClick = onRearCapture
        )
        PhotoCard(
            title = "4. Foto do lado do carona",
            uri = passengerSidePhotoUri,
            onClick = onPassengerSideCapture
        )

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isSending,
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            Icon(Icons.Default.CloudUpload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isSending) "Enviando..." else "Salvar entrada")
        }

    }
}

@Composable
private fun PhotoCard(
    title: String,
    uri: String?,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val imageBitmap = remember(uri) {
        uri?.let { value ->
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(value))?.use { input ->
                    BitmapFactory.decodeStream(input)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFF1E8DB)),
                contentAlignment = Alignment.Center
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color(0xFFA84714),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nenhuma foto capturada",
                            color = Color(0xFF68584A)
                        )
                    }
                }
            }
            FilledTonalButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tirar foto")
            }
        }
    }
}

@Composable
private fun ReservationDetailsTable(details: ReservationLookupResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFFCF7))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReservationDetailsRow("Hospede", details.guestName.ifBlank { "-" })
        ReservationDetailsRow("CPF", details.guestCpf.ifBlank { "-" })
        ReservationDetailsRow(
            "Check-in",
            formatDateForDisplay(details.checkinDate).ifBlank { "-" },
            valueColor = if (details.isOutOfDate) Color(0xFFB42318) else Color(0xFF067647)
        )
        ReservationDetailsRow("UH", details.uh.ifBlank { "-" })
        ReservationDetailsRow("Hospedes", "${details.adults.ifBlank { "0" }}/${details.children.ifBlank { "0" }}")
        if (details.isOutOfDate) {
            Text(
                text = "Reserva fora da data atual.",
                color = Color(0xFFB42318),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ReservationDetailsRow(label: String, value: String, valueColor: Color = Color(0xFF1D120A)) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.width(72.dp),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF68584A),
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = valueColor
        )
    }
}

private suspend fun loginOperator(
    serverUrl: String,
    username: String,
    password: String,
): LoginResult = withContext(Dispatchers.IO) {
    if (serverUrl.isBlank()) {
        return@withContext LoginResult(false, "Configure a URL do servidor.")
    }

    if (username.isBlank() || password.isBlank()) {
        return@withContext LoginResult(false, "Informe usuario e senha.")
    }

    val loginUrl = deriveLoginUrl(serverUrl)
    val client = OkHttpClient()
    val body = FormBody.Builder()
        .add("username", username)
        .add("password", password)
        .build()

    val request = Request.Builder()
        .url(loginUrl)
        .post(body)
        .build()

    return@withContext runCatching {
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            val json = runCatching { JSONObject(raw) }.getOrNull()
            if (!response.isSuccessful || json == null || !json.optBoolean("success")) {
                LoginResult(false, json?.optString("message").orEmpty().ifBlank { "Falha no login." })
            } else {
                val user = json.getJSONObject("user")
                LoginResult(
                    success = true,
                    message = json.optString("message", "Login realizado com sucesso."),
                    token = user.optString("api_token"),
                    userName = user.optString("name"),
                    username = user.optString("username")
                )
            }
        }
    }.getOrElse { error ->
        LoginResult(false, error.message ?: "Falha ao conectar ao servidor.")
    }
}

private suspend fun uploadEntry(
    context: Context,
    serverUrl: String,
    authToken: String,
    plate: String,
    reservation: String,
    brand: String,
    model: String,
    compressUploads: Boolean,
    expressoTokenEndpoint: String,
    expressoAttachmentEndpoint: String,
    expressoUser: String,
    expressoPassword: String,
    expressoReservationId: String,
    plateFromOcr: String,
    platePhoto: String?,
    frontPhoto: String?,
    driverSidePhoto: String?,
    rearPhoto: String?,
    passengerSidePhoto: String?,
): ApiResult = withContext(Dispatchers.IO) {
    if (plate.isBlank() || reservation.isBlank() || brand.isBlank() || model.isBlank()) {
        return@withContext ApiResult(false, "Preencha placa, reserva, marca e modelo.")
    }

    if (platePhoto == null || frontPhoto == null || driverSidePhoto == null || rearPhoto == null || passengerSidePhoto == null) {
        return@withContext ApiResult(false, "Capture as 5 fotos obrigatorias na ordem solicitada.")
    }

    if (expressoTokenEndpoint.isBlank() || expressoAttachmentEndpoint.isBlank() || expressoUser.isBlank() || expressoPassword.isBlank()) {
        return@withContext ApiResult(false, "Configure os dados da API Expresso.")
    }

    if (expressoReservationId.isBlank()) {
        return@withContext ApiResult(false, "Consulte a reserva pelo QR Code antes de enviar as fotos.")
    }

    val client = OkHttpClient()
    val formBuilder = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("api_token", authToken)
        .addFormDataPart("plate", plate)
        .addFormDataPart("reservation", reservation)
        .addFormDataPart("brand", brand)
        .addFormDataPart("model", model)
        .addFormDataPart(
            "client_timestamp",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        )

    val imageParts = listOf(
        Triple("plate_photo", Uri.parse(platePhoto), "plate"),
        Triple("front_photo", Uri.parse(frontPhoto), "front"),
        Triple("driver_side_photo", Uri.parse(driverSidePhoto), "driver_side"),
        Triple("rear_photo", Uri.parse(rearPhoto), "rear"),
        Triple("passenger_side_photo", Uri.parse(passengerSidePhoto), "passenger_side")
    )
    for ((fieldName, uri, fallbackName) in imageParts) {
        val imageBuildError = runCatching {
            formBuilder.addImagePart(context, fieldName, uri, fallbackName, compressUploads)
        }.exceptionOrNull()
        if (imageBuildError != null) {
            val debug = diagnosePhotoUri(context, uri)
            return@withContext ApiResult(
                false,
                "Nao foi possivel ler a foto ${friendlyPhotoName(fieldName)}. Tire novamente essa foto e tente enviar. Causa: ${imageBuildError.message ?: imageBuildError.javaClass.simpleName}. [$debug]"
            )
        }
    }

    var backendMessage = "Envio iPlate ignorado."
    if (serverUrl.isNotBlank() && authToken.isNotBlank()) {
        val request = Request.Builder()
            .url(serverUrl)
            .post(formBuilder.build())
            .build()
        val backendResult = runCatching {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                val json = runCatching { JSONObject(raw) }.getOrNull()
                if (!response.isSuccessful) {
                    ApiResult(false, json?.optString("message").orEmpty().ifBlank { "Falha no envio iPlate: HTTP ${response.code}" })
                } else {
                    ApiResult(true, json?.optString("message").orEmpty().ifBlank { "Entrada enviada no iPlate." })
                }
            }
        }.getOrElse { error ->
            ApiResult(false, error.message ?: "Falha ao conectar no iPlate.")
        }
        backendMessage = backendResult.message
    }

    val expressoToken = obtainExpressoToken(
        client = client,
        tokenEndpointUrl = expressoTokenEndpoint,
        apiUser = expressoUser,
        apiPassword = expressoPassword
    ) ?: return@withContext ApiResult(false, "Entrada gravada no iPlate, mas nao foi possivel obter token da API Expresso.")

    val photos = listOf(
        AttachmentPhoto(
            "plate_photo",
            Uri.parse(platePhoto),
            "plate",
            buildString {
                append("Foto da placa")
                val ocrPlate = plateFromOcr.uppercase().replace(Regex("[^A-Z0-9]"), "").take(7)
                if (ocrPlate.isNotBlank()) {
                    append(" - ")
                    append(ocrPlate)
                }
            }
        ),
        AttachmentPhoto("front_photo", Uri.parse(frontPhoto), "front", "Foto da frente"),
        AttachmentPhoto("driver_side_photo", Uri.parse(driverSidePhoto), "driver_side", "Foto lateral do motorista"),
        AttachmentPhoto("rear_photo", Uri.parse(rearPhoto), "rear", "Foto da traseira"),
        AttachmentPhoto("passenger_side_photo", Uri.parse(passengerSidePhoto), "passenger_side", "Foto do lado do carona"),
    )

    for (photo in photos) {
        val uploaded = uploadExpressoAttachment(
            client = client,
            context = context,
            attachmentEndpointUrl = expressoAttachmentEndpoint,
            token = expressoToken,
            reservationId = expressoReservationId,
            photo = photo,
            compressUploads = compressUploads
        )
        if (!uploaded.success) {
            return@withContext uploaded
        }
    }

    return@withContext ApiResult(true, "Fotos enviadas ao Expresso com sucesso. $backendMessage")
}

private fun deriveLoginUrl(serverUrl: String): String
{
    val marker = "/api/"
    val markerIndex = serverUrl.indexOf(marker)
    return if (markerIndex >= 0) {
        serverUrl.substring(0, markerIndex + marker.length) + "login.php"
    } else {
        serverUrl.trimEnd('/') + "/api/login.php"
    }
}

private suspend fun detectPlateFromImage(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    val image = InputImage.fromFilePath(context, uri)
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    return@withContext try {
        val result = recognizer.process(image).awaitResult()
        extractBrazilianPlate(result.text)
    } finally {
        recognizer.close()
    }
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitResult(): T =
    kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) {
                continuation.resume(result)
            }
        }
        addOnFailureListener { error ->
            if (continuation.isActive) {
                continuation.resumeWithException(error)
            }
        }
        addOnCanceledListener {
            continuation.cancel()
        }
    }

private fun extractBrazilianPlate(rawText: String): String? {
    val plateRegex = Regex("[A-Z]{3}[0-9][A-Z0-9][0-9]{2}")
    val candidates = rawText
        .uppercase()
        .lineSequence()
        .flatMap { line ->
            sequence {
                yield(line)
                yield(line.replace(" ", ""))
                yield(line.replace("-", ""))
            }
        }
        .map { candidate -> candidate.replace(Regex("[^A-Z0-9]"), "") }
        .filter { it.length >= 7 }
        .toList()

    for (candidate in candidates) {
        val normalized = normalizePlateCandidate(candidate)
        val match = plateRegex.find(normalized)
        if (match != null) {
            return match.value
        }
    }

    return null
}

private suspend fun fetchFipeBrands(): List<FipeOption> = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://parallelum.com.br/fipe/api/v1/carros/marcas")
        .build()

    return@withContext runCatching {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                emptyList()
            } else {
                val body = response.body?.string().orEmpty()
                val json = JSONArray(body)
                buildList {
                    for (index in 0 until json.length()) {
                        val item = json.getJSONObject(index)
                        add(
                            FipeOption(
                                code = item.optString("codigo"),
                                name = item.optString("nome")
                            )
                        )
                    }
                }
            }
        }
    }.getOrDefault(emptyList())
}

private suspend fun fetchFipeModels(brandCode: String): List<FipeOption> = withContext(Dispatchers.IO) {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("https://parallelum.com.br/fipe/api/v1/carros/marcas/$brandCode/modelos")
        .build()

    return@withContext runCatching {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                emptyList()
            } else {
                val body = response.body?.string().orEmpty()
                val json = JSONObject(body).optJSONArray("modelos") ?: JSONArray()
                buildList {
                    for (index in 0 until json.length()) {
                        val item = json.getJSONObject(index)
                        add(
                            FipeOption(
                                code = item.optString("codigo"),
                                name = item.optString("nome")
                            )
                        )
                    }
                }
            }
        }
    }.getOrDefault(emptyList())
}

private suspend fun searchReservations(
    serverUrl: String,
    authToken: String,
    term: String,
): List<ReservationOption> = withContext(Dispatchers.IO) {
    if (serverUrl.isBlank() || authToken.isBlank() || term.trim().length < 3) {
        return@withContext emptyList()
    }

    val client = OkHttpClient()
    val endpoint = deriveReservationSearchUrl(serverUrl, authToken, term)
    val request = Request.Builder()
        .url(endpoint)
        .build()

    return@withContext runCatching {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                emptyList()
            } else {
                val raw = response.body?.string().orEmpty()
                val json = JSONObject(raw)
                val items = json.optJSONArray("items") ?: JSONArray()
                buildList {
                    for (index in 0 until items.length()) {
                        val item = items.getJSONObject(index)
                        add(
                            ReservationOption(
                                reservationId = item.optInt("reservation_id"),
                                reservationCode = item.optString("reservation_code"),
                                guestName = item.optString("guest_name"),
                                status = item.optString("status"),
                                checkinDate = item.optString("checkin_date")
                            )
                        )
                    }
                }
            }
        }
    }.getOrDefault(emptyList())
}

private suspend fun fetchReservationDetails(
    tokenEndpointUrl: String,
    reservationEndpointUrl: String,
    apiUser: String,
    apiPassword: String,
    reservationCode: String,
): ReservationFetchResult = withContext(Dispatchers.IO) {
    if (tokenEndpointUrl.isBlank() || reservationEndpointUrl.isBlank() || apiUser.isBlank() || apiPassword.isBlank() || reservationCode.isBlank()) {
        return@withContext ReservationFetchResult(message = "Configure os dados da API Expresso.")
    }

    val client = OkHttpClient()
    val token = obtainExpressoToken(
        client = client,
        tokenEndpointUrl = tokenEndpointUrl,
        apiUser = apiUser,
        apiPassword = apiPassword
    ) ?: return@withContext ReservationFetchResult(message = "Nao foi possivel obter token da API Expresso.")

    val body = JSONObject()
        .put("token", token)
        .put("numero_reserva", reservationCode)
        .toString()
        .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
    val request = Request.Builder()
        .url(reservationEndpointUrl)
        .post(body)
        .build()

    return@withContext runCatching {
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                ReservationFetchResult(message = "Falha ao consultar reserva: HTTP ${response.code}.")
            } else {
                val root = JSONObject(raw)
                val parsed = parseReservationLookupResult(root, reservationCode)
                if (parsed == null) {
                    ReservationFetchResult(message = root.optString("msg").ifBlank { "Reserva nao encontrada na API Expresso." })
                } else {
                    ReservationFetchResult(details = parsed)
                }
            }
        }
    }.getOrElse { error ->
        ReservationFetchResult(message = error.message ?: "Falha ao consultar reserva na API Expresso.")
    }
}

private fun obtainExpressoToken(
    client: OkHttpClient,
    tokenEndpointUrl: String,
    apiUser: String,
    apiPassword: String,
): String? {
    val body = JSONObject()
        .put("user", apiUser)
        .put("password", apiPassword)
        .toString()
        .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
    val request = Request.Builder()
        .url(tokenEndpointUrl)
        .post(body)
        .build()

    return runCatching {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                null
            } else {
                val raw = response.body?.string().orEmpty()
                JSONObject(raw).optString("token").takeIf { it.isNotBlank() && it != "null" }
            }
        }
    }.getOrNull()
}

private fun uploadExpressoAttachment(
    client: OkHttpClient,
    context: Context,
    attachmentEndpointUrl: String,
    token: String,
    reservationId: String,
    photo: AttachmentPhoto,
    compressUploads: Boolean,
): ApiResult {
    val file = context.copyUriToCache(
        uri = photo.uri,
        fallbackName = photo.fallbackName,
        compressUploads = compressUploads,
        profile = compressionProfileFor(photo.fieldName)
    )
    val mimeType = if (compressUploads) "image/jpeg" else (context.contentResolver.getType(photo.uri) ?: "image/jpeg")
    val body = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("token", token)
        .addFormDataPart("ref_table", "reservas")
        .addFormDataPart("ref_id", reservationId)
        .addFormDataPart("detalhes", photo.details)
        .addFormDataPart("tags[]", "475")
        .addFormDataPart("file", file.name, file.asRequestBody(mimeType.toMediaTypeOrNull()))
        .build()

    val request = Request.Builder()
        .url(attachmentEndpointUrl)
        .post(body)
        .build()

    return runCatching {
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            val json = runCatching { JSONObject(raw) }.getOrNull()
            val status = json?.optString("status").orEmpty()
            val success = response.isSuccessful && !status.equals("fail", ignoreCase = true)
            if (success) {
                ApiResult(true, "Anexo enviado.")
            } else {
                ApiResult(
                    false,
                    json?.optString("msg")
                        ?.ifBlank { json.optString("message") }
                        ?.ifBlank { "Falha ao enviar ${photo.details}: HTTP ${response.code}" }
                        ?: "Falha ao enviar ${photo.details}: HTTP ${response.code}"
                )
            }
        }
    }.getOrElse { error ->
        ApiResult(false, error.message ?: "Falha ao enviar ${photo.details}.")
    }
}

private fun deriveReservationSearchUrl(serverUrl: String, authToken: String, term: String): String {
    val marker = "/api/"
    val markerIndex = serverUrl.indexOf(marker)
    val base = if (markerIndex >= 0) {
        serverUrl.substring(0, markerIndex + marker.length)
    } else {
        serverUrl.trimEnd('/') + "/api/"
    }

    return base + "reservation-search.php?api_token=" +
        Uri.encode(authToken) +
        "&term=" +
        Uri.encode(term)
}

private fun parseReservationLookupResult(root: JSONObject, reservationCode: String): ReservationLookupResult? {
    if (root.optString("status").equals("fail", ignoreCase = true)) {
        return null
    }

    val data = when {
        root.has("data") && root.optJSONObject("data") != null -> root.optJSONObject("data")
        root.has("reserva") && root.optJSONObject("reserva") != null -> root.optJSONObject("reserva")
        else -> root
    } ?: return null

    return ReservationLookupResult(
        reservationId = readJsonString(data, "reserva_id", "reservation_id", "id"),
        reservationCode = readJsonString(data, "numero_reserva", "reserva", "reservation", "codigo", "code")
            .ifBlank { reservationCode },
        guestName = readJsonString(data, "hospede_nome", "nome_hospede_principal", "hospede_principal", "guest_name", "nome"),
        guestCpf = formatCpf(readJsonString(data, "hospede_cpf", "cpf", "guest_cpf")),
        checkinDate = readJsonString(data, "data_checkin", "checkin", "checkin_date"),
        adults = readJsonString(data, "adultos", "adults", "numero_adultos", "qt_pessoas"),
        children = readJsonString(data, "criancas", "children", "numero_criancas"),
        uh = readJsonString(data, "uh", "unidade_habitacional", "room", "apartamento"),
    )
}

private fun readJsonString(json: JSONObject, vararg keys: String): String {
    for (key in keys) {
        val value = json.opt(key)
        if (value != null && value != JSONObject.NULL) {
            val text = value.toString().trim()
            if (text.isNotEmpty()) {
                return text
            }
        }
    }

    return ""
}

private fun formatCpf(value: String): String {
    val digits = value.onlyDigits()
    if (digits.length != 11) {
        return value
    }

    return "${digits.substring(0, 3)}.${digits.substring(3, 6)}.${digits.substring(6, 9)}-${digits.substring(9)}"
}

private fun isToday(checkinDate: String): Boolean {
    if (checkinDate.isBlank()) {
        return false
    }

    val today = LocalDate.now()
    val normalized = checkinDate.trim()

    val parsers = listOf<() -> LocalDate?>(
        { runCatching { LocalDate.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull() },
        { runCatching { LocalDate.parse(normalized.take(10)) }.getOrNull() },
        { runCatching { LocalDate.parse(normalized.take(10), DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrNull() },
        { runCatching { LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toLocalDate() }.getOrNull() },
        { runCatching { LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")).toLocalDate() }.getOrNull() },
    )

    return parsers.any { it() == today }
}

private fun formatDateForDisplay(value: String): String {
    if (value.isBlank()) {
        return value
    }
    val normalized = value.trim()
    val parsers = listOf<() -> LocalDate?>(
        { runCatching { LocalDate.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull() },
        { runCatching { LocalDate.parse(normalized.take(10)) }.getOrNull() },
        { runCatching { LocalDate.parse(normalized.take(10), DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrNull() },
        { runCatching { LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toLocalDate() }.getOrNull() },
        { runCatching { LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")).toLocalDate() }.getOrNull() },
    )
    val parsed = parsers.firstNotNullOfOrNull { it() } ?: return value
    return parsed.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}

private fun normalizePlateCandidate(candidate: String): String {
    val source = candidate.take(7).toCharArray()
    if (source.size < 7) {
        return candidate
    }

    source[0] = source[0].digitToLetter()
    source[1] = source[1].digitToLetter()
    source[2] = source[2].digitToLetter()
    source[3] = source[3].letterToDigit()
    source[5] = source[5].letterToDigit()
    source[6] = source[6].letterToDigit()

    return String(source)
}

private fun Char.digitToLetter(): Char = when (this) {
    '0' -> 'O'
    '1' -> 'I'
    '2' -> 'Z'
    '4' -> 'A'
    '5' -> 'S'
    '6' -> 'G'
    '8' -> 'B'
    else -> this
}

private fun Char.letterToDigit(): Char = when (this) {
    'O', 'Q', 'D' -> '0'
    'I', 'L' -> '1'
    'Z' -> '2'
    'S' -> '5'
    'B' -> '8'
    else -> this
}

private fun String.onlyDigits(): String = filter(Char::isDigit)

private fun MultipartBody.Builder.addImagePart(
    context: Context,
    fieldName: String,
    uri: Uri,
    fallbackName: String,
    compressUploads: Boolean,
) {
    val file = context.copyUriToCache(
        uri = uri,
        fallbackName = fallbackName,
        compressUploads = compressUploads,
        profile = compressionProfileFor(fieldName)
    )
    val mimeType = if (compressUploads) "image/jpeg" else (context.contentResolver.getType(uri) ?: "image/jpeg")
    addFormDataPart(
        fieldName,
        file.name,
        file.asRequestBody(mimeType.toMediaTypeOrNull())
    )
}

private fun compressionProfileFor(fieldName: String): ImageCompressionProfile = when (fieldName) {
    "plate_photo" -> ImageCompressionProfile(maxDimension = 1600, jpegQuality = 70)
    else -> ImageCompressionProfile(maxDimension = 1280, jpegQuality = 50)
}

private fun Context.copyUriToCache(
    uri: Uri,
    fallbackName: String,
    compressUploads: Boolean,
    profile: ImageCompressionProfile,
): File {
    val extension = contentResolver.getType(uri)
        ?.substringAfterLast('/')
        ?.ifBlank { "jpg" }
        ?: "jpg"
    val outputFile = File(cacheDir, "${System.currentTimeMillis()}_$fallbackName.$extension")

    if (!compressUploads) {
        openUriInputStream(uri)?.use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Nao foi possivel ler a foto.")

        return outputFile
    }

    // Try compressed upload first. If decode/compression fails for this device/image,
    // fallback to sending the original bytes instead of failing the whole submission.
    val compressionAttempt = runCatching {
        val compressedFile = File(cacheDir, "${System.currentTimeMillis()}_${fallbackName}_upload.jpg")
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        openUriInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, boundsOptions)
        } ?: throw IllegalStateException("Nao foi possivel ler a foto.")

        var sampleSize = 1
        while (
            boundsOptions.outWidth / sampleSize > profile.maxDimension ||
            boundsOptions.outHeight / sampleSize > profile.maxDimension
        ) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        val bitmap = openUriInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        } ?: throw IllegalStateException("Nao foi possivel decodificar a foto.")

        val scaledBitmap = scaleBitmap(bitmap, profile.maxDimension)
        FileOutputStream(compressedFile).use { output ->
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, profile.jpegQuality, output)
        }
        if (scaledBitmap !== bitmap) {
            scaledBitmap.recycle()
        }
        bitmap.recycle()
        compressedFile
    }
    if (compressionAttempt.isSuccess) {
        return compressionAttempt.getOrThrow()
    }

    openUriInputStream(uri)?.use { input ->
        FileOutputStream(outputFile).use { output ->
            input.copyTo(output)
        }
    } ?: throw IllegalStateException("Nao foi possivel ler a foto.")
    return outputFile
}

private fun Context.openUriInputStream(uri: Uri) =
    when (uri.scheme?.lowercase()) {
        "file" -> {
            val file = uri.path?.let { File(it) }
            if (file != null && file.exists() && file.canRead()) file.inputStream() else null
        }
        else -> contentResolver.openInputStream(uri)
            ?: contentResolver.openAssetFileDescriptor(uri, "r")?.createInputStream()
    }

private fun friendlyPhotoName(fieldName: String): String = when (fieldName) {
    "plate_photo" -> "da placa"
    "front_photo" -> "da frente"
    "driver_side_photo" -> "da lateral do motorista"
    "rear_photo" -> "da traseira"
    "passenger_side_photo" -> "da lateral do carona"
    else -> ""
}

private fun diagnosePhotoUri(context: Context, uri: Uri): String {
    val scheme = uri.scheme.orEmpty()
    return when (scheme.lowercase()) {
        "file" -> {
            val file = uri.path?.let { File(it) }
            if (file == null) {
                "scheme=file path=nulo"
            } else {
                "scheme=file exists=${file.exists()} read=${file.canRead()} size=${if (file.exists()) file.length() else -1} path=${file.absolutePath}"
            }
        }
        "content" -> {
            val afdInfo = runCatching {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                    "afd=true len=${afd.length}"
                } ?: "afd=false"
            }.getOrElse { "afd_error=${it.message}" }
            val streamInfo = runCatching {
                context.contentResolver.openInputStream(uri)?.use { "stream=true" } ?: "stream=false"
            }.getOrElse { "stream_error=${it.message}" }
            "scheme=content $afdInfo $streamInfo uri=$uri"
        }
        else -> "scheme=$scheme uri=$uri"
    }
}

private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val largestSide = maxOf(width, height)

    if (largestSide <= maxDimension) {
        return bitmap
    }

    val ratio = maxDimension.toFloat() / largestSide.toFloat()
    val targetWidth = (width * ratio).toInt().coerceAtLeast(1)
    val targetHeight = (height * ratio).toInt().coerceAtLeast(1)

    return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
}

private data class ImageCaptureTarget(
    val uri: Uri,
    val file: File,
)

private fun Context.createImageTarget(type: PhotoType): ImageCaptureTarget {
    val directory = File(filesDir, "camera").apply { mkdirs() }
    val file = File(directory, "${type.name.lowercase()}_${System.currentTimeMillis()}.jpg")
    val uri = FileProvider.getUriForFile(
        this,
        "$packageName.fileprovider",
        file
    )
    return ImageCaptureTarget(uri = uri, file = file)
}
