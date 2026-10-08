# SMS Ledger v1.112

## Build fix
- Fixed Android biometric app-lock compilation by using `FragmentActivity`, which matches the constructor exposed by `androidx.biometric:biometric:1.1.0`.
- Added the compatible `androidx.fragment:fragment-ktx` dependency.
- Fixed the Kotlin recursive type-checking error caused by the JavaScript bridge method `setAppLockEnabled` shadowing the private implementation method; the implementation is now named `applyAppLockEnabled`.
- No functional changes to the previously implemented Ledger, Assets, Liabilities, Loan, SMS reconciliation, Family Sharing, Insurance, performance, or security features.
