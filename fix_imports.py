import os

def add_import(file_path, new_imports):
    with open(file_path, 'r') as f:
        content = f.read()
    
    # Check if already imported
    for imp in new_imports:
        if imp not in content:
            # Find last import
            last_import_idx = content.rfind('import ')
            if last_import_idx != -1:
                end_of_line = content.find('\n', last_import_idx)
                content = content[:end_of_line+1] + f"import {imp}\n" + content[end_of_line+1:]
            
    with open(file_path, 'w') as f:
        f.write(content)

files_mic = [
    'app/src/main/java/com/example/ui/screens/AlbumsScreen.kt',
    'app/src/main/java/com/example/ui/screens/ArtistsScreen.kt',
    'app/src/main/java/com/example/ui/screens/FoldersScreen.kt',
    'app/src/main/java/com/example/ui/screens/HomeScreen.kt',
    'app/src/main/java/com/example/ui/screens/LibraryScreen.kt'
]

for file in files_mic:
    add_import(file, ['androidx.compose.material.icons.filled.Mic'])

add_import('app/src/main/java/com/example/ui/screens/FoldersScreen.kt', ['androidx.compose.ui.text.style.TextOverflow'])

