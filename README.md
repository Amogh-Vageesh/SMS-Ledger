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

## Home
Income vs expenses by month (amounts in lakh on each bar), a category donut, and your top
5 income sources and expense categories, for the last 3, 6 or 12 months. Tap a month to see
its numbers. Dark or light follows your phone's setting.

## Family
Settings > Family: add your name, then tap "Sync with family nearby" on both phones while
they're close together. Check the 4-digit code matches on both, and the two ledgers swap
over Bluetooth or direct Wi-Fi (no internet needed). Home then has a "Just me / Family"
switch. Sync again whenever you're together to refresh.

## Getting the right bank for each entry
Bank names are read from the message text, which can occasionally mention a different bank
as a third party (a loan lender, a NEFT sender) rather than the bank that actually sent the
SMS. Android also passes along the sender code (like "AD-HDFCBK"), and the app now prefers
that when it recognises it, which fixes most of these mix-ups. If an account still looks
wrong, open it under Accounts and tap Remove; it comes back on its own if a real SMS for it
arrives.

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
