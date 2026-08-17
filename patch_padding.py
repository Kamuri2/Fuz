import re

with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'r') as f:
    content = f.read()

old_code = """        } else {
            // ==================== PORTRAIT MODE ====================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            if (dragAmount > 35f) {
                                onBack()
                            }
                        }
                    }
                    .padding(vertical = 8.dp),"""

new_code = """        } else {
            // ==================== PORTRAIT MODE ====================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            if (dragAmount > 35f) {
                                onBack()
                            }
                        }
                    }
                    .padding(top = 8.dp, bottom = 80.dp),"""

content = content.replace(old_code, new_code)
with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'w') as f:
    f.write(content)
