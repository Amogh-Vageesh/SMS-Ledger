# SMS Ledger for Android

Reads bank transaction SMS automatically and builds your monthly expense ledger.
Everything stays on the phone: no server, no account, no analytics.

## How it works
- First launch asks for SMS access, then imports bank SMS from the last 90 days.
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

## Backups
Settings > Download backup saves a JSON file. Restore it on a new phone, or in the
web version, to move your ledger.
