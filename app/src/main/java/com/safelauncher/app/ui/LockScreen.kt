package com.safelauncher.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safelauncher.app.viewmodel.LauncherViewModel
import com.safelauncher.app.viewmodel.UnlockResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LockScreen(
    viewModel: LauncherViewModel,
    onUnlocked: () -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var isError by remember { mutableStateOf(false) }

    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
            currentDate = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR")).format(now)
                .replaceFirstChar { it.uppercase() }
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Relógio e Data estilo Motorola / Android Limpo
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Bloqueado",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = currentTime,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White,
                    letterSpacing = (-2).sp
                )
                Text(
                    text = currentDate,
                    fontSize = 15.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            // Indicador de Dígitos do PIN
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    repeat(4) { index ->
                        val filled = index < pinInput.length
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isError -> Color(0xFFEF4444)
                                        filled -> Color.White
                                        else -> Color(0xFF334155)
                                    }
                                )
                        )
                    }
                }

                // Teclado Numérico Circular
                KeypadGrid(
                    onNumberClick = { digit ->
                        if (pinInput.length < 4) {
                            pinInput += digit
                            isError = false
                            if (pinInput.length == 4) {
                                coroutineScope.launch {
                                    val result = viewModel.unlockWithPin(pinInput)
                                    if (result == UnlockResult.INVALID_PIN) {
                                        isError = true
                                        delay(300)
                                        pinInput = ""
                                    } else {
                                        pinInput = ""
                                        onUnlocked()
                                    }
                                }
                            }
                        }
                    },
                    onBackspace = {
                        if (pinInput.isNotEmpty()) {
                            pinInput = pinInput.dropLast(1)
                            isError = false
                        }
                    }
                )
            }

            // Rodapé discreto
            Text(
                text = "Emergência",
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
fun KeypadGrid(
    onNumberClick: (String) -> Unit,
    onBackspace: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { key ->
                    when (key) {
                        "" -> {
                            Spacer(modifier = Modifier.size(72.dp))
                        }
                        "DEL" -> {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onBackspace),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Backspace,
                                    contentDescription = "Apagar",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        else -> {
                            KeypadButton(
                                label = key,
                                onClick = { onNumberClick(key) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color(0xFF1E293B))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}
