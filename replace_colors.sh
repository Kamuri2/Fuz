#!/bin/bash
FILES=$(find app/src/main/java/com/example/ui -name "*.kt" -type f)

for file in $FILES; do
    sed -i 's/Color(0xFFFF5C00)/MaterialTheme.colorScheme.primary/g' "$file"
    sed -i 's/OrangeAccent/MaterialTheme.colorScheme.primary/g' "$file"
    
    # Ensure MaterialTheme is imported
    if ! grep -q "import androidx.compose.material3.MaterialTheme" "$file"; then
        # Insert after the first import
        sed -i '0,/import/s//import androidx.compose.material3.MaterialTheme\nimport/' "$file"
    fi
done
