#!/usr/bin/env python3
"""Validate Android string resources without requiring an Android build.

The default ``values`` directory is the source of truth. Locale resources may be
partial, but they must not introduce a resource that has no default fallback.
For strings and plurals, format arguments must also remain compatible with the
source resource so translated text cannot fail at runtime.
"""

from __future__ import annotations

import argparse
import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

# Android uses java.util.Formatter syntax. This intentionally ignores %%.
FORMAT = re.compile(r"%(?:\d+\$)?[-#+ 0,(<]*\d*(?:\.\d+)?(?:[tT])?[a-zA-Z]")
RESOURCE_TAGS = {"string", "plurals", "string-array"}


def text(element: ET.Element) -> str:
    return "".join(element.itertext())


def placeholders(value: str) -> Counter[str]:
    return Counter(match.group(0)[-1].lower() for match in FORMAT.finditer(value.replace("%%", "")))


def load(directory: Path) -> tuple[dict[str, ET.Element], list[str]]:
    resources: dict[str, ET.Element] = {}
    errors: list[str] = []
    for file in sorted(directory.glob("*.xml")):
        try:
            root = ET.parse(file).getroot()
        except ET.ParseError as error:
            errors.append(f"{file}: invalid XML: {error}")
            continue
        for element in root:
            name = element.get("name")
            if element.tag not in RESOURCE_TAGS or name is None:
                continue
            if name in resources:
                errors.append(f"{file}: duplicate resource name '{name}'")
            resources[name] = element
    return resources, errors


def values_for(element: ET.Element) -> dict[str, str]:
    if element.tag == "plurals":
        return {item.get("quantity", ""): text(item) for item in element}
    return {"value": text(element)}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--res-dir", type=Path, default=Path("app/src/main/res"))
    args = parser.parse_args()

    default_dir = args.res_dir / "values"
    source, errors = load(default_dir)
    if not source:
        errors.append(f"{default_dir}: no default string resources found")

    for directory in sorted(args.res_dir.glob("values-*")):
        localized, load_errors = load(directory)
        errors.extend(load_errors)
        for name, translated in localized.items():
            original = source.get(name)
            if original is None:
                errors.append(f"{directory}: '{name}' has no default fallback")
                continue
            if original.tag != translated.tag:
                errors.append(f"{directory}: '{name}' is {translated.tag}, expected {original.tag}")
                continue
            if translated.tag == "plurals" and "other" not in values_for(translated):
                errors.append(f"{directory}: plural '{name}' is missing the required 'other' quantity")

            original_values = values_for(original)
            for quantity, translated_text in values_for(translated).items():
                # Plural categories differ between languages, and a locale may
                # deliberately omit a number from a phrase. A translated
                # formatter is unsafe only when it introduces a type that the
                # source resource does not accept.
                expected = " ".join(original_values.values())
                if translated.get("formatted") == "false":
                    continue
                unsupported = placeholders(translated_text) - placeholders(expected)
                if unsupported:
                    errors.append(
                        f"{directory}: unsupported format argument in '{name}' ({quantity})"
                    )

    if errors:
        print("Translation resource validation failed:", file=sys.stderr)
        print("\n".join(f"- {error}" for error in errors), file=sys.stderr)
        return 1
    print("Translation resource validation passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
