# SMS Ledger v1.145

## My Ledger period consistency and category fixes
- Kept transfer records visible in the selected My Ledger period even though transfer categories are excluded from spending totals.
- Included transfers in All Years date-range discovery.
- Added transfer-category totals and transaction rows to the Transfers section, with support for transfer flags and friends/family transfers.
- Excluded transfer records from expense totals.
- Changed Subscriptions & Recurring category and item amounts to show actual linked transactions within the selected period; expected monthly amounts are no longer added to actual period spending when there are no linked transactions.
- Updated recurring category total labels to the selected period and aligned the recurring section heading with other Ledger sections.
- Cached subscription transaction lookups during a single render to reduce repeated scans.

## Validation limits
JavaScript syntax and archive integrity checks are required before release. Android compilation and device/runtime testing have not been performed in this environment.
