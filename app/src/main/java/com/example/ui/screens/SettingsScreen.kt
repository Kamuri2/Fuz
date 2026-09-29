package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppSettings
import com.example.model.AppTheme
import com.example.ui.Translations
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
    val primaryColor = MaterialTheme.colorScheme.primary
    val lang = settings.appLanguage

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Bar: Back, Title, Settings Icon
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
                    text = Translations.get(lang, "settings"),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0x1AFFFFFF))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 1. Audio Engine Hero Card: Jetpack Media3 (ExoPlayer)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Brush.linearGradient(listOf(Color(0x40FFFFFF), Color(0x10FFFFFF))), RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x1A1E1E24))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(primaryColor, primaryColor.copy(alpha = 0.5f)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = Translations.get(lang, "audio_engine_title"),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(primaryColor.copy(alpha = 0.2f))
                                    .border(1.dp, primaryColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = Translations.get(lang, "audio_engine_badge"),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = primaryColor
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = Translations.get(lang, "audio_engine_subtitle"),
                            fontSize = 12.sp,
                            color = GlassTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Format pills
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val formats = listOf("FLAC 24-bit", "MP3", "AAC", "OGG", "Opus", "WAV", "ALAC", "MediaCodec")
                    formats.forEach { fmt ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x14FFFFFF))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = fmt,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0x1FFFFFFF))
                )
                Spacer(modifier = Modifier.height(18.dp))

                // Crossfade Duration (Default 100 ms / 0.1 s)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Crossfade",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = if (settings.crossfadeDuration <= 0.15f) "100 ms" else String.format(java.util.Locale.US, "%.1f s", settings.crossfadeDuration),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Slider(
                    value = settings.crossfadeDuration,
                    onValueChange = {
                        val rounded = Math.round(it * 10f) / 10f
                        onUpdateSettings(
                            settings.copy(
                                crossfadeDuration = rounded,
                                fadeOutDuration = rounded,
                                fadeInDuration = rounded
                            )
                        )
                    },
                    valueRange = 0f..5f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color(0x33FFFFFF)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0s", fontSize = 11.sp, color = GlassTextSecondary)
                    Text("5s", fontSize = 11.sp, color = GlassTextSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Appearance & Themes Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x1A1E1E24))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Translations.get(lang, "appearance_themes"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Dark Mode Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (settings.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = GlassTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = Translations.get(lang, "dark_mode"),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = if (settings.isDarkMode) Translations.get(lang, "dark_mode_desc_on") else Translations.get(lang, "dark_mode_desc_off"),
                                fontSize = 12.sp,
                                color = GlassTextMuted
                            )
                        }
                    }

                    Switch(
                        checked = settings.isDarkMode,
                        onCheckedChange = { onUpdateSettings(settings.copy(isDarkMode = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = primaryColor,
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            uncheckedTrackColor = Color(0x33FFFFFF)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // App Language Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = GlassTextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = Translations.get(lang, "app_language"),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    var expandedLang by remember { mutableStateOf(false) }
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x26FFFFFF))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                                .clickable { expandedLang = true }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = settings.appLanguage, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = GlassTextSecondary, modifier = Modifier.size(18.dp))
                        }

                        DropdownMenu(
                            expanded = expandedLang,
                            onDismissRequest = { expandedLang = false },
                            modifier = Modifier.background(Color(0xFF22222A))
                        ) {
                            listOf("English", "Spanish", "French", "German", "Italian", "Portuguese", "Japanese").forEach { languageOption ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = languageOption,
                                            color = if (settings.appLanguage == languageOption) primaryColor else Color.White,
                                            fontWeight = if (settings.appLanguage == languageOption) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        onUpdateSettings(settings.copy(appLanguage = languageOption))
                                        expandedLang = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Global Theme Selection: Refined Swatches Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Translations.get(lang, "global_theme"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "${AppTheme.values().size} ${Translations.get(lang, "styles_count")}",
                        fontSize = 12.sp,
                        color = GlassTextMuted
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Grid of Theme Swatch Cards
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppTheme.values().forEach { theme ->
                        val isSelected = settings.selectedTheme == theme
                        val animatedBorderColor by animateColorAsState(
                            targetValue = if (isSelected) theme.primaryColor else Color(0x1FFFFFFF),
                            animationSpec = tween(250)
                        )
                        val animatedBgColor by animateColorAsState(
                            targetValue = if (isSelected) theme.primaryColor.copy(alpha = 0.16f) else Color(0x12FFFFFF),
                            animationSpec = tween(250)
                        )

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(animatedBgColor)
                                .border(1.dp, animatedBorderColor, RoundedCornerShape(12.dp))
                                .clickable { onUpdateSettings(settings.copy(selectedTheme = theme)) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.sweepGradient(
                                            listOf(theme.primaryColor, theme.bgGradientStart, theme.primaryColor)
                                        )
                                    )
                                    .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = theme.displayName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else GlassTextPrimary
                            )

                            if (isSelected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = theme.primaryColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Typography Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x1A1E1E24))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FontDownload,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Translations.get(lang, "typography_title"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = Translations.get(lang, "current_font"), fontSize = 12.sp, color = GlassTextMuted)
                        Text(
                            text = settings.fontFamilyName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (settings.fontFamilyName != "System Font (Default)") {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x33FF4D4D))
                                .border(1.dp, Color(0x66FF4D4D), RoundedCornerShape(10.dp))
                                .clickable { onUpdateSettings(settings.copy(fontFamilyName = "System Font (Default)")) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(Translations.get(lang, "reset"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF6B6B))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Fast Font Presets
                Text(text = Translations.get(lang, "popular_fonts"), fontSize = 12.sp, color = GlassTextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf("System Font (Default)", "Outfit", "Poppins", "Montserrat", "Inter", "Playfair Display", "Roboto", "Lato", "Pacifico")
                    presets.forEach { fontName ->
                        val isCur = settings.fontFamilyName == fontName
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCur) primaryColor.copy(alpha = 0.25f) else Color(0x14FFFFFF))
                                .border(1.dp, if (isCur) primaryColor else Color(0x26FFFFFF), RoundedCornerShape(10.dp))
                                .clickable { onUpdateSettings(settings.copy(fontFamilyName = fontName)) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (fontName.contains("(")) Translations.get(lang, "system_default") else fontName,
                                fontSize = 12.sp,
                                fontWeight = if (isCur) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCur) primaryColor else Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom font input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = fontInput,
                        onValueChange = { fontInput = it },
                        placeholder = { Text(Translations.get(lang, "font_placeholder"), color = GlassTextMuted, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0x1AFFFFFF),
                            unfocusedContainerColor = Color(0x14FFFFFF),
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color(0x26FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (fontInput.isNotBlank()) primaryColor else Color(0x33FFFFFF))
                            .clickable(enabled = fontInput.isNotBlank()) {
                                onUpdateSettings(settings.copy(fontFamilyName = fontInput.trim()))
                                fontInput = ""
                            }
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Translations.get(lang, "apply"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (fontInput.isNotBlank()) Color.Black else Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Lyrics Settings Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x1A1E1E24))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Translations.get(lang, "lyrics_settings"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Font Size Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(Translations.get(lang, "lyrics_font_size"), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(primaryColor.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${settings.lyricsFontSizePercent}%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = settings.lyricsFontSizePercent.toFloat(),
                    onValueChange = { onUpdateSettings(settings.copy(lyricsFontSizePercent = it.toInt())) },
                    valueRange = 80f..150f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = primaryColor,
                        inactiveTrackColor = Color(0x33FFFFFF)
                    )
                )

                Text(
                    text = Translations.get(lang, "lyrics_size_desc"),
                    fontSize = 12.sp,
                    color = GlassTextMuted
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Translation toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Translate, contentDescription = null, tint = GlassTextSecondary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = Translations.get(lang, "lyrics_translation"),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = Translations.get(lang, "lyrics_translation_desc"),
                                fontSize = 12.sp,
                                color = GlassTextMuted
                            )
                        }
                    }

                    Switch(
                        checked = settings.isLyricsTranslationEnabled,
                        onCheckedChange = { onUpdateSettings(settings.copy(isLyricsTranslationEnabled = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = primaryColor,
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            uncheckedTrackColor = Color(0x33FFFFFF)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Music Library Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x26FFFFFF), RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x1A1E1E24))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = Translations.get(lang, "music_library"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = Translations.get(lang, "rescan_desc"),
                    fontSize = 12.sp,
                    color = GlassTextMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(primaryColor, primaryColor.copy(alpha = 0.75f))
                            )
                        )
                        .clickable { onRescanAudio() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Translations.get(lang, "rescan_now"),
                            color = Color.Black,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}
