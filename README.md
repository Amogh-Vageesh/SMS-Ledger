# SMS Ledger v1.75

Bank statement import only. Gmail import/authorization UI and native bridge have been removed for now.

## Historical statement import
- Historical window: January 2022 onward.
- Select one or multiple bank statement files in one operation.
- Supports PDF/text/CSV plus legacy Excel .xls (Apache POI HSSF).
- Existing SMS transactions are matched first using date, amount, debit/credit, reference/UPI data and merchant/description.
- Matching adds statement provenance to the existing transaction instead of creating a duplicate.
- Multiple overlapping statement files are processed against the same ledger, so repeated rows are not intentionally added as separate transactions.
- Statement-only rows are added as new ledger transactions.
- Review is conservative when a possible match is found but cannot be confidently reconciled.

## Important
- This build does not include Gmail access.
- The attached sample .xls format is supported as an Excel workbook.
- Run the GitHub Actions Android release build to verify the full Android/Gradle dependency resolution.
