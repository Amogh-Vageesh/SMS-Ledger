# SMS Ledger v1.152

## Changes
- Removed repeated per-section share controls from My Ledger and account/asset panes; kept the dedicated top-level sharing controls.
- Removed the extra Share button from the Subscriptions & Recurring header so the expand chevron has its own aligned space.
- Improved Family Hub header/card styling to match Home and removed the selected-family subtitle from that header.
- Made recurring totals period-scoped from linked Ledger transactions; Sports and Utility Bills show their configured amount if there are no linked payments in the selected period.
- Added category-colored labels to expense transaction rows.
- Removed redundant “Remaining” text from liability balances and emphasized pending balances in red.
- Protection rows prefer the linked asset name and show the chosen policy scope where available; expiry is shown as month/year.
- Improved wrapping and overflow in linked-transaction windows.
- Added a Loans Given section to record borrower, amount given, amount repaid, dates, rate, status and notes. These records are not yet included in net-worth totals or linked automatically to Ledger transactions.

## Validation
- Inline JavaScript syntax check passed.
- ZIP integrity check passed.
- Android build and device-level UI tests were not run because this environment has no configured Android SDK/Gradle installation.
