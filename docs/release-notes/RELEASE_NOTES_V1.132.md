# SMS Ledger v1.132

- Corrected subscription period boundaries to use inclusive ISO dates, so month, quarter, and year filters include transactions throughout the selected period.
- Expanded the visible linked-payment preview for recurring entries and added an explicit empty-period message with guidance to link transactions or change the filter.
- Added a separate Protection Summary card for combined insurance/warranty coverage, annual insurance premiums, warranty cover, and maintenance package costs.
- Added separate share controls for Insurances, Warranties, and Maintenance packages in addition to existing My Ledger section-level controls.
- Added a Manage family action to the Family Hub card so users can discover where to add/join/manage family members.
- Hardened subscription state initialization to prevent missing recurring data from empty/uninitialized collections.
