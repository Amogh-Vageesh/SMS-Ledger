# SMS Ledger v1.131

## Fixes and improvements
- My Ledger is no longer hidden behind the SMS-permission gate; manually entered transactions and empty states remain visible, while SMS-derived transactions still follow the consent/readiness filter.
- My Ledger includes an expense category donut chart tied to the active Month / Quarter / Yearly / All years period.
- Family Hub on finance pages uses the Home-style card and display-name chips, with Share at the upper right.
- My Ledger accordion sections include per-section sharing switches; outgoing transaction sharing respects the selected Ledger section keys.
- Subscriptions & Recurring shows linked transactions under expanded subscription items, opens the original Ledger entry, and retains edit/delete actions.
- Subscription and Protection transaction-link sheets have search, category and period filters, plus a bounded scroll area and responsive two-column alignment.
- Protection top area shows insurance cover, warranty cover and maintenance package totals after the compact action buttons.
- Protection Insurances, Warranties and Maintenance sections collapse/expand like My Ledger sections and include sharing controls.
- Insurance, warranty and maintenance records support a covered property / vehicle / item field.
- Historical release notes remain consolidated under `docs/release-notes/`.

## Validation
- JavaScript syntax checked with Node.js.
- ZIP integrity checked.
- Android APK was not compiled in this environment.
