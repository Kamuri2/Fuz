with open('app/build.gradle.kts', 'r') as f:
    lines = f.readlines()

out = []
for l in lines:
    if 'jaudiotagger' not in l:
        out.append(l)

with open('app/build.gradle.kts', 'w') as f:
    f.writelines(out)
