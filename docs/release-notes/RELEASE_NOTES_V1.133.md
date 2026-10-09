# SMS Ledger v1.133

- Fixed My Ledger period-boundary handling for Month, Quarter, Yearly and All years, using normalized ISO dates.
- Recurring items now include manually saved records and transactions explicitly tagged with a subscription/recurring key, even when automatic schedule detection cannot infer a pattern.
- Recurring payment rows use the selected period and display a clear no-payments message when no linked transactions exist in that period.
- Added section-level sharing switches to Assets, Bank Accounts, Other/Investment Accounts, Credit Cards and Loan Accounts, alongside the existing My Ledger and Protection section controls.
- Consolidated Protection actions and summary into one card; removed annual premium from the summary; kept the normal app card typography and aligned three summary values in a responsive grid.
- Kept release notes under `docs/release-notes/`.

Validation: JavaScript syntax and ZIP integrity checked. Android APK build/device behavior not verified in this environment.
