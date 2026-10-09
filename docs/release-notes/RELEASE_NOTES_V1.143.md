# SMS Ledger v1.143

- Restored My Ledger period filtering to use the established `displayTxns()` pipeline before applying inclusive period bounds, addressing the regression introduced in v1.137.
- Kept the four period choices (Month, Quarter, Yearly, All years) and removes obsolete transaction-type filter controls from within My Ledger when rendered.
- Subscription transaction-link picker uses selected Ledger month/quarter/year boundaries and applies filters immediately when opened.
- Retains responsive Family Hub layout and removes the horizontal section-jump navigation.
