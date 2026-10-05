# SMS Ledger v1.68

Changes in v1.68:
- Fixed FamilyCloud.kt Kotlin compilation errors caused by calling String.length as a function.
- Complete-data sharing chunk calculations now use the Kotlin String.length property.
- Retains v1.67 complete family data sharing and all previous Family Hub/My Ledger fixes.
- GitHub Actions workflow included for release APK build.


## v1.70 Asset Market Intelligence & Asset-Linked Spending
- Added market intelligence fields: valuation range, confidence, outlook, source and freshness.
- Gold supports live spot refresh using the existing market endpoint; the app retains manual values if the service is unavailable.
- Added asset-linked transaction mapping for EMI/loan payments and indirect asset expenses such as repairs, upgrades, maintenance and insurance.
- Added conservative asset-link suggestions based on merchant/SMS text; the user must confirm the suggested asset.
- Loan accounts can now be linked to Property/Home, Vehicle or Gold assets; selecting an asset can set the corresponding loan category.
- Asset detail now shows linked EMI/loan payments, indirect expenses and number of linked entries.
- Property/vehicle values remain source-backed/manual rather than inventing a market price.
