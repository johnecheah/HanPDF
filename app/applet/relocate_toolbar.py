import sys
import os

path = "/app/applet/app/src/main/java/com/example/ui/screens/PdfAppScreens.kt"
if not os.path.exists(path):
    print("Error: file does not exist!")
    sys.exit(1)

with open(path, "r") as f:
    lines = f.readlines()

# Extract the block
start_idx = 4187
end_idx = 4820
extracted_block = lines[start_idx:end_idx]

# Modify the Surface definition inside extracted_block to explicitly specify the color:
for idx, line in enumerate(extracted_block):
    if "Surface(" in line and "tonalElevation =" in extracted_block[idx+1]:
        extracted_block.insert(idx + 1, "                    color = MaterialTheme.colorScheme.surface,\n")
        break

# Clean up indentation of extracted block: shift left by 4 spaces
adjusted_block = []
for line in extracted_block:
    if line.startswith("    "):
        adjusted_block.append(line[4:])
    elif line.strip() == "":
        adjusted_block.append("\n")
    else:
        adjusted_block.append(line)

# Now, remove the block from lines
new_lines = lines[:start_idx] + lines[end_idx:]

# Find insertion index in new_lines
insert_idx = -1
for i, line in enumerate(new_lines):
    if "Premium deep charcoal canvas desk workspace" in line:
        if ") {" in new_lines[i+1]:
            insert_idx = i + 2
            break

if insert_idx == -1:
    print("Error: Insertion point not found!")
    sys.exit(1)

# Insert the adjusted block
final_lines = new_lines[:insert_idx] + adjusted_block + new_lines[insert_idx:]

with open(path, "w") as f:
    f.writelines(final_lines)

print("Relocation completed successfully!")
