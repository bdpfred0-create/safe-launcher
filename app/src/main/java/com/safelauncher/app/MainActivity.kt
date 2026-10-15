package com.safelauncher.app

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.safelauncher.app.ui.HomeScreen
import com.safelauncher.app.ui.LockScreen
import com.safelauncher.app.ui.theme.SafeLauncherTheme
import com.safelauncher.app.viewmodel.LauncherViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SafeLauncherTheme {
                BackHandler {
                    if (viewModel.isDeviceLocked) {
                        // Não permite fechar a tela de bloqueio
                    } else if (viewModel.searchQuery.isNotEmpty()) {
                        viewModel.searchQuery = ""
                    } else if (viewModel.isSettingsOpen) {
                        viewModel.isSettingsOpen = false
                    } else if (viewModel.isPinDialogOpen) {
                        viewModel.isPinDialogOpen = false
                    }
                }

                if (viewModel.isDeviceLocked) {
                    LockScreen(
                        viewModel = viewModel,
                        onUnlocked = {
                            // Se não for o launcher padrão na primeira vez, sugere ao usuário
                            if (!isDefaultLauncher()) {
                                requestDefaultLauncher()
                            }
                        }
                    )
                } else {
                    HomeScreen(
                        viewModel = viewModel,
                        isDefaultLauncher = isDefaultLauncher(),
                        onRequestDefaultLauncher = { requestDefaultLauncher() },
                        onRequestAccessibility = { openAccessibilitySettings() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadApps()
    }

    /**
     * Verifica se o Safe Launcher já é a tela inicial padrão do aparelho
     */
    fun isDefaultLauncher(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        } else {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            resolveInfo?.activityInfo?.packageName == packageName
        }
    }

    /**
     * Dispara a caixa nativa do Android perguntando: "Definir como aplicativo de início?"
     */
    fun requestDefaultLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME) && !roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                startActivity(intent)
                return
            }
        }
        val intent = Intent(Settings.ACTION_HOME_SETTINGS)
        startActivity(intent)
    }

    fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }
}
