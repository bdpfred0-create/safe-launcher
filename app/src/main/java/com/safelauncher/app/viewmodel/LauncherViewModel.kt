package com.safelauncher.app.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safelauncher.app.data.SecurityPreferences
import com.safelauncher.app.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PinAction {
    UNLOCK_PANIC,
    OPEN_SETTINGS
}

enum class UnlockResult {
    SUCCESS_NORMAL,
    SUCCESS_DURESS,
    INVALID_PIN
}

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = SecurityPreferences(application)
    private val packageManager: PackageManager = application.packageManager

    // Lista de todos os apps instalados
    var allApps = mutableStateListOf<AppInfo>()
        private set

    // Estado da Tela de Bloqueio Camuflada
    var isDeviceLocked by mutableStateOf(prefs.isLockscreenEnabled)

    // Estado do Modo Pânico
    var isPanicMode by mutableStateOf(prefs.isPanicMode)
        private set

    // Senhas
    var masterPin by mutableStateOf(prefs.masterPin)
    var duressPin by mutableStateOf(prefs.duressPin)
    var isLockscreenEnabled by mutableStateOf(prefs.isLockscreenEnabled)

    // Busca de aplicativos
    var searchQuery by mutableStateOf("")

    // Controle de Modais
    var isSettingsOpen by mutableStateOf(false)
    var isPinDialogOpen by mutableStateOf(false)
    var currentPinAction by mutableStateOf(PinAction.UNLOCK_PANIC)

    private val bankKeywords = listOf(
        "nu", "nubank", "itau", "bradesco", "santander", "inter", 
        "c6", "mercadopago", "caixa", "bb", "bancodobrasil", "picpay",
        "pagseguro", "pagbank", "neon", "btg", "binance", "crypto",
        "bybit", "revolut", "wise", "xp", "rico", "clear"
    )

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val resolveInfoList = packageManager.queryIntentActivities(intent, 0)
                val sensitivePackages = prefs.getSensitivePackages()
                val currentPackage = getApplication<Application>().packageName

                resolveInfoList
                    .filter { it.activityInfo.packageName != currentPackage }
                    .map { resolveInfo ->
                        val pkg = resolveInfo.activityInfo.packageName
                        val label = resolveInfo.loadLabel(packageManager).toString()
                        val icon = resolveInfo.loadIcon(packageManager)
                        val isSensitive = sensitivePackages.contains(pkg) || 
                            (sensitivePackages.isEmpty() && isAutoDetectedBank(pkg, label))

                        AppInfo(
                            packageName = pkg,
                            label = label,
                            icon = icon,
                            isSensitive = isSensitive
                        )
                    }
                    .sortedBy { it.label.lowercase() }
            }

            allApps.clear()
            allApps.addAll(apps)

            if (prefs.getSensitivePackages().isEmpty()) {
                val detected = apps.filter { it.isSensitive }.map { it.packageName }.toSet()
                if (detected.isNotEmpty()) {
                    prefs.setSensitivePackages(detected)
                }
            }
        }
    }

    private fun isAutoDetectedBank(packageName: String, label: String): Boolean {
        val lowerPkg = packageName.lowercase()
        val lowerLabel = label.lowercase()
        return bankKeywords.any { keyword ->
            lowerPkg.contains(keyword) || lowerLabel.contains(keyword)
        }
    }

    fun getVisibleApps(): List<AppInfo> {
        return allApps
            .filter { app ->
                if (isPanicMode && app.isSensitive) false else true
            }
            .filter { app ->
                if (searchQuery.isBlank()) true else app.label.contains(searchQuery, ignoreCase = true)
            }
    }

    /**
     * Validação na Tela de Bloqueio:
     * - Se for Master PIN (1212): Desbloqueia no Modo Normal (todos apps visíveis)
     * - Se for Duress PIN (2121): Desbloqueia com SUCESSO no Modo Pânico (bancos ocultos)
     * O assaltante não percebe nenhuma mensagem de erro!
     */
    fun unlockWithPin(pin: String): UnlockResult {
        return when (pin) {
            prefs.masterPin -> {
                isPanicMode = false
                prefs.isPanicMode = false
                isDeviceLocked = false
                UnlockResult.SUCCESS_NORMAL
            }
            prefs.duressPin -> {
                isPanicMode = true
                prefs.isPanicMode = true
                isDeviceLocked = false
                UnlockResult.SUCCESS_DURESS
            }
            else -> {
                UnlockResult.INVALID_PIN
            }
        }
    }

    fun lockDevice() {
        if (isLockscreenEnabled) {
            isDeviceLocked = true
            searchQuery = ""
        }
    }

    fun triggerPanicMode(context: Context) {
        isPanicMode = true
        prefs.isPanicMode = true
        searchQuery = ""
        isSettingsOpen = false
    }

    fun verifyPinAndExecute(pin: String, context: Context): Boolean {
        if (pin == prefs.masterPin) {
            when (currentPinAction) {
                PinAction.UNLOCK_PANIC -> {
                    isPanicMode = false
                    prefs.isPanicMode = false
                    Toast.makeText(context, "Modo Normal Restaurado.", Toast.LENGTH_SHORT).show()
                }
                PinAction.OPEN_SETTINGS -> {
                    isSettingsOpen = true
                }
            }
            isPinDialogOpen = false
            return true
        } else {
            Toast.makeText(context, "PIN incorreto!", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    fun promptUnlockPanic() {
        currentPinAction = PinAction.UNLOCK_PANIC
        isPinDialogOpen = true
    }

    fun promptOpenSettings() {
        currentPinAction = PinAction.OPEN_SETTINGS
        isPinDialogOpen = true
    }

    fun toggleAppSensitivity(app: AppInfo) {
        val index = allApps.indexOfFirst { it.packageName == app.packageName }
        if (index != -1) {
            val updated = app.copy(isSensitive = !app.isSensitive)
            allApps[index] = updated
            prefs.toggleAppSensitive(app.packageName)
        }
    }

    fun updatePins(newMaster: String, newDuress: String, context: Context) {
        if (newMaster.length >= 4 && newDuress.length >= 4 && newMaster != newDuress) {
            prefs.masterPin = newMaster
            prefs.duressPin = newDuress
            masterPin = newMaster
            duressPin = newDuress
            Toast.makeText(context, "Senhas salvas com sucesso!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "As senhas devem ter ao menos 4 dígitos e serem diferentes!", Toast.LENGTH_LONG).show()
        }
    }

    fun setLockscreenActive(enabled: Boolean) {
        isLockscreenEnabled = enabled
        prefs.isLockscreenEnabled = enabled
        if (!enabled) isDeviceLocked = false
    }

    fun launchApp(context: Context, packageName: String) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            } else {
                Toast.makeText(context, "Não foi possível abrir o app", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao abrir app: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
