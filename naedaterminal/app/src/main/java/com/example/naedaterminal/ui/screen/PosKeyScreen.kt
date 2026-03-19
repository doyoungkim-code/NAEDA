package com.example.naedaterminal.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naedaterminal.ui.theme.KronaOneFontFamily
import com.example.naedaterminal.ui.theme.NaedaFontFamily

private val BgColor = Color(0xFFFCFFFF)
private val TextPrimary = Color(0xFF0D3B35)

@Composable
fun PosKeyScreen(onConnected: () -> Unit) {
    var posKey by remember { mutableStateOf("") }
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(220.dp))

            Text(
                text = "NAEDA",
                fontSize = 64.sp,
                fontWeight = FontWeight.Normal,
                color = primary,
                letterSpacing = 6.sp,
                fontFamily = KronaOneFontFamily
            )

            Spacer(Modifier.weight(0.2f))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "POS Key",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = NaedaFontFamily
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = posKey,
                    onValueChange = { posKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "Enter your POS Key",
                            color = TextPrimary.copy(alpha = 0.35f),
                            fontFamily = NaedaFontFamily
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = primary.copy(alpha = 0.6f)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primary,
                        unfocusedBorderColor = primary.copy(alpha = 0.25f),
                        focusedContainerColor = primary.copy(alpha = 0.05f),
                        unfocusedContainerColor = primary.copy(alpha = 0.04f)
                    )
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onConnected,
                    enabled = posKey.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primary,
                        disabledContainerColor = primary.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "연결하기",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = NaedaFontFamily
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            NaedaFooter(modifier = Modifier.padding(bottom = 36.dp))
        }
    }
}

@Composable
fun NaedaFooter(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "내다 ",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = NaedaFontFamily
            )
            Text(
                text = "(NAEDA)",
                color = primary,
                fontSize = 15.sp,
                fontFamily = KronaOneFontFamily
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "NADA PAY",
                color = primary,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                fontFamily = KronaOneFontFamily
            )
            Text(" • ", color = TextPrimary.copy(alpha = 0.35f), fontSize = 11.sp)
            Text(
                text = "SECURE CORE",
                color = TextPrimary.copy(alpha = 0.4f),
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                fontFamily = KronaOneFontFamily
            )
        }
    }
}