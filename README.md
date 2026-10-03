# SMS Ledger v1.63

GitHub-ready Android project.

Changes in v1.63:
- Family members can select shared family events when adding an expense.
- Event selection now resolves both local and remotely synced family events.
- Selected remote event remains visible in the expense form and is saved by event ID.
- Event picker labels remotely synced events as shared events.
- New events created from the expense picker retain owner identity.
- Version 1.63 / versionCode 58.

Build through `.github/workflows/build.yml`.


## v1.64 changes
- Home/Family Hub no longer displays income, expenses, monthly balance, or total balance. Those remain in My Ledger (Summary).
- Bottom navigation label changed from Summary to My Ledger.
- Home All family view shows only data shared among family members: shared events, calendar items, shopping items, and shared family data.
- Selecting a family member shows their shared events and shopping list; events they created remain visible when other members tag expenses to them.
- Home period controls were removed because financial period analysis belongs in My Ledger.
