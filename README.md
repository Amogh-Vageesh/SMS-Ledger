# SMS Ledger v1.100

Release update based on v1.99.

## UI and Ledger
- Moved Total Balance from the Ledger/Family home page to My Ledger.
- Standardized mobile typography and spacing retained from prior release.
- Enlarged the My Ledger expense donut/chart area for better mobile readability.
- My Ledger now shows only the top 5 expense categories in the category visualization and category list.

## Assets and liabilities
- Added a highlighted Asset financing / loans area in Assets.
- Each active asset has a visible "+ Add loan / lender" action.
- Loans can be created for a specific asset and tracked separately by purpose and lender/source.
- Lender source can be Bank, NBFC, Family Member, Friend, Employer or Other, with a lender/source name.
- Existing linked transaction history and loan/interest calculations are retained.

## Build metadata
- versionCode: 95
- versionName: 1.100

The project intentionally does not require any check_v*.js or extracted inspection files for APK building.


## Build fix in v1.101

The previous v1.100 package used Android Gradle Plugin 8.7.3 but the GitHub Actions workflow requested Gradle 8.7. AGP 8.7.x requires Gradle 8.9, so the workflow has been corrected to Gradle 8.9.

For Android Studio, use JDK 17 and Gradle 8.9 (or let Android Studio use its configured compatible Gradle distribution). The ZIP is a complete Android project; `check_v*.js` files are not required.

Important: `app/release.keystore` is included because the project currently uses it for release signing. Keep it private.
