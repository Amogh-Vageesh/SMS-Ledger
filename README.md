# SMS Ledger – APK builder (v1.154 design update)

This repo builds a new SMS Ledger APK on GitHub Actions **without the full Android source**.
It takes the released v1.153 APK in `base/`, replaces only the app's screen file
(`assets/index.html`) with `app/index.html`, raises the version to **1.154 (code 148)**,
then aligns and signs it with **your own release key**.

```
base/app-release.apk          ← v1.153 as released (do not edit)
app/index.html                ← the redesigned UI (edit this for future UI changes)
scripts/repack.py             ← swaps the UI file + bumps version
.github/workflows/build-apk.yml
```

## Why your own key matters
v1.153 is signed by the certificate **CN=SMS Ledger, O=Personal, C=IN**
(SHA-1 `9C:5B:35:49:E1:2D:3D:FE:44:F1:B0:EB:8D:AC:18:FE:CF:3B:29:D5`).
Android only installs an update if it is signed by the same key, and Google Sign-In
(Firebase) is registered against that SHA-1. The workflow checks this and **stops** if
the key doesn't match, so you can never produce an APK that would force an uninstall.

## One-time setup (about 5 minutes)

1. **Create a new private GitHub repo** (e.g. `sms-ledger-builder`) and upload everything
   in this zip, keeping the folders. The hidden `.github` folder is needed.
   *Easiest:* on the new repo page choose **"uploading an existing file"** and drag in
   the unzipped folder contents. If `.github` doesn't upload from the browser, create the
   file manually: **Add file → Create new file**, name it
   `.github/workflows/build-apk.yml` and paste the contents.

2. **Turn your keystore into text** (the `.jks` / `.keystore` file you used to sign v1.153):
   - Windows (PowerShell):
     `[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\release.jks")) | Set-Clipboard`
   - macOS: `base64 -i release.jks | pbcopy`
   - Linux: `base64 -w0 release.jks`

3. In the repo: **Settings → Secrets and variables → Actions → New repository secret**,
   add these four:

   | Secret name | Value |
   |---|---|
   | `SIGNING_KEYSTORE_BASE64` | the text from step 2 |
   | `SIGNING_STORE_PASSWORD` | keystore password |
   | `SIGNING_KEY_ALIAS` | key alias (e.g. `smsledger`) |
   | `SIGNING_KEY_PASSWORD` | key password (often same as store password) |

   These are the same values your original build workflow uses. GitHub never shows a secret
   again after saving, so take them from wherever you keep the keystore, not from the old repo.

## Build
- **Actions → Build SMS Ledger APK → Run workflow** (or just push a change to `app/index.html`).
- Takes ~1–2 minutes. When green, open **Releases** (right side of the repo page) and
  download `sms-ledger-v1.154.apk` on your phone, then install. It updates over v1.153;
  your data stays.

## Future UI changes
Replace `app/index.html` and push. To bump the version again, run the workflow manually and
enter e.g. version code `149` and name `1.155`. The name must keep the same number of
characters as the base (`1.153` → `1.155` is fine; `1.15` is not).

For changes to the native Android parts (SMS reading, permissions, Firebase, notifications),
use your original Android project. This builder only changes the screens.

## If something fails
- **"Add the SIGNING_… secrets"** → step 3 not done.
- **"This keystore is not the one that signed v1.153"** → wrong keystore or alias.
- **Lost the keystore entirely?** Run the workflow with *allow_new_key* ticked. You'll get a
  `…-NEWKEY.apk` that only installs after uninstalling the old app (export a backup from
  Settings → Your data first), and you must add its SHA-1 in Firebase for Google Sign-In.
