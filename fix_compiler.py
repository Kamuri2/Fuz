import subprocess
try:
    subprocess.run(["gradle", ":app:assembleDebug"], check=True)
    print("Compiled successfully!")
except Exception as e:
    print(e)
