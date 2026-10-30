# Amritpal Property — Android app

Amritpal Singh (Patiala) ke property/rental kaam ka app. Sab kuch **phone ke andar** — koi cloud nahi, koi login nahi, koi mahine ka kharcha nahi.

Owner: **Amritpal Singh** · **98159 44507** · Patiala

## App kya karta hai

| Screen | Kaam |
|---|---|
| **Calls** | Phone ki call log se aayi calls apne aap list mein. `+ Lead` → lead ban jayegi. `WA` → seedha WhatsApp. |
| **Leads** | Saare leads, status (Naya → Baat hui → Visit → Deal / Lost), follow-up date + reminder. |
| **Lead detail** | Area/budget/note, WhatsApp templates, Call, follow-up, delete. |
| **Photos** | Flat ka album, gallery se photo, multi-select, **WhatsApp par bhejo** caption ke saath. |
| **Paisa** | **Is mahine ki kamai**, pichhle mahine se comparison (↑↓ %), **6 mahine ka line chart**, rent ledger (kaun kitna de raha hai), customer-wise payments, **backup file**. |

## Paisa screen kaise use karo

- `+ Payment` dabao → customer ka naam, amount, aur type chuno:
  - **Brokerage** = aapki kamai (chart aur "is mahine ki kamai" mein judta hai)
  - **Rent** = tenant ka rent (rent ledger mein dikhta hai)
  - **Advance** / **Other**
- Chart pichhle 6 mahine ki brokerage kamai dikhata hai — jitna data daaloge, utni upar-neeche line banegi.
- **Rent ledger**: jo leads `Tenant` hain aur jinke budget mein rent likha hai, unka is mahine ka status — `Aaya` / `Aadha` / `Baaki`.
- **Backup file banao** → JSON file save hoti hai (saari leads, payments, albums, calls). Ise WhatsApp/Drive par rakh lo.

## APK build (GitHub par)

Repo: `github.com/lalli123e-sys/patialarental`

1. **Actions** tab → **Build APK** → **Run workflow**
2. 3–5 minute
3. Run ke neeche **Artifacts → patiala-rental-apk** → `app-debug.apk`

## Phone par install

1. APK phone mein bhejo → tap → "Unknown source" allow → Install
2. App kholo → **Call logs, Contacts, Notifications** allow karo

> Naya version install karne par purana data safe rehta hai (database upgrade ho jata hai).

## Do sach

- **WhatsApp apne aap message nahi bhejta** — app chat kholta hai, message pehle se likha hota hai, aap Send dabate ho.
- **Call log permission** Android 10+ par restricted hai — sideloaded app mein ek baar Allow karna padta hai.

## Files

```
app/src/main/java/com/patialarental/app/
  MainActivity.kt   - entry
  Data.kt           - DB, call-log import, WhatsApp, notifications, backup
  Ui.kt             - colours + clay components
  Screens.kt        - Calls / Leads / Photos / Paisa tabs
  Detail.kt         - Lead detail, Album detail
  Paisa.kt          - kamai chart, rent ledger, payments, backup
.github/workflows/build.yml
```
