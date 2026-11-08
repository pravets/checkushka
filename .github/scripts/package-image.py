#!/usr/bin/env python3
"""Сборка per-OS архива checkushka со встроенным JRE через jpackage.

Аналог `.github/scripts/build-jpackage.py` из BSL Language Server.
"""

import argparse
import os
import re
import shutil
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

WIN_CONSOLE_WRAPPER = r"""@echo off
rem Псевдоним для checkushka.exe, оставлен для уже написанных скриптов и ярлыков.
rem Кодировку вывода выбирает сама JVM по текущей кодовой странице консоли.
"%~dp0checkushka.exe" %*
exit /b %ERRORLEVEL%
"""

MAIN_CLASS = "ru.pravets.checkushka.cli.Main"
APP_NAME = "checkushka"


def find_exec_jar(source: Path) -> Path:
    if source.is_dir():
        candidates = sorted(source.glob("*-exec.jar"))
        if len(candidates) != 1:
            sys.exit(f"Expected exactly one *-exec.jar in {source}, found: {len(candidates)}")
        return candidates[0]
    if not source.is_file():
        sys.exit(f"exec-jar not found: {source}")
    return source


def version_from_jar(jar: Path) -> str:
    match = re.search(rf"{APP_NAME}-(.+)-exec\.jar$", jar.name)
    if not match:
        sys.exit(f"Cannot parse version from {jar.name}; pass --version")
    return match.group(1)


def jpackage_command() -> str:
    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        candidate = Path(java_home) / "bin" / ("jpackage.exe" if os.name == "nt" else "jpackage")
        if candidate.exists():
            return str(candidate)
    return "jpackage"


def build_image(jar: Path, version: str, work: Path) -> Path:
    input_dir = work / "input"
    input_dir.mkdir()
    shutil.copy2(jar, input_dir / jar.name)

    dest = work / "image"
    dest.mkdir()
    # --app-version не передаём: jpackage на macOS требует, чтобы первое число было больше нуля,
    # а версия приложения всё равно берётся из манифеста jar (checkushka --version)
    command = [
        jpackage_command(),
        "--type", "app-image",
        "--name", APP_NAME,
        "--input", str(input_dir),
        "--main-jar", jar.name,
        "--main-class", MAIN_CLASS,
        "--dest", str(dest),
        "--java-options", "-Dfile.encoding=UTF-8",
    ]
    if sys.platform == "win32":
        # Без --win-console лаунчер собирается как GUI-приложение: интерактивный cmd
        # не подключает его к консоли и не ждёт завершения, поэтому команда молчит,
        # а код возврата теряется. На Linux и macOS опция не существует.
        command.append("--win-console")
    print(f"jpackage: {jar.name}, version {version}")
    subprocess.run(command, check=True)

    entries = list(dest.iterdir())
    if len(entries) != 1:
        sys.exit(f"jpackage produced unexpected entries: {[entry.name for entry in entries]}")
    return entries[0]


def archive(image: Path, prefix: str, out: Path) -> Path:
    if prefix == "win":
        wrapper = image / "checkushka-console.bat"
        wrapper.write_text(WIN_CONSOLE_WRAPPER.replace("\n", "\r\n"), encoding="utf-8")

    out.mkdir(parents=True, exist_ok=True)
    archive_path = out / f"{APP_NAME}_{prefix}.zip"
    with zipfile.ZipFile(archive_path, "w", zipfile.ZIP_DEFLATED) as archive_file:
        for path in sorted(image.rglob("*")):
            archive_file.write(path, path.relative_to(image.parent))
    return archive_path


def main() -> None:
    # Windows-консоль по умолчанию cp1252: без этого любые не-ASCII сообщения валят скрипт
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    if hasattr(sys.stderr, "reconfigure"):
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")

    parser = argparse.ArgumentParser(description="Собрать per-OS архив checkushka со встроенным JRE")
    parser.add_argument("--jar", required=True, help="exec-jar или каталог с ним")
    parser.add_argument("--prefix", required=True, choices=["win", "nix", "mac"],
                        help="платформа в имени архива")
    parser.add_argument("--out", default=".", help="куда положить архив")
    parser.add_argument("--version", help="версия приложения; по умолчанию из имени jar")
    args = parser.parse_args()

    jar = find_exec_jar(Path(args.jar))
    version = args.version or version_from_jar(jar)
    out = Path(args.out)

    work = Path(tempfile.mkdtemp(prefix="checkushka-image-"))
    try:
        image = build_image(jar, version, work)
        archive_path = archive(image, args.prefix, out)
        size_mb = archive_path.stat().st_size / 1024 / 1024
        print(f"created {archive_path} ({size_mb:.1f} MB)")
    finally:
        shutil.rmtree(work, ignore_errors=True)


if __name__ == "__main__":
    main()
