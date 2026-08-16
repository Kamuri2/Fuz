with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "r") as f:
    content = f.read()

target = """                }
            }
        } else {"""
replacement = """                }
            }
            }
        } else {"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/screens/PlayerScreen.kt", "w") as f:
    f.write(content)
print("Added brace back")
