package com.example.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppSettings
import com.example.model.AppTheme
import com.example.ui.theme.GlassTextMuted
import com.example.ui.theme.GlassTextPrimary
import com.example.ui.theme.GlassTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdateSettings: (AppSettings) -> Unit,
    onBack: () -> Unit,
    onRescanAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fontInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Bar: Back, Settings Title, Gear Icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_btn")) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = com.example.ui.Translations.get(settings.appLanguage, "settings"),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x1AFFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main Settings Card matching Screenshots #5, #6, #7
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Crossfade
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = com.example.ui.Translations.get(settings.appLanguage, "crossfade_duration"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Slider(
                        value = settings.crossfadeDuration,
                        onValueChange = { onUpdateSettings(settings.copy(crossfadeDuration = it)) },
                        valueRange = 0f..5f,
                        steps = 49,
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = String.format("%.1f s", settings.crossfadeDuration),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.width(48.dp)
                    )
                }


                // Section Title: Appearance & Themes
                Text(
                    text = com.example.ui.Translations.get(settings.appLanguage, "appearance_themes"),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 1. Dark Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = com.example.ui.Translations.get(settings.appLanguage, "dark_mode"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = GlassTextSecondary
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .clickable {
                                onUpdateSettings(settings.copy(isDarkMode = !settings.isDarkMode))
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (settings.isDarkMode) "Active" else "Inactive",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 2. App Language
                Text(
                    text = com.example.ui.Translations.get(settings.appLanguage, "app_language"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(14.dp))
                var expandedLang by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1AFFFFFF))
                        .clickable { expandedLang = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(text = settings.appLanguage, color = Color.White, fontSize = 15.sp)
                    androidx.compose.material3.DropdownMenu(
                        expanded = expandedLang,
                        onDismissRequest = { expandedLang = false },
                        modifier = Modifier.background(Color(0x1AFFFFFF))
                    ) {
                        listOf("English", "Spanish", "French", "German", "Italian", "Portuguese", "Japanese").forEach { lang ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(lang, color = if (settings.appLanguage == lang) Color.White else GlassTextMuted) },
                                onClick = {
                                    onUpdateSettings(settings.copy(appLanguage = lang))
                                    expandedLang = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))

                // 3. Global Theme Grid matching photos
                Text(
                    text = "Global Theme",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(14.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppTheme.values().forEach { theme ->
                        val isSelected = settings.selectedTheme == theme
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) Color.White else Color(0x1AFFFFFF))
                                .clickable { onUpdateSettings(settings.copy(selectedTheme = theme)) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = theme.displayName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else GlassTextPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // 4. Typography (Google Fonts)
                Text(
                    text = "Typography (Google Fonts)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = settings.fontFamilyName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF3A1A1A))
                            .clickable { onUpdateSettings(settings.copy(fontFamilyName = "System Font (Default)")) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Reset", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF4D4D))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = fontInput,
                        onValueChange = { fontInput = it },
                        placeholder = { Text("Or type another (e.g. Roboto M)", color = GlassTextMuted, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0x1AFFFFFF),
                            unfocusedContainerColor = Color(0x1AFFFFFF),
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .clickable {
                                if (fontInput.isNotBlank()) {
                                    onUpdateSettings(settings.copy(fontFamilyName = fontInput))
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        Text("Apply", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // 5. Lyrics Font Size
                Text(
                    text = "Lyrics Font Size",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Smaller", fontSize = 12.sp, color = GlassTextMuted)
                    Text("${settings.lyricsFontSizePercent}%", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Larger", fontSize = 12.sp, color = GlassTextMuted)
                }

                Slider(
                    value = settings.lyricsFontSizePercent.toFloat(),
                    onValueChange = { onUpdateSettings(settings.copy(lyricsFontSizePercent = it.toInt())) },
                    valueRange = 80f..150f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color(0xFF707070),
                        inactiveTrackColor = Color(0xFF333333)
                    )
                )

                Text(
                    text = "Adjust size so long lyrics fit on a single line without breaking.",
                    fontSize = 12.sp,
                    color = GlassTextMuted
                )

                Spacer(modifier = Modifier.height(28.dp))

                // 6. Lyrics Translation (Automatic)
                Text(
                    text = "Lyrics Translation (Automatic)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable lyrics translation",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "When enabled you can choose the target language. Works offline.",
                            fontSize = 12.sp,
                            color = GlassTextMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .clickable {
                                onUpdateSettings(settings.copy(isLyricsTranslationEnabled = !settings.isLyricsTranslationEnabled))
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (settings.isLyricsTranslationEnabled) "Active" else "Inactive",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Music Library Management Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Music Library",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))



                    Button(
                        onClick = onRescanAudio,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Re-Escanear", color = Color.White, fontSize = 12.sp)
                    }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}
