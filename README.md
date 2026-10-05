# SMS Ledger v1.72

## Shared family transaction editing

v1.72 adds collaborative editing for transactions that are shared through Family Cloud.

- A transaction owner can share an expense with all family members or selected members.
- The owner can choose **Allow shared family members to edit merchant, category, note, asset and event**.
- Family members who have access and edit permission can open the shared transaction on their own device and update those metadata fields.
- Amount, transaction type, date, currency, FX rate and original SMS remain read-only for another family member so the source financial fact cannot be silently changed.
- Changes are stored as a separate Firestore `transactionEdits` record, so the original transaction is not duplicated or overwritten by another member.
- The edit is pushed to all permitted family devices through the existing Firestore listener.
- The owner device also receives the shared edit and applies it to its local transaction.
- Sharing permissions are respected: `All family` or the selected audience. If editing is disabled, the transaction is view-only.
- Shared transaction edits include merchant, category, note, linked asset, person/beneficiary tag and event tag.

## Firebase rule update required

The APK contains the updated `firestore.rules`, including the `transactionEdits` collection. The Firebase project must use these rules for collaborative editing to work.

If deploying with Firebase CLI, run:

```bash
firebase deploy --only firestore:rules
```

or publish the contents of `firestore.rules` from the Firebase Console.

## Build

GitHub Actions uses Gradle 8.7 and JDK 17 to build the release APK.

Version: **1.72**  
Version code: **67**
