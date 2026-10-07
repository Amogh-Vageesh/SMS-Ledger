# SMS Ledger v1.108

Consolidated release based on v1.107.

## Latest fixes
- Dedicated mobile-aligned Liabilities sheet with lender/source on the left, amount on the right, and two-column loan/remaining or outstanding/limit details.
- Credit-card outstanding is included in total liabilities; card limits remain excluded.
- Subscriptions & recurring section is collapsed by default and expands to show the complete list.
- SMS duplicate reconciliation now treats an identical SMS fingerprint as definitive even if an older record has a different parsed date/category.
- Manual category, loan link, asset link, notes, reference and statement metadata are preserved when duplicates are merged.
- Loan balances continue to reconcile from verified linked transactions; verified loan receipts can correct an initially entered original loan amount.
- Ledger loan tags and loan-account linked transactions remain synchronized.
- Granular family sharing: each family member can have independent ON/OFF permissions for Ledger/Transactions, Bank Accounts, Credit Cards, Investments, Assets, Loans & Liabilities, Insurance, Cash/Wallets, Retirement/EPF/NPS, and Budgets & Goals.
- Unshared sections are removed from the shared state and therefore do not contribute to family totals.
- Section-specific Add actions and the approved compact Assets/Liabilities UI are retained.

## Build
- versionName: 1.108
- versionCode: 103
- Gradle workflow: 8.9
- JDK: 17
