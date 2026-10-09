# SMS Ledger v1.140

- Reduced My Ledger navigation effort by moving Recent Transactions directly below the balance/expense summary and opening it by default. The jump bar now puts Recent and Recurring first.
- Fixed subscription linked-payment previews so they show only transactions inside the selected period; they no longer fall back to unrelated older payments when the selected period has no payments.
- Normalized transaction dates in subscription/protection link-picker rows and corrected inclusive month/quarter/year filtering to avoid raw-date string slicing issues.
- Preserved the Month / Quarter / Yearly / All years period controls; the period selector contains only period choices, not income/expense type filters.

Validation performed: inline JavaScript syntax check and ZIP integrity check. Android Gradle build and on-device behavior were not verified.
