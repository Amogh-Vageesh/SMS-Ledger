SMS Ledger v1.86 — Dedicated loan document import

New in v1.86:
- Added Settings → Loan documents as a dedicated import area, separate from normal bank-statement import.
- Import one or multiple PDF, XLS/XLSX, CSV or text loan documents using the Android document picker.
- Supported document types: payment/amortization schedule, loan account statement, top-up/enhancement document, and balance-transfer/takeover document.
- Each imported document is classified with editable fields for lender, loan name, original/transferred amount, current outstanding, interest rate, EMI, start/transfer date, tenure and loan category.
- Existing loans can be selected and updated; a new loan can be created directly from a document.
- Top-up documents add a separate loan tranche with its own amount, rate, date, tenure and outstanding balance.
- Balance-transfer documents close the old lender's active loan and create a new lender loan with a transfer tranche; optional transfer-time top-up is stored separately, preventing double-counting of liabilities.
- Loan documents are metadata/audit records. Forecast payment schedules do not create artificial spending transactions. Actual cash transactions should still come from SMS or bank-statement import.
- Extracted values are suggestions and must be reviewed before saving because lender PDFs can vary in layout.
- Kannada labels were added for the new loan-document workflow.

Version: 1.86 / versionCode 82.

Existing v1.85 loan features retained:
- Multiple independent loan tranches.
- Original loan + top-ups with separate rates.
- Bank-to-bank balance transfer/takeover.
- Weighted interest rate and total sanctioned/outstanding values.
- Loan proceeds and loan-transfer transaction classifications.


## v1.87 loan document reconciliation
- Loan documents are imported from the Asset screen or Settings and must be attached to an existing asset/loan; the importer no longer silently creates a new loan.
- Multiple statement documents can be selected and are processed sequentially against the same asset/loan.
- Each statement is retained as a document source; payment rows are reconciled against existing SMS transactions before a new Ledger transaction is created.
- Matched SMS + statement payments retain both sources; unmatched confident payment rows are added as Loan repayment entries and linked to the asset/loan.
- Loan statement/schedule metadata updates the linked loan account and asset without duplicating the liability.
- Lender detection prefers full bank names from the document header and no longer treats an incidental bank name in a narrative as the lender.
