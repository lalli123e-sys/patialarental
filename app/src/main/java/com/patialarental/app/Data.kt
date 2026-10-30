package com.patialarental.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.net.Uri
import android.os.Build
import android.provider.CallLog
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

data class CallRow(val id: Long, val phone: String, val name: String, val ts: Long)

data class Lead(
    val id: Long = 0L,
    val phone: String,
    val name: String = "",
    val kind: String = "Tenant",
    val area: String = "",
    val budget: String = "",
    val note: String = "",
    val status: String = "Naya",
    val follow: Long = 0L
)

data class Album(val id: Long, val title: String, val area: String, val rent: String)
data class Photo(val id: Long, val path: String)

fun norm(p: String): String = p.filter { it.isDigit() }.takeLast(10)

class Db(ctx: Context) : SQLiteOpenHelper(ctx, "patiala.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE leads(id INTEGER PRIMARY KEY AUTOINCREMENT, phone TEXT NOT NULL, name TEXT, kind TEXT, area TEXT, budget TEXT, note TEXT, status TEXT, follow INTEGER DEFAULT 0, created INTEGER)")
        db.execSQL("CREATE TABLE calls(id INTEGER PRIMARY KEY AUTOINCREMENT, phone TEXT NOT NULL, name TEXT, ts INTEGER NOT NULL, kind INTEGER, UNIQUE(phone, ts))")
        db.execSQL("CREATE TABLE albums(id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, area TEXT, rent TEXT, created INTEGER)")
        db.execSQL("CREATE TABLE photos(id INTEGER PRIMARY KEY AUTOINCREMENT, album INTEGER, path TEXT, created INTEGER)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun calls(): List<CallRow> {
        val out = mutableListOf<CallRow>()
        readableDatabase.rawQuery("SELECT id, phone, name, ts FROM calls ORDER BY ts DESC LIMIT 400", null).use { c ->
            while (c.moveToNext()) {
                out.add(CallRow(c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "", c.getLong(3)))
            }
        }
        return out
    }

    fun addCall(phone: String, name: String, ts: Long, kind: Int): Boolean {
        val cv = ContentValues()
        cv.put("phone", norm(phone))
        cv.put("name", name)
        cv.put("ts", ts)
        cv.put("kind", kind)
        return writableDatabase.insertWithOnConflict("calls", null, cv, SQLiteDatabase.CONFLICT_IGNORE) != -1L
    }

    fun leads(): List<Lead> {
        val out = mutableListOf<Lead>()
        readableDatabase.rawQuery(
            "SELECT id, phone, name, kind, area, budget, note, status, follow FROM leads " +
                "ORDER BY (status='Deal' OR status='Lost'), follow=0, follow, id DESC", null
        ).use { c ->
            while (c.moveToNext()) out.add(readLead(c))
        }
        return out
    }

    fun lead(id: Long): Lead? {
        readableDatabase.rawQuery(
            "SELECT id, phone, name, kind, area, budget, note, status, follow FROM leads WHERE id=? LIMIT 1",
            arrayOf(id.toString())
        ).use { c ->
            if (c.moveToFirst()) return readLead(c)
        }
        return null
    }

    private fun readLead(c: Cursor) = Lead(
        c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "", c.getString(3) ?: "Tenant",
        c.getString(4) ?: "", c.getString(5) ?: "", c.getString(6) ?: "", c.getString(7) ?: "Naya", c.getLong(8)
    )

    fun saveLead(l: Lead): Long {
        val cv = ContentValues()
        cv.put("phone", l.phone)
        cv.put("name", l.name)
        cv.put("kind", l.kind)
        cv.put("area", l.area)
        cv.put("budget", l.budget)
        cv.put("note", l.note)
        cv.put("status", l.status)
        cv.put("follow", l.follow)
        if (l.id == 0L) {
            cv.put("created", System.currentTimeMillis())
            return writableDatabase.insert("leads", null, cv)
        }
        writableDatabase.update("leads", cv, "id=?", arrayOf(l.id.toString()))
        return l.id
    }

    fun deleteLead(id: Long) {
        writableDatabase.delete("leads", "id=?", arrayOf(id.toString()))
    }

    fun albums(): List<Album> {
        val out = mutableListOf<Album>()
        readableDatabase.rawQuery("SELECT id, title, area, rent FROM albums ORDER BY id DESC", null).use { c ->
            while (c.moveToNext()) out.add(Album(c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "", c.getString(3) ?: ""))
        }
        return out
    }

    fun addAlbum(title: String, area: String, rent: String): Long {
        val cv = ContentValues()
        cv.put("title", title)
        cv.put("area", area)
        cv.put("rent", rent)
        cv.put("created", System.currentTimeMillis())
        return writableDatabase.insert("albums", null, cv)
    }

    fun deleteAlbum(id: Long) {
        writableDatabase.delete("photos", "album=?", arrayOf(id.toString()))
        writableDatabase.delete("albums", "id=?", arrayOf(id.toString()))
    }

    fun photos(album: Long): List<Photo> {
        val out = mutableListOf<Photo>()
        readableDatabase.rawQuery("SELECT id, path FROM photos WHERE album=? ORDER BY id", arrayOf(album.toString())).use { c ->
            while (c.moveToNext()) out.add(Photo(c.getLong(0), c.getString(1) ?: ""))
        }
        return out
    }

    fun countPhotos(album: Long): Int {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM photos WHERE album=?", arrayOf(album.toString())).use { c ->
            if (c.moveToFirst()) return c.getInt(0)
        }
        return 0
    }

    fun addPhoto(album: Long, path: String) {
        val cv = ContentValues()
        cv.put("album", album)
        cv.put("path", path)
        cv.put("created", System.currentTimeMillis())
        writableDatabase.insert("photos", null, cv)
    }

    fun deletePhoto(id: Long) {
        writableDatabase.delete("photos", "id=?", arrayOf(id.toString()))
    }
}

object CallSync {

    fun granted(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED

    fun import(ctx: Context): Int {
        if (!granted(ctx)) return 0
        val prefs = ctx.getSharedPreferences("pr", Context.MODE_PRIVATE)
        val since = prefs.getLong("lastTs", System.currentTimeMillis() - 4L * 24 * 3600 * 1000)
        var maxTs = since
        var added = 0
        val db = Db(ctx)
        try {
            val proj = arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.DATE, CallLog.Calls.TYPE)
            ctx.contentResolver.query(
                CallLog.Calls.CONTENT_URI, proj, "${CallLog.Calls.DATE} > ?", arrayOf(since.toString()),
                "${CallLog.Calls.DATE} ASC"
            )?.use { c ->
                while (c.moveToNext()) {
                    val num = c.getString(0) ?: continue
                    if (num.isBlank()) continue
                    val ts = c.getLong(2)
                    if (ts > maxTs) maxTs = ts
                    var name = c.getString(1) ?: ""
                    if (name.isBlank()) name = contactName(ctx, num)
                    if (db.addCall(num, name, ts, c.getInt(3))) added++
                }
            }
        } catch (e: Exception) {
            // permission or provider issue - ignore
        } finally {
            db.close()
        }
        prefs.edit().putLong("lastTs", maxTs).apply()
        return added
    }

    private fun contactName(ctx: Context, number: String): String {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return ""
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            ctx.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) ?: "" else ""
            } ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}

object Msg {
    private fun head(name: String): String = "*" + (if (name.isBlank()) "Ji" else name) + " ji*, Sat Sri Akal 🙏\n"

    fun intro(name: String, area: String, budget: String): String {
        val sb = StringBuilder(head(name))
        sb.append("Amritpal Singh, *Patiala Rental*.\n\n")
        if (area.isNotBlank() || budget.isNotBlank()) {
            sb.append("*Available:* ")
            sb.append(area)
            if (area.isNotBlank() && budget.isNotBlank()) sb.append("  ·  ")
            sb.append(budget)
            sb.append("\n")
        }
        sb.append("Visit dekhna chahenge? Aaj shaam ka time bhi hai.\n\n— Amritpal, 98765 43210")
        return sb.toString()
    }

    fun followup(name: String, area: String): String =
        head(name) + "Jis " + (if (area.isBlank()) "flat" else area) + " ke baare mein baat hui thi — koi decision socha?\n\n" +
            "Aaj hi visit fix kar dein, flat khaali hai.\n— *Patiala Rental*"

    fun rentDue(name: String, amount: String): String =
        head(name) + "Is mahine ka rent " + (if (amount.isBlank()) "" else "*₹" + amount + "* ") +
            "baaki hai ji.\n\nUPI ya cash — jaisa aapko theek lage.\n— *Patiala Rental*"

    fun visit(name: String, place: String): String =
        head(name) + "Visit confirm 🙏\n\n*" + place + "*\nAaj shaam 5 baje. Pahunchna ho to ek call kar lein.\n\n— Amritpal, *Patiala Rental*"
}

object Wa {
    fun chat(ctx: Context, phone: String, text: String) {
        val n = norm(phone)
        val url = "https://wa.me/91" + n + "?text=" + Uri.encode(text)
        try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            try {
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/91" + n)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e2: Exception) {
            }
        }
    }

    fun dial(ctx: Context, phone: String) {
        try {
            ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(phone))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
        }
    }

    fun shareImages(ctx: Context, uris: List<Uri>, text: String) {
        if (uris.isEmpty()) return
        val i = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0])
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
        i.type = "image/*"
        i.putExtra(Intent.EXTRA_TEXT, text)
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            ctx.startActivity(Intent.createChooser(i, "Photo kahan bhejein?").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
        }
    }
}

object Notify {
    const val CH = "pr"

    fun channel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CH) == null) {
            nm.createNotificationChannel(NotificationChannel(CH, "Yaad aur calls", NotificationManager.IMPORTANCE_HIGH))
        }
    }

    fun show(ctx: Context, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        channel(ctx)
        val pi = PendingIntent.getActivity(
            ctx, id, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(ctx, CH)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        ctx.getSystemService(NotificationManager::class.java).notify(id, n)
    }
}

object Reminders {
    fun set(ctx: Context, id: Long, at: Long, title: String, text: String) {
        Notify.channel(ctx)
        val am = ctx.getSystemService(android.app.AlarmManager::class.java)
        val i = Intent(ctx, ReminderReceiver::class.java)
            .putExtra("title", title)
            .putExtra("text", text)
        val pi = PendingIntent.getBroadcast(
            ctx, id.toInt(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            am.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, at, pi)
        } catch (e: Exception) {
        }
    }

    fun cancel(ctx: Context, id: Long) {
        val am = ctx.getSystemService(android.app.AlarmManager::class.java)
        val i = Intent(ctx, ReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            ctx, id.toInt(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(pi)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Follow-up"
        val text = intent.getStringExtra("text") ?: "Kisi lead ko call karna hai."
        Notify.show(ctx, 11, title, text)
    }
}

class CallReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.getStringExtra(TelephonyManager.EXTRA_STATE) != TelephonyManager.EXTRA_STATE_IDLE) return
        val app = ctx.applicationContext
        val pending = goAsync()
        Thread {
            var n = 0
            try {
                n = CallSync.import(app)
            } catch (e: Exception) {
            }
            if (n > 0) Notify.show(app, 12, "Call record ho gayi", "Patiala Rental kholo — nayi call lead mein badal do.")
            pending.finish()
        }.start()
    }
}
