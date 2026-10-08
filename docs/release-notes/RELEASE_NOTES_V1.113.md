# SMS Ledger v1.113

## Loan closure date
- Added a dedicated **Close loan** action for loan accounts.
- Closing a loan requires/selects a closure date.
- Closed loans are moved to **Closed loans / Closed accounts** and excluded from current liabilities.
- The closing balance is preserved as `closedBalance` for historical reference.
- Closed loans show the closure date in Loan history.
- Loans can be reopened; reopening clears the closure status/date and returns the account to active liabilities.
- Editing an existing loan also provides Loan status and Closure date fields.
- Existing loan, liability, SMS reconciliation, family sharing, insurance, performance, app-lock and UI fixes from v1.112 are retained.

Version: 1.113
Version code: 108
