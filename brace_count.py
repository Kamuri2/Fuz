with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

lines = content.split('\n')
stack = []
for i, line in enumerate(lines):
    for j, char in enumerate(line):
        if char == '{':
            stack.append((i+1, line))
        elif char == '}':
            if stack:
                stack.pop()

print(f"Unclosed braces: {len(stack)}")
for item in stack:
    print(f"Line {item[0]}: {item[1].strip()}")
