# SMS Ledger v1.89

Version: 1.89 / versionCode 85

## Loan document extraction and asset update fixes
- Loan documents imported from an existing asset update that asset and its linked loan instead of creating a new loan.
- Axis-style repayment schedules are parsed row-by-row.
- Current rate/outstanding/EMI are taken from the latest schedule row applicable as of the document date, not a future repayment row.
- Loan start date prefers the earliest statement-period start date; otherwise disbursement/start date; otherwise earliest schedule due date.
- Lender detection prioritizes known lender names from the filename/document and normalizes Axis/HDFC/ICICI/etc.
- Interest-rate history is retained for parsed schedule rows.
- Asset now exposes current outstanding, current interest rate and current EMI fields.
- Imported document data remains after removing the original document record.

## Backup
Backups retain processed financial data, loan/asset metadata, statement reconciliation and document metadata. Original source PDFs/XLS files are not required to restore processed data.

## Update
Install v1.89 over v1.88; do not uninstall first. The package ID and release signing key are retained, so the Android app can be updated in place when the APK is signed with the same release key.


## v1.91 UI update
- Renamed bottom Accounts tab to **Assets**.
- Assets moved out of the monthly Summary/Overview and into the Assets tab.
- Assets tab now groups: Assets, Bank accounts, Other accounts, Loan accounts, Bills due.
- Each section can be expanded/collapsed and explicitly hidden/shown.
- Monthly expense summary now uses a donut chart with category amount and percentage, matching the requested first graph design.
- Existing asset/loan/account data structures are retained.


## v1.91 UI and loan-data update
- Moved Net worth to the top of the Assets tab.
- Reworked Assets into persistent section cards for Assets, Bank accounts, Other accounts, Loan accounts and Bills due; removed expand/hide controls.
- Kept the expense donut on My Ledger and removed the duplicated Net worth card from My Ledger.
- Loan-linked assets now sync original amount, outstanding, loan start date, current rate, current EMI, interest paid from processed loan documents, EMI history and rate history from the linked loan record when the asset is opened.
- Loan document processing now stores parsed interest/EMI metadata for future use. Existing documents imported before these fields were stored may need a one-time re-import to calculate interest paid accurately.
- Existing transactions and document reconciliation are retained.


## v1.92 UI fixes
- Fixed My Ledger expense donut legend alignment on narrow Android screens.
- Added a stronger top/scroll reset when switching bottom tabs so My Ledger does not reopen partway down the page.
- Moved unpaid bills due in the current month into My Ledger as a dedicated “Bills due this month” card.
- Removed current-month Bills Due from the Assets page; future-dated unpaid bills remain in Assets.
- Hardened the Assets renderer so a malformed subscription/account/asset record cannot blank the entire Assets tab.
- Assets tab continues to show Net Worth at the top, followed by Assets, Bank accounts, Other accounts, Loan accounts and recurring items.
