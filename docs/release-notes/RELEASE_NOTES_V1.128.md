# SMS Ledger v1.128

## Home-style Family Hub, My Ledger rendering, Protection UI and Events
- Fixed My Ledger rendering so the complete page populates instead of leaving the old period-summary/blank body visible.
- My Ledger keeps the Assets-style collapsible sections: Income, Expenses, Transfers, Expense Categories, Events, Subscriptions & Recurring, and Recent Transactions.
- Added a safe fallback if one section fails to render, so the Ledger never becomes visually blank.
- Unified the Family Hub header across financial tabs with the Home-style card, member chips, selected member, shared-data status, and a clear Share action.
- Protection add controls are compact mini buttons rather than large cards.
- Protection continues to use collapsible categories and two-column policy rows.
- Added a working Event Details dialog from Home, My Ledger and overview event rows. It shows linked transactions, spending, received amounts, and supports opening the original transaction; local events can be edited.
- Added mobile-safe two-column CSS for Ledger, subscription and Protection rows.
