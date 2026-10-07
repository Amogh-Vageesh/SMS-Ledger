# SMS Ledger v1.98 — Family Finance Extension

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


## v1.98 follow-up fixes
- Unified app-wide mobile typography and spacing.
- Assets sections use compact descriptions and expand/collapse interaction.
- Active accounts are the default account view; Credit Cards and Closed Accounts have dedicated views.
- Loan accounts support Bank / NBFC and Friends & Family lender types.
- Asset-linked transactions are fully scrollable and open the original Ledger entry when tapped.
- Vehicle/asset loan interest can be derived from linked or matching loan/EMI data, including unlinked vehicle loans such as Corolla Altis.
