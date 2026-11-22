"""Retener tres releases propios y proteger los punteros current/previous."""
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys


def prune(root, keep=3):
    root = Path(root).resolve()
    releases = root / 'releases'
    if not releases.is_dir():
        return
    protected = set()
    for name in ('current', 'previous'):
        pointer = root / name
        if pointer.is_symlink():
            target = pointer.resolve()
            if target.parent != releases:
                raise ValueError('Puntero fuera del directorio de releases.')
            protected.add(target)
    candidates = sorted((p for p in releases.iterdir() if p.is_dir() and not p.is_symlink() and (p / '.dom2-release').is_file()),
                        key=lambda p: p.stat().st_mtime, reverse=True)
    protected.update(candidates[:keep])
    for directory in candidates:
        if directory in protected:
            continue
        resolved = directory.resolve()
        if resolved.parent != releases or not (directory / '.dom2-release').is_file():
            continue
        info = directory / 'release.json'
        image = json.loads(info.read_text())['image'] if info.is_file() else ''
        if not re.fullmatch(r'ocasion-dom2:[a-zA-Z0-9_.-]+', image):
            continue
        # Sin --force: Docker conserva imágenes en uso. No se eliminan volúmenes.
        result = subprocess.run(['docker', 'image', 'rm', image], capture_output=True)
        if result.returncode == 0:
            shutil.rmtree(resolved)


if __name__ == '__main__':
    prune(sys.argv[1])
