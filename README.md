# Patiala Rental — Android app

Amritpal Singh ke rental/dealer kaam ke liye app. Sab kuch **phone ke andar** — koi cloud nahi, koi login nahi, koi mahine ka kharcha nahi.

## App kya karta hai

| Screen | Kaam |
|---|---|
| **Calls** | Phone ki call log se aayi calls apne aap list mein. `+ Lead` dabao → lead ban jayega. `WA` se seedha WhatsApp. |
| **Leads** | Saare leads, status (Naya → Baat hui → Visit → Deal / Lost), follow-up date. |
| **Lead kholne par** | Rate/area/budget note, WhatsApp (message pehle se likha), Call, Follow-up date + reminder, Save. |
| **Photos** | Flat ka album, gallery se photo daalo, multi-select, **WhatsApp par bhejo** (caption ready). |
| **Reminder** | Follow-up date par phone notification — "Ramesh ji ko call karna hai". |

Data phone ke andar SQLite mein rehta hai (`patiala.db`). Photos app ke private folder mein.

## APK kaise banega (free, GitHub par)

Aapko kuch install nahi karna. Sirf 5 step:

1. **github.com** par free account banao (email se).
2. Top-right `+` → **New repository**. Naam: `patiala-rental`. **Public** chuno. `Create repository`.
3. Repo ke andar **"uploading an existing file"** link dabao. Is folder ki **saari files aur folders** drag karo —
   `app`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `.github`, `README.md`.
   Neeche **Commit changes** dabao.

   > `.github` folder upload mein chhup jaaye (kyunki naam dot se shuru hota hai) to:
   > repo mein **Add file → Create new file** → naam likho `.github/workflows/build.yml`
   > → isi repo wali `build.yml` ki copy-paste kar do → Commit.
   > **Ye file zaroori hai** — isi se APK banta hai.
4. Upar **Actions** tab kholo. Left mein **Build APK** → right mein **Run workflow** → green button.
5. 6–8 minute ruko, phir usi run ke neeche **Artifacts → patiala-rental-apk** download karo.
   Zip khologe to andar `app-debug.apk` milega.

## Phone par install

1. APK phone mein bhejo (WhatsApp/Drive/USB).
2. APK par tap → "Unknown source se install" allow karo → **Install**.
3. App kholo → **Allow** karo: Call logs, Contacts, Notifications.
4. Bas — Calls screen par aapki calls dikhne lagengi.

> Pehli baar call list khaali lage to app band karke dobara kholo — call log se import ho jayegi.

## Do sach (jo app nahi kar sakta)

- **WhatsApp apne aap message nahi bhejta.** App WhatsApp kholta hai, message pehle se likha hota hai — aap sirf **Send** dabate ho.
- **Call log permission** Android 10+ par restricted hai. Sideloaded app mein ek baar Allow karna padta hai. Agar phone rok de, to Calls screen par note dikhega — tab manual `+ Naya` lead se kaam chalta rahega.

## Project files

```
app/src/main/java/com/patialarental/app/
  MainActivity.kt   - app entry
  Data.kt           - database, call-log import, WhatsApp, notifications
  Ui.kt             - colours + clay components
  Screens.kt        - Calls / Leads / Photos + tab bar
  Detail.kt         - Lead detail, Album detail, photo grid
.github/workflows/build.yml  - APK build
```

## Aage kya add kar sakte hain

- Backup file (JSON) WhatsApp/Drive par
- Rent ledger (kaun kitna de raha hai)
- Cloud sync (agar staff ka doosra phone ho)
- Play Store par daalna (uske liye call-log permission ka policy case banana padega)
