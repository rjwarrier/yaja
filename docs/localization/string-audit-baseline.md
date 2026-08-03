# String Audit Baseline

Created: 2026-08-03

Run the audit with:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests com.mj.yaja.localization.StringResourceAuditTest
```

The test writes the full per-key report to:

```text
app/build/reports/localization/string-audit.md
```

The committed regression baseline lives at:

```text
app/src/test/resources/localization/string-audit-baseline.properties
```

## Current Baseline

- Base strings: 1,303
- Base plurals: 28
- Translated locales: 44
- Hardcoded UI string candidates: 14
- Missing strings: 6 locales have 175 missing strings; 38 locales have 191 missing strings
- Missing plurals: every translated locale is missing 1 plural resource
- Extra strings/plurals: 0
- Duplicate string/plural keys: 0
- Placeholder mismatches: 239 total

The audit intentionally does not require all existing localization debt to be fixed before passing. It fails when the risky counts get worse, so new strings, plurals, placeholder mistakes, duplicate keys, or new hardcoded UI literals are visible immediately.

## Locale Summary

| Locale | Missing strings | Missing plurals | Placeholder mismatches | Identical to English |
|---|---:|---:|---:|---:|
| values-am | 175 | 1 | 58 | 13 |
| values-ar | 191 | 1 | 2 | 14 |
| values-bn | 191 | 1 | 8 | 33 |
| values-cs | 191 | 1 | 2 | 52 |
| values-da | 191 | 1 | 1 | 70 |
| values-de | 191 | 1 | 1 | 54 |
| values-el | 191 | 1 | 2 | 45 |
| values-es | 191 | 1 | 3 | 52 |
| values-fa | 191 | 1 | 3 | 30 |
| values-fi | 191 | 1 | 3 | 47 |
| values-fil | 191 | 1 | 1 | 108 |
| values-fr | 191 | 1 | 3 | 60 |
| values-gu | 175 | 1 | 13 | 21 |
| values-he | 191 | 1 | 3 | 32 |
| values-hi | 191 | 1 | 7 | 36 |
| values-hu | 175 | 1 | 3 | 52 |
| values-in | 191 | 1 | 2 | 63 |
| values-it | 191 | 1 | 2 | 48 |
| values-ja | 191 | 1 | 6 | 36 |
| values-kn | 191 | 1 | 7 | 195 |
| values-ko | 191 | 1 | 7 | 23 |
| values-ml | 191 | 1 | 9 | 31 |
| values-mr | 191 | 1 | 8 | 30 |
| values-ms | 191 | 1 | 2 | 59 |
| values-nb | 191 | 1 | 1 | 63 |
| values-nl | 191 | 1 | 3 | 57 |
| values-or | 175 | 1 | 12 | 23 |
| values-pa | 175 | 1 | 7 | 20 |
| values-pl | 191 | 1 | 1 | 31 |
| values-pt | 191 | 1 | 3 | 53 |
| values-ro | 191 | 1 | 0 | 66 |
| values-ru | 191 | 1 | 3 | 14 |
| values-sv | 191 | 1 | 1 | 68 |
| values-sw | 191 | 1 | 4 | 48 |
| values-ta | 191 | 1 | 8 | 69 |
| values-te | 191 | 1 | 9 | 43 |
| values-th | 191 | 1 | 2 | 15 |
| values-tr | 191 | 1 | 4 | 48 |
| values-uk | 191 | 1 | 2 | 32 |
| values-ur | 191 | 1 | 5 | 24 |
| values-vi | 191 | 1 | 4 | 34 |
| values-zh | 191 | 1 | 8 | 24 |
| values-zh-rTW | 191 | 1 | 5 | 20 |
| values-zu | 175 | 1 | 1 | 37 |

## Update Protocol

When localization debt is intentionally reduced, run the audit, inspect the generated Markdown report, then update the properties baseline only after reviewing the resource changes. Do not raise any baseline count to make a failing build pass unless the new debt is intentional and documented.
