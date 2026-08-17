import glob
import re

files = glob.glob('app/src/main/java/**/*.kt', recursive=True)

for file in files:
    with open(file, 'r') as f:
        content = f.read()
    
    bad_pattern = """val artworkBitmap: androidx.compose.ui.graphics.ImageBitmap? = null
                }
            }"""
    if bad_pattern in content:
        content = content.replace(bad_pattern, "val artworkBitmap: androidx.compose.ui.graphics.ImageBitmap? = null")
        with open(file, 'w') as f:
            f.write(content)
            print(f"Fixed {file}")
