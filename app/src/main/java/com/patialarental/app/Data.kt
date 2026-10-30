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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

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

data class Pay(
    val id: Long = 0L,
    val lid: Long = 0L,
    val name: String = "",
    val phone: String = "",
    val amount: Long = 0L,
    val kind: String = "Brokerage",
    val note: String = "",
    val ts: Long = System.currentTimeMillis()
)

fun norm(p: String): String = p.filter { it.isDigit() }.takeLast(10)

class Db(ctx: Context) : SQLiteOpenHelper(ctx, "patiala.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE leads(id INTEGER PRIMARY KEY AUTOINCREMENT, phone TEXT NOT NULL, name TEXT, kind TEXT, area TEXT, budget TEXT, note TEXT, status TEXT, follow INTEGER DEFAULT 0, created INTEGER)")
        db.execSQL("CREATE TABLE calls(id INTEGER PRIMARY KEY AUTOINCREMENT, phone TEXT NOT NULL, name TEXT, ts INTEGER NOT NULL, kind INTEGER, UNIQUE(phone, ts))")
        db.execSQL("CREATE TABLE albums(id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, area TEXT, rent TEXT, created INTEGER)")
        db.execSQL("CREATE TABLE photos(id INTEGER PRIMARY KEY AUTOINCREMENT, album INTEGER, path TEXT, created INTEGER)")
        db.execSQL(CREATE_PAYS)
    }

    private val CREATE_PAYS =
        "CREATE TABLE IF NOT EXISTS pays(id INTEGER PRIMARY KEY AUTOINCREMENT, lid INTEGER, name TEXT, phone TEXT, amount INTEGER, kind TEXT, note TEXT, ts INTEGER)"

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) db.execSQL(CREATE_PAYS)
    }

    fun pays(): List<Pay> {
        val out = mutableListOf<Pay>()
        readableDatabase.rawQuery("SELECT id, lid, name, phone, amount, kind, note, ts FROM pays ORDER BY ts DESC", null).use { c ->
            while (c.moveToNext()) {
                out.add(
                    Pay(
                        c.getLong(0), c.getLong(1), c.getString(2) ?: "", c.getString(3) ?: "",
                        c.getLong(4), c.getString(5) ?: "Brokerage", c.getString(6) ?: "", c.getLong(7)
                    )
                )
            }
        }
        return out
    }

    fun addPay(p: Pay): Long {
        val cv = ContentValues()
        cv.put("lid", p.lid)
        cv.put("name", p.name)
        cv.put("phone", p.phone)
        cv.put("amount", p.amount)
        cv.put("kind", p.kind)
        cv.put("note", p.note)
        cv.put("ts", p.ts)
        return writableDatabase.insert("pays", null, cv)
    }

    fun deletePay(id: Long) {
        writableDatabase.delete("pays", "id=?", arrayOf(id.toString()))
    }

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
        sb.append("Amritpal Singh, *Amritpal Property*.\n\n")
        if (area.isNotBlank() || budget.isNotBlank()) {
            sb.append("*Available:* ")
            sb.append(area)
            if (area.isNotBlank() && budget.isNotBlank()) sb.append("  ·  ")
            sb.append(budget)
            sb.append("\n")
        }
        sb.append("Visit dekhna chahenge? Aaj shaam ka time bhi hai.\n\n— Amritpal Singh, 98159 44507")
        return sb.toString()
    }

    fun followup(name: String, area: String): String =
        head(name) + "Jis " + (if (area.isBlank()) "flat" else area) + " ke baare mein baat hui thi — koi decision socha?\n\n" +
            "Aaj hi visit fix kar dein, flat khaali hai.\n— *Amritpal Property*"

    fun rentDue(name: String, amount: String): String =
        head(name) + "Is mahine ka rent " + (if (amount.isBlank()) "" else "*₹" + amount + "* ") +
            "baaki hai ji.\n\nUPI ya cash — jaisa aapko theek lage.\n— *Amritpal Property*"

    fun visit(name: String, place: String): String =
        head(name) + "Visit confirm 🙏\n\n*" + place + "*\nAaj shaam 5 baje. Pahunchna ho to ek call kar lein.\n\n— Amritpal Singh, *Amritpal Property*"
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

object Backup {
    fun write(ctx: Context, db: Db, uri: Uri): Boolean {
        return try {
            val root = JSONObject()
            root.put("app", "Amritpal Property")
            root.put("owner", "Amritpal Singh")
            root.put("phone", "9815944507")
            root.put("exported", System.currentTimeMillis())

            val leads = JSONArray()
            for (l in db.leads()) {
                val o = JSONObject()
                o.put("id", l.id)
                o.put("phone", l.phone)
                o.put("name", l.name)
                o.put("kind", l.kind)
                o.put("area", l.area)
                o.put("budget", l.budget)
                o.put("note", l.note)
                o.put("status", l.status)
                o.put("follow", l.follow)
                leads.put(o)
            }
            root.put("leads", leads)

            val pays = JSONArray()
            for (p in db.pays()) {
                val o = JSONObject()
                o.put("id", p.id)
                o.put("lid", p.lid)
                o.put("name", p.name)
                o.put("phone", p.phone)
                o.put("amount", p.amount)
                o.put("kind", p.kind)
                o.put("note", p.note)
                o.put("ts", p.ts)
                pays.put(o)
            }
            root.put("pays", pays)

            val calls = JSONArray()
            for (c in db.calls()) {
                val o = JSONObject()
                o.put("phone", c.phone)
                o.put("name", c.name)
                o.put("ts", c.ts)
                calls.put(o)
            }
            root.put("calls", calls)

            val albums = JSONArray()
            for (a in db.albums()) {
                val o = JSONObject()
                o.put("id", a.id)
                o.put("title", a.title)
                o.put("area", a.area)
                o.put("rent", a.rent)
                albums.put(o)
            }
            root.put("albums", albums)

            val photos = JSONArray()
            for (a in db.albums()) {
                for (p in db.photos(a.id)) {
                    val o = JSONObject()
                    o.put("album", a.id)
                    o.put("path", p.path)
                    photos.put(o)
                }
            }
            root.put("photos", photos)

            val out = ctx.contentResolver.openOutputStream(uri) ?: return false
            out.use { it.write(root.toString(2).toByteArray()) }
            true
        } catch (e: Exception) {
            false
        }
    }
}

object Restore {
    fun run(ctx: Context, db: Db, uri: Uri): String {
        return try {
            val text = ctx.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                ?: return "File khuli nahi"
            val root = JSONObject(text)
            val w = db.writableDatabase
            var nl = 0
            var np = 0
            var nc = 0
            var na = 0
            var nph = 0
            w.beginTransaction()
            try {
                w.delete("pays", null, null)
                w.delete("photos", null, null)
                w.delete("albums", null, null)
                w.delete("calls", null, null)
                w.delete("leads", null, null)

                val leads = root.optJSONArray("leads")
                if (leads != null) {
                    for (i in 0 until leads.length()) {
                        val o = leads.getJSONObject(i)
                        val cv = ContentValues()
                        cv.put("id", o.optLong("id"))
                        cv.put("phone", o.optString("phone"))
                        cv.put("name", o.optString("name"))
                        cv.put("kind", o.optString("kind", "Tenant"))
                        cv.put("area", o.optString("area"))
                        cv.put("budget", o.optString("budget"))
                        cv.put("note", o.optString("note"))
                        cv.put("status", o.optString("status", "Naya"))
                        cv.put("follow", o.optLong("follow"))
                        if (w.insert("leads", null, cv) != -1L) nl++
                    }
                }

                val pays = root.optJSONArray("pays")
                if (pays != null) {
                    for (i in 0 until pays.length()) {
                        val o = pays.getJSONObject(i)
                        val cv = ContentValues()
                        cv.put("id", o.optLong("id"))
                        cv.put("lid", o.optLong("lid"))
                        cv.put("name", o.optString("name"))
                        cv.put("phone", o.optString("phone"))
                        cv.put("amount", o.optLong("amount"))
                        cv.put("kind", o.optString("kind", "Brokerage"))
                        cv.put("note", o.optString("note"))
                        cv.put("ts", o.optLong("ts"))
                        if (w.insert("pays", null, cv) != -1L) np++
                    }
                }

                val calls = root.optJSONArray("calls")
                if (calls != null) {
                    for (i in 0 until calls.length()) {
                        val o = calls.getJSONObject(i)
                        val cv = ContentValues()
                        cv.put("phone", norm(o.optString("phone")))
                        cv.put("name", o.optString("name"))
                        cv.put("ts", o.optLong("ts"))
                        cv.put("kind", 0)
                        if (w.insertWithOnConflict("calls", null, cv, SQLiteDatabase.CONFLICT_IGNORE) != -1L) nc++
                    }
                }

                val albums = root.optJSONArray("albums")
                if (albums != null) {
                    for (i in 0 until albums.length()) {
                        val o = albums.getJSONObject(i)
                        val cv = ContentValues()
                        cv.put("id", o.optLong("id"))
                        cv.put("title", o.optString("title"))
                        cv.put("area", o.optString("area"))
                        cv.put("rent", o.optString("rent"))
                        if (w.insert("albums", null, cv) != -1L) na++
                    }
                }

                val photos = root.optJSONArray("photos")
                if (photos != null) {
                    for (i in 0 until photos.length()) {
                        val o = photos.getJSONObject(i)
                        val path = o.optString("path")
                        if (path.isBlank() || !File(path).exists()) continue
                        val cv = ContentValues()
                        cv.put("album", o.optLong("album"))
                        cv.put("path", path)
                        if (w.insert("photos", null, cv) != -1L) nph++
                    }
                }

                w.setTransactionSuccessful()
            } finally {
                w.endTransaction()
            }
            "Restore ho gaya ✓  $nl leads · $np payments · $na album · $nc calls"
        } catch (e: Exception) {
            "Restore fail: " + (e.message ?: "file sahi nahi hai")
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
