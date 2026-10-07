# SMS Ledger v1.97 — Fix Ledger Issues + Family Finance

This build starts from the supplied SMS Ledger v1.95 project and preserves the existing UI/design language rather than replacing it with a new prototype.

## Navigation / design rules
- **Ledger** remains the existing Ledger screen and keeps its existing **Family button** in the header. Its existing organizer/home design is preserved.
- **My Ledger** keeps the existing month/day/year/event financial UI. A compact **Family Hub** is added at the top with `All Family`, the signed-in person's display name, and the display names of people who shared data.
- **Assets** keeps the existing asset/account card design. Family Hub is added at the top, with Active/Closed asset filtering and separate Credit Cards.
- **Insurance** is a new dedicated tab using the same visual language as the current app. It has Family Hub, Active/Expired filters, coverage summary and Add Insurance.
- **Add** and **Settings** keep the existing design.

## Family Hub naming
- Only the person's chosen/display name is shown.
- No relationship labels such as Spouse, Child, Wife, Husband, Son or Daughter.
- Google-linked family members are prompted for a display name.

## Financial rules
- Credit-card limits are excluded from assets.
- Credit-card outstanding is a liability.
- EPF/investment/retirement balances remain financial assets.
- Self transfers are excluded from income/expense totals.
- Credit-card bill payments are excluded from spending totals.
- One underlying transaction should remain one ledger record; duplicate SMS records should be consolidated.
- Promotional/advertisement SMS should not become transactions.
- Shared family transactions/assets are combined without intentional double counting where shared IDs/keys are available.
- Closed/sold assets retain their history and can be viewed under Closed.
- Insurance is protection and is not counted as an asset or net-worth value; premiums remain expenses.

## Asset lifecycle
Assets can be marked Active or Closed/Sold. Closed/Sold records retain sale/closure date, selling price and selling expenses.

## Insurance
Insurance types include Health, Term/Life, Vehicle, Property, Travel & One-Time and Other. Expired/one-time policies remain in history.


## v1.97 release fixes
- Implements the complete Fix Ledger Issues pass across Home/My Ledger, Assets, Summary, SMS classification, transfers, cards, bills, mandates, family sharing and Google sign-in diagnostics.
- Home Total Balance is based on actual held financial-account balances; investment/retirement/EPF/PF balances are included, while credit-card limits are excluded.
- Internal transfers and credit-card bill payments do not inflate income or expenses.
- Asset/account sections are expandable/collapsible.
- Action buttons use consistent sizing.
- All pages use one unified Figtree/Noto Sans typography system with standardized title, section, body and small-text sizes.
- Family views show only each person's chosen/display name; relationship labels are not used.
- Version code/name are 92 / 1.97.
