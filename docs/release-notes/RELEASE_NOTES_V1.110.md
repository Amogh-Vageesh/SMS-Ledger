# SMS Ledger v1.110

## Loan balance zero-entry fix
- Loan account editing now accepts an explicit ₹0 outstanding balance.
- When a loan has stored tranches, entering zero synchronizes tranche outstanding values to zero instead of restoring the previous tranche total.
- A manually entered zero is retained as the current balance until later verified loan transactions or statement reconciliation provide a different authoritative balance.
- Original loan amount and loan history remain intact.
