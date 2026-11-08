#!/usr/bin/env python3
"""Дымовая проверка образа из релиза: лаунчер запускается, отвечает и отдаёт код возврата.

Проверка идёт через пайпы, поэтому она не заменяет ручной запуск в консоли, но ловит
регрессии упаковки: пропавший лаунчер, пустой вывод, потерянный код возврата и — отдельно
на Windows — возврат лаунчера к GUI-подсистеме, из-за которой интерактивный cmd не
подключает процесс к консоли и команда выглядит как «ничего не произошло».
"""

import argparse
import os
import re
import stat
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

LAUNCHERS = {
    "win": "checkushka/checkushka.exe",
    "nix": "checkushka/bin/checkushka",
    "mac": "checkushka.app/Contents/MacOS/checkushka",
}

PE_GUI = 2
PE_CONSOLE = 3

UNSAFE_FLAG = "--sun-misc-unsafe-memory-access=allow"

# Функциональные проверки идут с принудительным UTF-8: иначе в консоли с cp866
# вывод (штатно) будет в cp866, и сравнение строк зависело бы от окружения
UTF8_ENV = {"CHECKUSHKA_STDOUT_ENCODING": "UTF-8"}


def console_code_pages() -> list[str]:
    """Кодировки, которыми потребитель может декодировать вывод: UTF-8 и кодовая страница."""
    charsets = ["utf-8"]
    if sys.platform != "win32":
        return charsets
    try:
        result = subprocess.run(["cmd", "/c", "chcp"], capture_output=True, timeout=30, check=False)
    except OSError:
        return charsets
    numbers = re.findall(rb"(\d+)", result.stdout)
    if not numbers:
        return charsets
    code_page = int(numbers[-1])
    if code_page != 65001:
        charsets.append(f"cp{code_page}")
    return charsets


def launcher_config(root: Path) -> Path:
    """Ищет cfg лаунчера, в котором jpackage хранит java-options образа."""
    for candidate in sorted(root.rglob("*.cfg")):
        text = candidate.read_text(encoding="utf-8", errors="replace")
        if "ru.pravets.checkushka.cli.Main" in text:
            return candidate
    raise SystemExit("FAIL launcher config (*.cfg) with the main class not found in the image")


def extract(archive: Path, dest: Path) -> None:
    with zipfile.ZipFile(archive) as package:
        package.extractall(dest)
    # zipfile не хранит права POSIX: без этого лаунчер и утилиты внутри runtime не запустятся
    for path in dest.rglob("*"):
        posix = path.as_posix()
        if path.is_file() and ("/bin/" in posix or "/MacOS/" in posix):
            path.chmod(path.stat().st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)


def pe_subsystem(path: Path) -> int | None:
    data = path.read_bytes()
    if data[:2] != b"MZ":
        return None
    pe = int.from_bytes(data[0x3C:0x40], "little")
    if data[pe:pe + 4] != b"PE\0\0":
        return None
    magic = int.from_bytes(data[pe + 24:pe + 26], "little")
    if magic not in (0x10B, 0x20B):
        return None
    return int.from_bytes(data[pe + 24 + 0x44:pe + 24 + 0x46], "little")


def run(launcher: Path, args: list[str], cwd: Path, env: dict[str, str] | None = None) -> subprocess.CompletedProcess:
    return subprocess.run(
        [str(launcher), *args],
        capture_output=True,
        cwd=cwd,
        env={**os.environ, **(env or {})},
        timeout=300,
        check=False,
    )


def decoded(data: bytes) -> str:
    return data.decode("utf-8", errors="replace")


def expect(
    launcher: Path,
    args: list[str],
    cwd: Path,
    exit_code: int,
    fragment: str,
    stream: str = "stdout",
    env: dict[str, str] | None = None,
) -> str:
    result = run(launcher, args, cwd, env)
    text = decoded(result.stdout if stream == "stdout" else result.stderr)
    label = " ".join(args) or "<без аргументов>"
    if result.returncode != exit_code:
        raise SystemExit(
            f"FAIL {label}: exit code {result.returncode}, expected {exit_code}\n"
            f"stdout: {decoded(result.stdout)[:400]}\nstderr: {decoded(result.stderr)[:400]}"
        )
    if fragment not in text:
        raise SystemExit(
            f"FAIL {label}: {fragment!r} not found in {stream}\n"
            f"{stream}: {text[:400]}"
        )
    print(f"ok   {label} -> exit {exit_code}, {stream} contains {fragment!r}")
    return text


def expect_decodable(launcher: Path, root: Path, fragment: str) -> None:
    """Автоматическая кодировка: в консольном окружении допустима кодовая страница."""
    result = run(launcher, ["--help"], root)
    for charset in console_code_pages():
        if fragment in result.stdout.decode(charset, errors="replace"):
            print(f"ok   automatic encoding decodes as {charset}")
            return
    raise SystemExit(
        f"FAIL --help output does not decode as any of {console_code_pages()}\n"
        f"bytes: {result.stdout[:120]!r}"
    )


def main() -> None:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    if hasattr(sys.stderr, "reconfigure"):
        sys.stderr.reconfigure(encoding="utf-8", errors="replace")

    parser = argparse.ArgumentParser(description="Дымовая проверка собранного образа")
    parser.add_argument("--archive", required=True, help="архив checkushka_<os>.zip")
    parser.add_argument("--prefix", required=True, choices=sorted(LAUNCHERS),
                        help="платформа: win, nix или mac")
    args = parser.parse_args()

    archive = Path(args.archive).resolve()
    if not archive.is_file():
        raise SystemExit(f"archive not found: {archive}")

    with tempfile.TemporaryDirectory(prefix="checkushka-smoke-") as workspace:
        root = Path(workspace)
        extract(archive, root)
        launcher = root / LAUNCHERS[args.prefix]
        if not launcher.is_file():
            raise SystemExit(f"launcher not found in archive: {LAUNCHERS[args.prefix]}")
        print(f"launcher: {LAUNCHERS[args.prefix]}")

        if args.prefix == "win":
            subsystem = pe_subsystem(launcher)
            if subsystem != PE_CONSOLE:
                raise SystemExit(
                    f"FAIL PE subsystem is {subsystem}, expected {PE_CONSOLE} (WINDOWS_CUI). "
                    "Соберите образ с jpackage --win-console, иначе cmd не подключит процесс к консоли"
                )
            print("ok   PE subsystem = 3 (WINDOWS_CUI)")

        config = launcher_config(root)
        if UNSAFE_FLAG not in config.read_text(encoding="utf-8", errors="replace"):
            raise SystemExit(
                f"FAIL {config.name} has no {UNSAFE_FLAG}: XStream warnings about sun.misc.Unsafe "
                "would flood stderr on every run"
            )
        print(f"ok   {config.name} sets {UNSAFE_FLAG}")
        # Неизвестный JVM-флаг уронил бы запуск ниже, поэтому отдельная проверка версии
        # встроенного JRE не нужна: успешные запуски лаунчера и есть доказательство.

        expect(launcher, ["--version"], root, 0, "checkushka ", env=UTF8_ENV)
        expect(launcher, ["--help"], root, 0, "Проверка метаданных", env=UTF8_ENV)
        expect(launcher, ["list-checks"], root, 0, "MetadataOrder", env=UTF8_ENV)
        expect(launcher, ["check", "__no_such_directory__"], root, 2, "Ошибка",
               stream="stderr", env=UTF8_ENV)
        expect_decodable(launcher, root, "Проверка метаданных")

    print(f"smoke test passed: {archive.name}")


if __name__ == "__main__":
    main()
