# SMS Ledger v1.151

## Source changes
- Hardened yearly selection: selecting a year updates `ovYear`, `cursor`, saved state, and refreshes the Ledger view directly.
- Made subscription transaction matching more tolerant of normalized recurring keys, merchant names, and tagged debit/expense rows to reduce false zero totals.
- Aligned Subscriptions & Recurring Share and expand controls.
- Hid Manage Family controls from non-Home tabs while preserving the Home placement.
- Standardized Family Hub member-chip sizing and card alignment across tabs.
- Made Protection expiry month/year the visible right-side value and allowed expiry/provider details to wrap on narrow screens.

## Validation
JavaScript syntax and ZIP integrity checks are run for this source package. Android compilation and on-device runtime testing are not available in the current environment; these changes are not claimed as device-verified.
