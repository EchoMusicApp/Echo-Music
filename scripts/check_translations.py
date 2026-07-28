#!/usr/bin/env python3
"""Validate Android string resources without requiring an Android build.

The default ``values`` directory is the source of truth. Locale resources may be
partial, but they must not introduce a resource that has no default fallback.
For strings and plurals, formatter arguments must remain compatible with the
source resource so translated text cannot fail at runtime.
"""

from __future__ import annotations

import argparse
import re
import sys
from collections import Counter
from pathlib import Path

from defusedxml import ElementTree as ET

# Android uses java.util.Formatter syntax. The conversion is deliberately kept
# with its argument index: `%1$d` and `%2$d` are not interchangeable. `%%` is
# an escaped percent sign and does not consume an argument.
FORMAT = re.compile(
    r"%(?:(?P<index>[1-9]\d*)\$)?"
    r"(?P<flags>[-#+ 0,(<]*)"
    r"(?P<width>\d*)"
    r"(?:\.(?P<precision>\d+))?"
    r"(?P<date>[tT])?"
    r"(?P<conversion>[a-zA-Z%])"
)
RESOURCE_TAGS = {"string", "plurals", "string-array"}
PLURAL_QUANTITIES = {"zero", "one", "two", "few", "many", "other"}


def text(element: ET.Element) -> str:
    """Return all text content, including text in nested XML spans."""

    return "".join(element.itertext())


def placeholders(value: str) -> Counter[tuple[int, str]]:
    """Return formatter argument positions and types used by ``value``.

    Android permits both explicit positions (``%2$s``), implicit positions
    (``%s``), and the reuse flag (``%<s``). A counter preserves repeated
    arguments while making harmless reordering of placeholders valid.
    """

    result: Counter[tuple[int, str]] = Counter()
    next_implicit = 1
    previous_index: int | None = None
    for match in FORMAT.finditer(value):
        conversion = match.group("conversion")
        if conversion == "%":
            continue

        flags = match.group("flags")
        if match.group("index"):
            argument_index = int(match.group("index"))
        elif "<" in flags:
            if previous_index is None:
                # A malformed reuse flag is still reported as an unsupported
                # argument rather than silently accepting it.
                argument_index = 0
            else:
                argument_index = previous_index
        else:
            argument_index = next_implicit
            next_implicit += 1

        conversion_type = f"{match.group('date') or ''}{conversion}".lower()
        result[(argument_index, conversion_type)] += 1
        previous_index = argument_index
    return result


def load(directory: Path) -> tuple[dict[str, ET.Element], list[str]]:
    """Load resources from one values directory and collect structural errors."""

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
    """Map a resource to its value(s), keyed by plural quantity where needed."""

    if element.tag == "plurals":
        return {item.get("quantity", ""): text(item) for item in element}
    return {"value": text(element)}


def expected_value(
    source: dict[str, str], quantity: str, is_plural: bool
) -> str | None:
    """Find the source value corresponding to a localized value.

    Android plural rules are locale-specific. A locale can add a named
    category such as ``few`` even when the English source only contains
    ``one`` and ``other``. In that case the source ``other`` item is the only
    safe fallback. Non-plural resources never use a fallback quantity.
    """

    if quantity in source:
        return source[quantity]
    if is_plural and quantity in PLURAL_QUANTITIES:
        return source.get("other")
    return None


def validate(res_dir: Path) -> list[str]:
    """Return all validation errors found below ``res_dir``."""

    default_dir = res_dir / "values"
    source, errors = load(default_dir)
    if not source:
        errors.append(f"{default_dir}: no default string resources found")

    for directory in sorted(res_dir.glob("values-*")):
        localized, load_errors = load(directory)
        errors.extend(load_errors)
        for name, translated in localized.items():
            original = source.get(name)
            if original is None:
                errors.append(f"{directory}: '{name}' has no default fallback")
                continue
            if original.tag != translated.tag:
                errors.append(
                    f"{directory}: '{name}' is {translated.tag}, expected {original.tag}"
                )
                continue
            if translated.get("formatted") == "false" or original.get("formatted") == "false":
                continue
            if translated.tag == "plurals" and "other" not in values_for(translated):
                errors.append(
                    f"{directory}: plural '{name}' is missing the required 'other' quantity"
                )

            original_values = values_for(original)
            for quantity, translated_text in values_for(translated).items():
                translated_signature = placeholders(translated_text)
                # A locale-specific category may intentionally spell out a
                # non-numeric value (for example, Arabic's `zero` item).
                # There is no source formatter to compare in that case, but a
                # newly introduced formatter must still be rejected.
                if (
                    translated.tag == "plurals"
                    and quantity not in original_values
                    and not translated_signature
                ):
                    continue
                expected_text = expected_value(
                    original_values, quantity, translated.tag == "plurals"
                )
                if expected_text is None:
                    continue
                expected_signature = placeholders(expected_text)
                # Android permits a translation to omit a numeric argument
                # when the language expresses the quantity in the surrounding
                # word. It must not introduce an argument with a different
                # position or type, however.
                unsupported = translated_signature - expected_signature
                if unsupported:
                    errors.append(
                        f"{directory}: incompatible format arguments in "
                        f"'{name}' ({quantity}): expected {dict(expected_signature)}, "
                        f"found {dict(translated_signature)}"
                    )
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--res-dir", type=Path, default=Path("app/src/main/res"))
    args = parser.parse_args()

    errors = validate(args.res_dir)
    if errors:
        print("Translation resource validation failed:", file=sys.stderr)
        print("\n".join(f"- {error}" for error in errors), file=sys.stderr)
        return 1
    print("Translation resource validation passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
