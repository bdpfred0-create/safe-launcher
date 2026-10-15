package com.safelauncher.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.safelauncher.app.data.SecurityPreferences

/**
 * Serviço de Proteção Ativa em Segundo Plano:
 * Monitora qualquer tentativa de abrir aplicativos sensíveis (bancos).
 * Se o Modo Pânico estiver ativo (acionado pela senha 2121),
 * qualquer banco que tente abrir (pela Play Store, recentes, notificações, etc.)
 * é sumariamente fechado em milissegundos e o celular volta à Tela Inicial.
 */
class SafeAccessibilityService : AccessibilityService() {

    private lateinit var prefs: SecurityPreferences

    override fun onCreate() {
        super.onCreate()
        prefs = SecurityPreferences(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val openedPackage = event.packageName?.toString() ?: return

            // Não interferir com o próprio Launcher ou sistema básico
            if (openedPackage == packageName || openedPackage == "com.android.systemui") {
                return
            }

            // Se o Modo Pânico estiver ativo
            if (prefs.isPanicMode) {
                val sensitivePackages = prefs.getSensitivePackages()

                // Se o pacote que tentou abrir for um banco marcado como sensível
                if (sensitivePackages.contains(openedPackage)) {
                    // Força o retorno imediato para a tela inicial
                    performGlobalAction(GLOBAL_ACTION_HOME)
                }
            }
        }
    }

    override fun onInterrupt() {}
}
