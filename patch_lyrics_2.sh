sed -i 's/fontSize = if (isActive) 21.sp else 16.sp/fontSize = 18.sp/g' app/src/main/java/com/example/ui/screens/PlayerScreen.kt
sed -i 's/lineHeight = if (isActive) 28.sp else 22.sp/lineHeight = 24.sp/g' app/src/main/java/com/example/ui/screens/PlayerScreen.kt
sed -i 's/fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium/fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium/g' app/src/main/java/com/example/ui/screens/PlayerScreen.kt
sed -i 's/color = if (isActive) Color(0xFFFF9E66) else Color.White.copy(alpha = 0.4f)/color = animatedColor/g' app/src/main/java/com/example/ui/screens/PlayerScreen.kt
sed -i 's/\.clip(RoundedCornerShape(8.dp))/.graphicsLayer { scaleX = animatedScale; scaleY = animatedScale }\n                        .clip(RoundedCornerShape(8.dp))/g' app/src/main/java/com/example/ui/screens/PlayerScreen.kt
