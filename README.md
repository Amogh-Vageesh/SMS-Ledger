# SMS Ledger v1.88

## Loan document batch import and processed-data backup

- Import loan documents from an existing Asset → Loan → Import documents.
- Select multiple yearly statements/schedules together and review once.
- No automatic new-loan creation during document import.
- Extracts loan start date where clearly labeled and uses the latest applicable document for current interest rate/outstanding/EMI.
- Reconciles statement payment rows with existing SMS and creates statement-only Ledger entries only when no matching transaction exists.
- Retains document metadata and reconciliation history after the source file is removed.
- Remove document record without deleting processed Ledger, loan or asset data.
- JSON/manual and automatic backups contain processed transactions, accounts/loans, assets, loan document metadata, reconciliation and family data. Original PDF/XLS bytes are not included in the processed backup.
- Restore brings back processed loan/asset/document data in addition to Ledger entries.

Version: 1.88 / versionCode 84
