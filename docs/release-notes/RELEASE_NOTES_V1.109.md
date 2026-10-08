# SMS Ledger v1.109

## Assets tab accordion fix
- Fixed expand/collapse for Assets, Bank accounts, Other accounts, Credit Cards, Loan accounts, and Subscriptions & recurring.
- Added explicit Android WebView-safe summary click handling instead of relying only on native `<details>` behaviour.
- Pane open/closed state is preserved while switching account filters and re-rendering the Assets tab.
- Default remains Active accounts with Bank accounts open; other sections are collapsed until opened.
- No changes to saved financial data.
