from pathlib import Path

root = Path("nextelis")

folders = [
    "docs",
    "backend",
    "android",
    "asterisk",
    "tests",
]

files = [
    "README.md",
    "docs/PROJECT.md",
    "docs/ARCHITECTURE.md",
    "docs/ROADMAP.md",
]

for folder in folders:
    (root / folder).mkdir(parents=True, exist_ok=True)

for file in files:
    path = root / file

    if not path.exists():
        path.touch()

print(f"NexTelis project created at: {root.resolve()}")