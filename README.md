SMS Ledger v1.85 — Loan top-up and balance-transfer model

New in v1.85:
- Loans can contain multiple independent tranches.
- Add a loan top-up without overwriting the original loan. Each top-up stores its own amount, interest rate, start date and tenure.
- Weighted interest rate and total sanctioned/outstanding amounts are shown for multi-tranche loans.
- Transfer a loan from one bank/lender to another. The old loan is marked transferred/closed and hidden from active liabilities; a new loan account is created with a transfer tranche and optional separate top-up tranche.
- A balance transfer does not double-count the liability.
- Loan disbursement/top-up credits are classified as Loan proceeds, not income.
- Balance-transfer/takeover settlement messages are classified as Loan transfer, not spending.
- If a loan balance SMS arrives for an existing loan with tranches, the tranche outstanding amounts are reconciled to the latest bank-reported balance.
- Existing v1.84 asset-linked loan interest and transaction-linking behavior is retained.

Version: 1.85 / versionCode 81.

Important:
- Loan statement/payment schedule imports remain the source of truth for actual principal/interest. Tranches are the structural model used to keep original loans, top-ups and lender transfers separate.
- The app does not assume that a top-up has the same rate as the original loan.
