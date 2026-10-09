# SMS Ledger v1.153

## Variable recurring and subscription amounts
- For variable recurring items, display the average actual linked payment in the currently selected Ledger period (month, quarter, year, or all years).
- If no linked payment exists in the selected period, display the configured typical/average amount.
- Category totals remain the total actual spending in the selected period; they are not replaced by averages.
- Fixed recurring entries continue to show period spending when linked payments exist, and use the configured amount fallback for Sports and Utility Bills when no linked payments exist.

## Validation
- JavaScript syntax and archive integrity checks are run on the source package. Android compilation/device runtime testing is not included unless explicitly performed in an Android build environment.
