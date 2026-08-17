import re

with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'r') as f:
    content = f.read()

old_modal = """
        if (showAddToPlaylistModal) {
            ModalBottomSheet(
                onDismissRequest = { showAddToPlaylistModal = false },
                containerColor = Color(0x1AFFFFFF)
            ) {
"""

new_modal = """
        if (showAddToPlaylistModal) {
            ModalBottomSheet(
                onDismissRequest = { showAddToPlaylistModal = false },
                containerColor = Color(0xFF1E1E1E) // Solid dark background for readability
            ) {
"""
content = content.replace(old_modal, new_modal)

old_card = """
                            colors = CardDefaults.cardColors(containerColor = Color(0x1AFFFFFF))
"""

new_card = """
                            colors = CardDefaults.cardColors(containerColor = Color(0x2AFFFFFF))
"""
content = content.replace(old_card, new_card)


with open('app/src/main/java/com/example/ui/screens/PlayerScreen.kt', 'w') as f:
    f.write(content)
