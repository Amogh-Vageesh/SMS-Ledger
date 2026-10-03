# SMS Ledger for Android

Reads bank transaction SMS automatically and builds your monthly expense ledger.
Everything stays on the phone: no server, no account, no analytics.

## How it works
- First launch asks for SMS access, then lets you choose how far back to read:
  3 months, 1 year, 5 years or all SMS on the phone. Settings has the same choices to import again later
  (entries already in the ledger are skipped, so re-importing is safe).
- Large imports are fed to the ledger in batches with a progress counter; 5 years of
  bank SMS usually takes under a minute.
- The ledger is stored in a private app file, so years of entries fit comfortably.
- Tap the month name to jump to any month; each year shows its total spend.
- When a bank SMS arrives you get a notification ("₹486.00 spent · tap to log").
  Anything received while the app was closed is picked up the next time you open it.
- Only messages from bank-style senders (e.g. AD-HDFCBK) that mention an amount and
  words like debited/credited/spent are read. SMS from phone numbers are ignored.
- UI and parser live in app/src/main/assets/index.html (same as the web version).
  Improve parsing there and rebuild.

## Get the APK without installing Android Studio
1. Create a private GitHub repo and upload the contents of this folder
   (keep the .github folder).
2. Open the repo's Actions tab. "Build APK" runs on every push (about 5 minutes).
3. Open the finished run, download sms-ledger-apk, unzip, and copy the .apk to your phone.

## Or build in Android Studio
Open this folder, let Gradle sync, then Build > Build APK(s), or connect the phone and press Run.

## Installing on the phone
1. Open the APK and allow "Install unknown apps" for your file manager when asked.
2. If Play Protect warns, choose "Install anyway".
3. If the SMS permission is greyed out as a "Restricted setting":
   Settings > Apps > SMS Ledger > (three dots) > Allow restricted settings, then grant SMS.
4. Xiaomi/Redmi, Oppo, Vivo, Realme and OnePlus phones often block background receivers.
   Set Battery to "No restrictions" and enable Autostart for live notifications.
   Import-on-open works regardless.

## Accounts tab
- Bills due: card statements and utility bill SMS with due dates. You get reminders 3 days
  before, the day before and on the due date. Bills are marked paid automatically when a
  matching card payment or biller payment shows up.
- Balances: bank balances and card limits from the latest SMS, grouped by type. Tap one to
  edit it or hide it, or add accounts by hand (PF, NPS, wallets and so on).
- Subscriptions and recurring payments, detected from your history, with a monthly and yearly
  total and a reminder the day before renewal.
- Reimbursements pending and money owed to you from split payments.

## Language
English or ಕನ್ನಡ (Kannada). Chosen on first launch; change any time in Settings > Language.

## Getting help in the app
- A one-time guided tour walks through Home, Details, Entries, Accounts, Add and Settings
  right after you pick a language on first launch.
- A small "?" button, always in the same spot near the bottom right, explains whatever
  screen you're currently on — tap it any time.
Both are in your language automatically.

## Second tab renamed
"Month" is now "Details" (ವಿವರಗಳು in Kannada), since it covers Month, Year and All years,
not just a single month.

## Merchant and person totals
Tap any merchant or person's name (in Entries, Home's top income sources, or the Month
tab's Top places) to see everything from them: total paid or received, entry count, and a
year-by-year breakdown you can expand into the actual entries. Tapping the rest of an entry
still opens it for editing, same as before.

## First-launch order, fixed for existing installs
Found and fixed a real bug: the tour and sign-in prompt only ran through the language-picker
chain, so anyone *updating* from an older version (language already chosen) never saw either.
Both now also run on a plain app boot, so this version fixes itself in place once installed.

## Dual SIM, now asked about up front
If the phone has more than one SIM with bank SMS, onboarding now asks once which is yours,
right after sign-in — no more needing to find it in Settings first. Settings still has the
exact same controls, now described as "update any time" rather than the only way to set it.

## Summary tab (renamed from Details) + Events filter
"Details" is now "Summary". Alongside Month / Year / All years, there's a new Events option
that lists every event with its total — tap one to open its category breakdown.

## Family Hub
A new Family icon on Home opens a dedicated page: add people, add events, and a prominent
"+ Add an expense" that opens the same entry sheet with tagging already built in. Each person
and event has a Share toggle — marked as local-only for now (sharing isn't live until cloud
sync is built), so nothing is silently inert without an explanation.

## First-launch order
Language, then a quick guided tour, then a skippable "Sign in with Google" prompt, then SMS
access. Signing in is never required to use the app — "Not now" moves straight on to SMS
permission, same as before this was added.

## Sign-in reliability
Found and fixed a gap where a failure during sign-in (Firebase not ready, Play Services
issue, etc.) could leave the button stuck on "Signing in…" with no error shown. Every error
path now reaches the page, and a 20-second timeout catches the case where the native side
never responds at all, so the button is never permanently stuck.

## People and events
Settings > People: add anyone you tag spending to (local for now; sharing comes once cloud
sync is built). Editing any entry adds "Who's this for" and "Tag to an event" — the event
picker searches existing events first and nudges toward a close match (same name or date)
before letting you create a near-duplicate. Accounts > Events lists each one with total
spent and a category breakdown, same as a trip.

## Cloud account (sign-in, step 1 of family sharing)
Settings > Cloud account: sign in with Google (via Firebase Auth + the modern Credential
Manager API). This is the first building block for live family sharing — right now it only
establishes who's signed in; events/people/lists don't sync across phones yet. That's next,
once this step is confirmed working on a real device.

## Theme
Settings > Theme: System (follows your phone), Light, or Dark. Applies immediately, including
the status and navigation bar colours, and is remembered across app restarts.

## Home
Income vs expenses by month (amounts in lakh on each bar), a category donut, and your top
5 income sources and expense categories, for the last 3, 6 or 12 months. Tap a month to see
its numbers. Dark or light follows your phone's setting.

## More promotional messages excluded
Loan-offer texts like "Congratulations! Loan up to Rs.20,000 may be credited to your bank
account" were being read as real credits, since they use the same wording ("credited") as a
genuine transaction. These are now recognised by their speculative phrasing ("may be
credited", "instant loan", "loan up to Rs...") and skipped. Along the way, a genuine loan
disbursement ("Rs.5,00,000 Personal Loan disbursed to your A/c...") that was being missed
entirely is now correctly recorded as income.

## NPS (Protean/NSDL format)
Protean's wording differs from EPFO's: "Investment value in Tier I (PRANXX7290) as on
30.06.2026 is Rs X" for the balance, and "PRAN XX7290: Units for (Mon-YYYY) contribution of
Rs Y credited" for each contribution. Both are now read correctly (account "XX7290" merges
into one NPS entry whether or not there's a space after "PRAN"), and a monthly contribution
counts as income, the same reasoning as an EPFO contribution: it's money you've earned that
never separately showed up as a bank debit, unlike a SIP confirmation (still excluded, since
that debit was already counted when it happened).

## More savings and investment balances
Beyond EPFO and NPS, the app now recognises balance SMS for PPF, Sukanya Samriddhi, Fixed
Deposits, Recurring Deposits and mutual fund value/folio updates, filing each under Accounts
> Other accounts with its own label instead of lumping it in as a plain bank balance. It also
fixed a gap where a balance message with a written-out date ("as on 31-Mar-26") could fail to
be read at all, since month names like Mar/Apr weren't being skipped over correctly.

## Dual SIM (two numbers, one phone)
If this phone has two SIMs each receiving bank SMS (yours and your spouse's, say), Settings
shows a "Dual SIM" section once it's found SMS from both. Name each SIM and mark which is
yours; the other is then treated exactly like a synced family member on Home's Family view,
with no Bluetooth pairing needed since both are already on this phone. No extra permission
is requested — SIM names are read from the SMS you've already given the app access to, or
fall back to "SIM 1"/"SIM 2" for you to rename.

## Family
Settings > Family: add your name, then tap "Sync with family nearby" on both phones while
they're close together. Check the 4-digit code matches on both, and the two ledgers swap
over Bluetooth or direct Wi-Fi (no internet needed). Home then has a "Just me / Family"
switch. Sync again whenever you're together to refresh.

## Standing Instructions and mandates
"We have activated a Standing Instruction... Merchant: X, Maximum Amount: Rs Y, Frequency:
monthly" sets up future charges; it isn't a charge itself. It now creates a subscription
(Accounts > Subscriptions) with the merchant, amount and frequency read from the message,
instead of being logged as a payment.

## Failed transactions worded as "not completed"
A transaction that a bank reports as "not completed" (rather than "declined" or "failed",
which were already excluded) was being read as a real expense, since it still describes an
amount and a transaction. It's now recognised and skipped like other failed payments.

## Advance bill notices ("please maintain sufficient balance")
A message like "Bill payment of Rs.958 for BESCOM is scheduled for 24-04-2026. Please
maintain sufficient balance" is a heads-up, not a completed payment — it used to get read as
an actual expense because it contains wording ("payment of") that looks like a debit. It's
now recognised and filed as a bill due on that date instead, the same as other advance
notices.

## EMI due notices
A message like "EMI of Rs.68,946 for Axis Bank Loan A/c XX7441 is due on 10-09-26" was
already treated as a bill, not an expense (it says "due on", which the app has recognised
from the start). It now also gets a clearer name ("Axis loan ··7441") and an "EMI" label in
the Bills list, the same as EMI notices worded as an upcoming auto-debit.

## EPF passbook updates
"Your passbook balance against <UAN> is Rs. 18,83,844. Contribution of Rs. 29,936 for due
month Dec-24 has been received" states two very different figures — a large running total
and this month's contribution. The app reads the contribution as income (money you've
earned, even though it lands in your EPF account rather than your bank) and the running
total separately as your EPFO balance under Accounts, instead of mistaking the running
total for a huge transaction.

## More ads and bill-reminder wording excluded
- "Please clear your bill of Rs.X for your Airtel Black ID..." was being read as an expense
  (the trailing "Ignore if already paid" disclaimer contains the word "paid", which looks like
  a completed payment to the scanner). It's now filed as a proper bill instead.
- Loan-offer ads phrased as "can be credited" or "Complete your application" (not just "may be
  credited") are now recognised and skipped too, alongside the earlier fix.

## Getting the right bank for each entry
Bank names are read from the message text, which can occasionally mention a different bank
as a third party (a loan lender, a NEFT sender) rather than the bank that actually sent the
SMS. Android also passes along the sender code (like "AD-HDFCBK"), and the app now prefers
that when it recognises it, which fixes most of these mix-ups. If an account still looks
wrong, open it under Accounts and tap Remove; it comes back on its own if a real SMS for it
arrives.

## If tapping "Sync with family nearby" does nothing
From 1.17, any problem starting a sync now shows a message instead of failing silently — so
if you still see nothing at all, that itself is worth reporting with a screenshot. Otherwise
check what the message says:

## If family sync won't connect
- Both phones need Bluetooth switched on. On Android 12 and older, the phone's Location
  toggle also needs to be on system-wide, even though the app never reads your location —
  that's an Android requirement for this kind of Bluetooth search, not a choice this app
  makes. From 1.14 the app checks both and tells you which one is off.
- Keep both phones on the Settings screen, unlocked, and close together for the full sync.
- Update Google Play services from the Play Store if it's old.
- On Xiaomi/Redmi, Oppo, Vivo or OnePlus, the same Autostart/battery settings mentioned above
  for SMS also affect Bluetooth scanning while the app is in the background.

## Money to and from people you know
Family and friends need their own category, distinct from Self transfer (which is for your
own accounts): "Friends and family transfers", also excluded from spend and income. Use it
for things like your spouse sending money for household expenses.

## Lending money to someone
When you get money back that you'd earlier paid to someone (a personal loan, not a bill
split), it shouldn't count as new income. There's a new "Loan repayment" category for this,
excluded from both spend and income like Self transfer and Card payments.
The same logic covers a normal bill split too, not just a full loan: if you paid the whole
bill and a friend's share is unsettled, their repayment gets the same suggestion and settles
the split automatically.
To use it: mark what you lent as a split with your share at ₹0 (Entries > tap it > Split
this with others > Your share 0) so it isn't counted as spending either. When the repayment
SMS arrives, opening that entry shows a suggestion — "This looks like X paying back the
₹Y you lent on <date>" — tick it and the credit is filed as a repayment while the original
is marked settled, in one step. You can also just pick "Loan repayment" from the category
list yourself at any time.

## Transfers and card bill payments
Paying a card bill or moving money between your own accounts sends two SMS: one debit, one
credit. The app pairs them (same amount, within 2 days, different accounts) and files both
as Card payments or Self transfer, which aren't counted as spending or income. "Payment
received" messages from your card issuer are read as money arriving on the card, not a spend.
Categories you set yourself are never changed by this.

## Merchant rules
One set of rules gives each merchant its name and category. Importing a FinArt backup adds its
rules to this set; correcting an entry adds or updates a rule. Settings > View and edit rules
to search, change or delete. "Undo my rule changes" goes back to the imported rules.

## Rescanning from scratch
Settings > Automatic SMS reading > tick "Start fresh", then pick a range. You choose what
to clear (SMS entries, learned names and categories, bills/balances, and optionally manual
and FinArt entries) before the SMS are read again.

## Other currencies
Pick the currency when adding an entry. The app fetches that day's rate to convert to ₹
(ECB reference rates; AED, SAR, QAR, OMR and BHD via their US-dollar peg). You can type
your own rate, for example to match your card statement. Entries without a rate are left
out of totals until converted (Settings > Currencies > Convert them now).

## Data only comes from SMS
The app no longer imports from FinArt or any other file; entries, bills, accounts, balances
and subscriptions all come from reading your own SMS. Settings > Data not from SMS shows
anything still on your phone that predates this (from an earlier import or FinArt backup)
and lets you remove it in one step. Merchant name and category rules are still built into
the app; they're software, not your data.

## How far back can it actually go?
Only as far as the SMS still on the phone. If you switched phones without restoring SMS,
or your messages app auto-deletes old messages, older entries won't be there to read.

## Backups
Settings > Download backup saves a JSON file. Restore it on a new phone, or in the
web version, to move your ledger.

## v1.40 Family Hub update

- First-run sequence is now **Language → Google account → SMS permission**.
- The previous ledger Home dashboard is now available under **Summary**.
- Home is now the **Family Hub**, with family income/expense totals, events, calendar items and shopping list.
- Events can be created directly from Home.
- Multiple-SIM handling no longer creates family members. A detected SIM is only tagged to the person who uses that SIM, and the tag can be changed in Settings.
- Shared events, calendar items and shopping items are included in the existing Family Sync payload.
- GitHub Actions builds the release APK with Gradle 8.7.

## v1.41 startup fixes
- First-run Google account step now has **Skip for now** and never blocks local ledger use.
- SMS scanning is blocked until onboarding has completed and Android reports SMS permission granted.
- SMS runtime permission is requested explicitly; the app no longer scans on resume just because a permission happens to exist.
- Dual-SIM onboarding requires a **person name for every SIM**. SIM1/SIM2 are only placeholders and are not stored as family-member names.
- The same SIM names remain editable later in Settings.
- Google sign-in failures/cancellation are shown on-screen instead of leaving the onboarding sheet stuck.


## Google Sign-In for family members

Version 1.42 supports a Google identity for each family member record. The intended model is **one Google account per person, normally signed in on that person's own phone**. Firebase Auth maintains one active Google session per app installation; it does not support several people being simultaneously signed into one phone as separate Firebase users.

### Enable Google Sign-In in Firebase

1. Open the Firebase project shown in `app/google-services.json`: **sms-ledger-family**.
2. Go to **Authentication → Sign-in method**.
3. Enable **Google** as a provider and save.
4. In **Project settings → Your apps → Android app**, confirm package name `in.vageesh.smsledger`.
5. Add the SHA-1 certificate fingerprint for every signing certificate used to build the app. The supplied release keystore uses the certificate already represented in the current `google-services.json`; if you replace the keystore or use a different GitHub Actions signing key, add that key's SHA-1 too.
6. Download the updated `google-services.json` if Firebase gives you a changed configuration, and replace `app/google-services.json`.
7. In **Authentication → Users**, you should see a user appear after a family member completes Google sign-in.

### How family members use it

1. Install the same APK on each family member's phone.
2. On first launch, choose the language.
3. Choose that person's own Google account.
4. Grant SMS permission when requested.
5. If the phone has multiple SIMs, enter the person name for each SIM.
6. Open **Settings → Family** and use the member name when sharing/syncing family data.
7. The family member's Google email/UID can be stored against that member record.

### Linking a member from Family settings

In **Settings → Family**, add a family member and use **Link Google** next to that member. This launches Google account selection and records the selected account's UID/email on that member record. Because Firebase has one active user per app installation, this action changes the current signed-in account on that phone; it is therefore recommended to perform it on the relevant family member's own phone rather than switching accounts on the family owner's phone.

### Important distinction

- **Google account** identifies the person.
- **SIM name** identifies which SIM produced an SMS transaction.
- A SIM does **not** create a Firebase/Google family member.
- Family sync can continue to use the existing nearby-device mechanism; Google identity is an identity layer, not a second SIM.

## Family Cloud / Firestore setup (v1.43)

Family Cloud uses Firebase Authentication + Cloud Firestore. Each family member signs in with their own Google account on their own phone. A family owner creates a family and shares the generated 8-character family code; another member signs in with Google and joins with that code.

### 1. Enable Cloud Firestore

In Firebase Console for the same project used by `app/google-services.json`:

1. Open **Build / Firestore Database**.
2. Click **Create database**.
3. Use the default database and choose a production-oriented mode.
4. Choose the region closest to your users.

The app's `firebase.json` points to `firestore.rules`. Deploy the included rules with Firebase CLI if desired:

```bash
firebase login
firebase use <your-project-id>
firebase deploy --only firestore:rules
```

Or paste `firestore.rules` into **Firestore Database → Rules** and Publish.

### 2. Authentication

Under **Authentication → Sign-in method**, keep **Google** enabled. The Android app already uses Firebase Authentication for Google Sign-In.

### 3. Android app configuration

Keep the `app/google-services.json` generated for this Firebase project. If you change the Firebase Android app, download a fresh file and replace it.

The Android package/application ID is:

`in.vageesh.smsledger`

### 4. Family workflow

On phone A:

1. Sign in with Google.
2. Home → Family.
3. Select **Create family**.
4. Give the family a name.
5. Share the displayed family code.

On phone B:

1. Install the same APK.
2. Sign in with the second family member's Google account.
3. Home → Family → **Join with code**.
4. Enter the family code and the member's display name.

After joining, Firestore listeners keep shared events, calendar entries, shopping items and shared transactions updated across phones.

### 5. Privacy model

The app does **not** upload the entire SMS inbox. Family Cloud publishes only the data selected for family sharing. The Family Hub contains a **Share my SMS transactions with family** toggle; when it is off, the local SMS ledger remains on the phone.

### 6. Security

The included rules require Firebase Authentication and verify family membership before allowing access to a family's shared collections. Do not replace them with Firestore "allow read, write: if true" rules in production.


## v1.44 Family Hub fixes

- Google Sign-In uses Credential Manager with a Google Play services fallback.
- SMS reading is gated by explicit in-app consent. Android permission being granted is not treated as permission to scan until the user confirms.
- Dual-SIM detection uses Android's active subscription list, not historical SMS subscription IDs. At least one SIM name is required; remaining SIMs may be left blank and receive an internal temporary label that can be renamed later.
- Family Hub sharing supports All family members or selected members for events, calendar items, shopping items, and explicitly shared transactions.
- Firestore sharing rules use `shareWith: "all"` or `audienceUids` so selected-member data is restricted at the database-rule level.
- Event creators can edit/delete their events.
- Event details show category breakdown and who spent the most.
- Shopping items remain visible when checked off instead of disappearing from the active list.
- The standalone Entries tab is removed; Home now has a Recent expenses card and a floating searchable expense window. Tapping an expense still opens the existing full edit/detail sheet.

### Firebase / Google setup

1. In Firebase Console, open the project used by `app/google-services.json`.
2. Authentication -> Sign-in method -> enable Google.
3. Authentication -> Settings / Google configuration: confirm the Web OAuth client used by the app exists.
4. Firebase Project settings -> Your apps -> Android app `in.vageesh.smsledger`: ensure the SHA-1 for the release key is registered. The included release keystore is configured by the project build; if you replace it, register the SHA-1 of your replacement key instead.
5. Download the current `google-services.json` after changing OAuth settings and replace `app/google-services.json` if Firebase provides a new file.
6. Create/enable Cloud Firestore in the same project.
7. Publish the included `firestore.rules`. With Firebase CLI: `firebase login`, `firebase use <project-id>`, `firebase deploy --only firestore:rules`.
8. Build the release APK with the included GitHub Actions workflow.
9. Install the same release build on each family member's phone. Each member signs in with their own Google account and joins the family using the family code.

### Important

The app's local SMS ledger remains local unless family transaction sharing is enabled. Shared Family Hub objects are synchronized through Firestore.

## v1.45 fixes

- Google Sign-In now uses Firebase's generated `default_web_client_id` instead of a hard-coded OAuth client ID and falls back to the legacy Google account picker when Credential Manager cannot complete the picker, including cancellation/provider failures.
- SMS-derived dashboard data is hidden until the user completes the new in-app SMS consent and an SMS import has completed. Upgrading from an earlier build forces this consent/import cycle once.
- SIM detection uses current active subscription slots (`simSlotIndex`) and de-duplicates by slot, avoiding stale historical SMS subscription IDs. Android `READ_PHONE_STATE` is requested only as part of the required access flow so active SIM slots can be identified accurately.
- Dual-SIM onboarding requires a name for at least one SIM. Other SIM names can be left blank; those SIMs receive an internal hidden placeholder and are not displayed in the Family Hub until renamed in Settings.
- The bottom navigation uses the actual number of visible tabs; removing a tab no longer leaves an empty sixth column.
- Settings blocks use content-visibility containment to reduce off-screen rendering work and improve scrolling performance.
- Recent Expenses has been removed from Home. Full expenses remain accessible through the floating expense window.
- Completed shopping items remain visible instead of disappearing from the Home shopping list.
- Event details include category totals and identify the top spender. Only the event creator can edit or delete an event.

## v1.46 changes

- Google sign-in uses the explicit Google account chooser (`GetSignInWithGoogleOption`) with the existing legacy picker as a fallback.
- After Android SMS permission is granted, the app automatically starts the initial 90-day bank-SMS import. It does not read existing SMS before permission is granted.
- Active SIM detection is based on current Android active subscription slots. Dual-SIM setup requires a name for at least one SIM; unnamed SIMs receive an internal hidden identifier and do not appear in the Family Hub until renamed.
- The Home title is now **Ledger** and the family icon is larger.
- Settings no longer contains People/Family management; family management remains on the Home/Family screen.
- Monthly budgets use a category picker. Category is optional; selecting Overall creates a single monthly budget.
- Automatic local backups can be scheduled daily or weekly; the latest 7 are retained inside the app's private storage.
- Kannada translations were expanded for the new Home, Settings, backup, budget, SMS and family UI.

### Google Sign-In Firebase requirement

In Firebase Console, enable **Authentication → Sign-in method → Google** for the Firebase project used by this app. The Android OAuth client must use package `in.vageesh.smsledger` and the SHA-1 of the release signing key. The Web OAuth client from `google-services.json` is used as the server client ID for the Google ID token.

## Google Sign-In certificate check (v1.47)

In the Android app, open **Settings → Cloud account → Show app SHA-1 / SHA-256**. Compare the displayed SHA-1 with the Android OAuth client certificate fingerprint in Firebase Console → Project settings → Your apps → Android app → SHA certificate fingerprints.

For the current release keystore shipped with this project, the release SHA-1 is:
`9C:5B:35:49:E1:2D:3D:FE:44:F1:B0:EB:8D:AC:18:FE:CF:3B:29:D5`

The package name is `in.vageesh.smsledger`. If you build with a different signing key, the SHA-1 will be different and that fingerprint must also be registered in Firebase.

## SMS import in v1.47

The first import now reads the complete SMS history available on the phone (not just 90 days). The ledger parser still converts only supported bank transaction SMS into entries; OTPs, promotional messages and unrelated SMS are ignored. Existing installations receive a one-time full-history migration after upgrading to v1.47.


## v1.48 build fix
The SHA fingerprint diagnostic no longer depends on BuildConfig and safely handles nullable SigningInfo/signatures, fixing the Kotlin release compilation errors reported by GitHub Actions.

### Google Sign-In troubleshooting (v1.51)

If the APK's in-app SHA-1 differs from Firebase, add that SHA-1 under Firebase Console -> Project settings -> Your apps -> Android app. If you changed the SHA-1 or OAuth client configuration, download a fresh `google-services.json` from Firebase and replace `app/google-services.json` before rebuilding. Firebase's current Android guidance explicitly calls for updating the Firebase config file after Google sign-in OAuth configuration changes. The app now times out Credential Manager after 15 seconds and falls back to the Google Play services account picker; Firebase authentication is also given a 15-second timeout and reports the stage/status instead of hanging indefinitely.


## v1.51 build compatibility
This release keeps Kotlin 2.0.21 and uses Firebase Android BoM 33.12.0 with the main `firebase-auth` and `firebase-firestore` modules, plus Google Identity `googleid:1.1.1`. This avoids the Kotlin metadata mismatch introduced by newer dependencies while retaining Firebase Authentication and Firestore cloud functionality.

## v1.52 changes
- Replaced `app/google-services.json` with the latest Firebase configuration supplied for `sms-ledger-family`.
- Added a pending SMS queue in `SmsReceiver`: accepted bank SMS are queued even when the WebView/activity is not ready, then imported automatically when the app is open/resumes.
- Added an in-app **Test Google Sign-In** diagnostic in Settings. It uses the Google Play Services sign-in path and reports the exact stage/error returned, together with the installed APK signing SHA-1 and Web OAuth client ID.
- Version code/name: 46 / 1.52.


## v1.54 transaction intelligence
- Correlates bank debits with biller/payment-confirmation SMS so confirmations do not count as extra income/expense.
- Classifies matched own-account transfers as internal transfers and excludes them from income/expense totals.
- Tracks standing-instruction setup and stop/cancel messages and matches subsequent recurring debits.
- Excludes promotional/non-financial SMS from ledger calculations.
- Home Total Balance includes bank, investment, FD and EPF/PF/NPS account balances and excludes credit-card limits.

## GitHub Actions build

This project includes `.github/workflows/build.yml`. Push the project to GitHub and run **Build APK** from Actions, or push to `main`/`master` to trigger the workflow automatically. The release APK is uploaded as the `smsledger-release` artifact.
