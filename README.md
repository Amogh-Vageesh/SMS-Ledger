SMS Ledger v1.80

Fixes:
- Transaction edit category selector is now a visible searchable picker with up to 40 categories and tap-to-select behavior.
- Improved My Ledger daily spending graph with a cleaner trend/area presentation and total-spend KPI.
- Version 1.80 / versionCode 76.

iPhone note:
The current project is Android-specific because SMS ingestion uses Android SMS APIs and Kotlin/Android WebView. It cannot be installed directly on iPhone. A future iOS companion app should use SwiftUI with the same Firebase backend; iOS can support shared ledger, manual transactions, bank-statement import, assets, family sync, etc., but automatic SMS reading like Android is not available in the same way.


## v1.81 category picker
The transaction edit screen restores the earlier simple Category dropdown. Tap Category and choose directly from the full category list; no typing/search is required.
