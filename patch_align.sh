sed -i 's/modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)/modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp)/g' app/src/main/java/com/example/ui/screens/PlayerScreen.kt
sed -i 's/color = Color(0xFFFFB088)/color = androidx.compose.material3.MaterialTheme.colorScheme.primary/g' app/src/main/java/com/example/ui/screens/PlayerScreen.kt
