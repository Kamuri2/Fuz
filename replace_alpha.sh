#!/bin/bash
FILES=$(find app/src/main/java/com/example/ui -name "*.kt" -type f)

for file in $FILES; do
    sed -i 's/Color(0x35FF5C00)/MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)/g' "$file"
    sed -i 's/Color(0x33FF5C00)/MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)/g' "$file"
    sed -i 's/Color(0x28FF5C00)/MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)/g' "$file"
done
