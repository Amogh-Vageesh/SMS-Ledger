# SMS Ledger v1.139

- Replaced My Ledger period filter handler with a single canonical render pass after persisting the selected period.
- Added top-right Share actions to My Ledger section headers, including Subscriptions & Recurring.
- Added a sticky horizontally scrollable My Ledger section jump bar so users can move between expanded sections without long scrolling.
- Capped the linked subscription transaction preview height so it remains navigable on mobile.
- Builds on v1.137 date-normalization and transaction lookup changes.

Validation: JavaScript syntax and ZIP integrity checked; Android build and device runtime remain unverified.
