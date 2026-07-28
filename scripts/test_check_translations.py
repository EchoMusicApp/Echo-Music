#!/usr/bin/env python3
"""Regression tests for scripts/check_translations.py."""

from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).parent))

from check_translations import placeholders, validate  # noqa: E402


class TranslationValidatorTests(unittest.TestCase):
    def test_formatter_positions_are_part_of_signature(self) -> None:
        self.assertNotEqual(
            placeholders("%1$d %2$s"),
            placeholders("%2$d %2$s"),
        )
        self.assertEqual(
            placeholders("%1$d %2$s"),
            placeholders("%2$s %1$d"),
        )
        self.assertEqual(placeholders("100%% complete"), {})

    def test_plural_quantities_are_not_combined(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            (root / "values").mkdir()
            (root / "values-fr").mkdir()
            (root / "values" / "strings.xml").write_text(
                """<resources>
                    <plurals name="songs">
                        <item quantity="one">%1$d song</item>
                        <item quantity="other">%2$s songs</item>
                    </plurals>
                </resources>"""
            )
            (root / "values-fr" / "strings.xml").write_text(
                """<resources>
                    <plurals name="songs">
                        <item quantity="one">%1$d chanson</item>
                        <item quantity="other">%1$d chansons</item>
                    </plurals>
                </resources>"""
            )

            errors = validate(root)
            self.assertTrue(any("songs" in error and "other" in error for error in errors))

    def test_locale_specific_plural_category_falls_back_to_other(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            (root / "values").mkdir()
            (root / "values-ar").mkdir()
            (root / "values" / "strings.xml").write_text(
                """<resources><plurals name="songs">
                    <item quantity="one">%d song</item>
                    <item quantity="other">%d songs</item>
                </plurals></resources>"""
            )
            (root / "values-ar" / "strings.xml").write_text(
                """<resources><plurals name="songs">
                    <item quantity="zero">لا أغاني</item>
                    <item quantity="one">%d أغنية</item>
                    <item quantity="other">%d أغانٍ</item>
                </plurals></resources>"""
            )

            self.assertEqual(validate(root), [])


if __name__ == "__main__":
    unittest.main()
