#!/usr/bin/env python3
"""Validate that every declared mixin class is packaged in the built JAR."""

import json
import sys
import zipfile
from pathlib import Path

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")


def main() -> int:
    jars = sorted(
        path for path in Path("build/libs").glob("*.jar")
        if "sources" not in path.name
    )
    if not jars:
        print("ERROR: No JAR file found in build/libs/")
        return 1

    configs = sorted(Path("src/main/resources").rglob("*.mixins.json"))
    if not configs:
        print("WARNING: No mixin configuration files found")
        return 0

    jar_path = jars[0]
    errors = 0
    checked = 0
    print(f"Validating mixins in {jar_path.name}")

    with zipfile.ZipFile(jar_path) as archive:
        entries = set(archive.namelist())
        for config_path in configs:
            print(f"Checking {config_path.name}...")
            try:
                config = json.loads(config_path.read_text(encoding="utf-8"))
            except (OSError, json.JSONDecodeError) as exc:
                print(f"  FAIL invalid JSON: {exc}")
                errors += 1
                continue

            package = config.get("package")
            if not isinstance(package, str) or not package:
                print("  FAIL missing package declaration")
                errors += 1
                continue

            for array_name in ("mixins", "client", "server"):
                for mixin in config.get(array_name, []):
                    checked += 1
                    class_path = f"{package}.{mixin}".replace(".", "/") + ".class"
                    if class_path not in entries:
                        print(f"  FAIL {mixin}: expected {class_path}")
                        errors += 1

    print(f"Checked {checked} declared mixins; failures: {errors}")
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
