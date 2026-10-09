# SMS Ledger v1.144

## Fixes
- Declared My Ledger period selection (`ovMode`) and selected year (`ovYear`) as explicit JavaScript state. This prevents the `id="ovMode"` browser named-element property from being mistaken for the selected period, which could stop Month / Quarter / Yearly / All years from applying correctly.
- Added top-right Share actions to Home page Events, Calendar, Shopping list, Shared activity, and Financial summary sections, matching the Share action pattern used in My Ledger. Existing section sharing controls remain available.

## Validation
- JavaScript syntax and ZIP integrity checked.
- Android compilation and on-device UI behavior were not verified in this environment. Please validate period changes against real transaction dates before treating this release as confirmed.
