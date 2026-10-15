package com.safelauncher.app

import android.os.Bundle
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
                            // Desbloqueado com sucesso (seja modo normal ou modo pânico)
                        }
                    )
                } else {
                    HomeScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadApps()
    }
}
