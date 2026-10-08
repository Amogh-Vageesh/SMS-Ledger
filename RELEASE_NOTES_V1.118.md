# SMS Ledger v1.118

## Combined release: subscriptions, recurring payments, protection links and shared/co-owned assets

### My Ledger
- Keeps the My Ledger period filters as **Month, Quarter, Yearly, All years and Events**.
- Subscriptions and recurring transactions are managed from My Ledger rather than Assets.
- Subscription categories include Entertainment, Utility Bills, Insurance Premiums, Sports, Housing & Maintenance, Education, Telecom, Software & Apps, Memberships and Other.
- Recurring payments support Monthly, Quarterly and Yearly frequency.
- Variable recurring bills can be recorded with optional unit, unit rate, minimum/base and maximum values; actual Ledger payments remain the source of truth.
- Recurring/subscription records can be linked to an Asset.
- Linked subscription payments can optionally apply an Asset link to selected existing Ledger transactions.

### Protection
- Insurance, Warranty and Maintenance records remain under Protection.
- Insurance/Warranty/Maintenance category rows are expandable, and individual records are directly tappable for editing.
- Existing Ledger transactions can be linked directly to a specific Protection record without creating duplicate financial entries.
- Maintenance plans support linked assets and linked transactions.

### Assets and co-ownership
- Assets support informational co-owner records with a share percentage or amount.
- Ownership share is explicitly **informational only** and is not used to split asset value, expenses, loans, interest or net-worth calculations.
- Asset-linked spending aggregates shared Ledger transactions from family members when those transactions are shared and carry the same asset ID, avoiding duplicate financial records.
- Existing loan/account and asset links are preserved.

### Duplicate asset validation
- New assets are checked against existing local assets using name/type and identifiers such as registration, VIN or serial number.
- Potential matches in shared family asset data trigger a validation request instead of immediately creating a duplicate.
- The existing asset owner can confirm that it is the same asset or reject the match as a different asset.
- A confirmed match tells the requester to use the existing shared asset rather than creating a duplicate.
- Asset validation requests are synchronized through the Family Cloud and protected by targeted audience rules.

### Build/version
- versionName: 1.118
- versionCode: 113
- Based on v1.117.
- JavaScript syntax checked with Node.js.
- ZIP integrity checked.
- APK compilation was not performed in this environment because the Android SDK/Gradle toolchain is unavailable.
