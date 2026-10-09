# SMS Ledger v1.133

Source release with My Ledger, Protection, transaction-linking and Family Hub fixes.

SMS Ledger v1.108 — consolidated ledger, loan reconciliation, liability UI, recurring collapse, and granular family sharing.

SMS Ledger v1.106 — Loan reconciliation, safe SMS deduplication and liabilities UI

- Scan & reconcile existing Ledger: safe duplicate merge preserving corrected categories, loan links, asset links and notes.
- Loan-linked credits can establish the verified original loan amount; tagged repayments reduce verified outstanding when no newer statement balance overrides it.
- Loan account linked transactions include both loan receipts and repayments.
- Loan statement outstanding date is retained so post-statement verified repayments are reflected without double-counting earlier statement payments.
- EMI / loan transactions show a loan-account link selector; bank/NBFC statement matching can populate the link.
- Liabilities modal uses a consistent mobile two-column layout.
- Credit-card outstanding is included in liabilities; credit-card limits are excluded.
- v1.113 / versionCode 108.
