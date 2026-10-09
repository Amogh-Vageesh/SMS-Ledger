# SMS Ledger v1.149

## Fixes and source-level improvements
- Normalize transaction and event dates before applying selected-year boundaries.
- Build year-picker options from normalized transaction dates, including slash-separated date formats.
- Put the Settings quick-jump selector before the Language section.
- Keep explicitly marked English date UI out of the Kannada text mutation pass.
- Expand Kannada translations for common Ledger, period, Protection, sharing, and settings labels.
- Clarify that the headline Total Balance is the current actual balance and is not expected to change with a historical period filter; income, expenses, categories, events and recurring payment totals are period-specific.

## Validation limitations
JavaScript syntax and ZIP integrity checks are required before distribution. Android compilation/device testing were not performed in the available environment.
