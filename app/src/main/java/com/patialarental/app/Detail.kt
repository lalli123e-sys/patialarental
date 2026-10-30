package com.patialarental.app

import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

fun pickDate(ctx: Context, current: Long, onPick: (Long) -> Unit) {
    val c = Calendar.getInstance()
    if (current > 0L) c.timeInMillis = current
    DatePickerDialog(
        ctx,
        { _, y, m, d ->
            val cal = Calendar.getInstance()
            cal.set(y, m, d, 11, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            onPick(cal.timeInMillis)
        },
        c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
    ).show()
}

fun copyPhoto(ctx: Context, uri: Uri): String? {
    return try {
        val dir = File(ctx.filesDir, "photos")
        if (!dir.exists()) dir.mkdirs()
        val f = File(dir, "p" + System.currentTimeMillis() + "_" + (0..999).random() + ".jpg")
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            f.outputStream().use { out -> input.copyTo(out) }
        }
        f.absolutePath
    } catch (e: Exception) {
        null
    }
}

@Composable
fun Thumb(path: String, mod: Modifier = Modifier) {
    val bmp by produceState<ImageBitmap?>(null, path) {
        value = withContext(Dispatchers.IO) {
            try {
                val o = android.graphics.BitmapFactory.Options()
                o.inSampleSize = 4
                android.graphics.BitmapFactory.decodeFile(path, o)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }
    Box(mod.background(Cream2)) {
        val b = bmp
        if (b != null) {
            Image(bitmap = b, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
}

@Composable
fun LeadScreen(db: Db, id: Long, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val base = remember { db.lead(id) }

    if (base == null) {
        Column(
            Modifier.fillMaxSize().background(Cream).statusBarsPadding().padding(24.dp)
        ) {
            Text("Lead nahi mila", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Pill("Wapas", Ambers, Amber, onClick = onBack)
        }
        return
    }

    var name by remember { mutableStateOf(base.name) }
    var area by remember { mutableStateOf(base.area) }
    var budget by remember { mutableStateOf(base.budget) }
    var note by remember { mutableStateOf(base.note) }
    var status by remember { mutableStateOf(base.status) }
    var follow by remember { mutableStateOf(base.follow) }

    fun save() {
        db.saveLead(Lead(id, base.phone, name, base.kind, area, budget, note, status, follow))
        if (follow > System.currentTimeMillis()) {
            Reminders.set(
                ctx, id, follow,
                "Follow-up: " + (if (name.isBlank()) base.phone else name),
                "Call karo — " + (if (area.isBlank()) "flat" else area) + " ke baare mein baat karni hai."
            )
        } else {
            Reminders.cancel(ctx, id)
        }
        onBack()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Cream)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill("← Wapas", White, Ink2, onClick = onBack)
            Spacer(Modifier.weight(1f))
            Pill(base.kind, Lavs, Lav, small = true)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            if (name.isBlank()) base.phone else name,
            color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold
        )
        Text(base.phone, color = Ink2, fontSize = 14.sp)
        Spacer(Modifier.height(14.dp))

        Clay(Modifier.fillMaxWidth()) {
            Cap("Status")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Naya", "Baat hui", "Visit", "Deal", "Lost").forEach { s ->
                    val on = s == status
                    Pill(
                        s,
                        if (on) Amber else Cream2,
                        if (on) Color.White else Ink2,
                        small = true,
                        onClick = { status = s }
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        Clay(Modifier.fillMaxWidth()) {
            Cap("Zaroorat")
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = area, onValueChange = { area = it },
                label = { Text("Area") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = budget, onValueChange = { budget = it },
                label = { Text("Budget") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Naam") }, singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(10.dp))

        Clay(Modifier.fillMaxWidth()) {
            Cap("Follow-up")
            Spacer(Modifier.height(6.dp))
            Text(dateStr(follow), color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Date chuno", Ambers, Amber, onClick = {
                    pickDate(ctx, follow) { follow = it }
                })
                if (follow > 0L) {
                    Pill("Hatao", Cream2, Ink2, onClick = { follow = 0L; Reminders.cancel(ctx, id) })
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) {
                BigButton("WhatsApp") {
                    Wa.chat(ctx, base.phone, Msg.intro(name, area, budget))
                }
            }
            Box(Modifier.weight(1f)) {
                BigButton("Call karo", bg1 = Ink2, bg2 = Ink) {
                    Wa.dial(ctx, base.phone)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) {
                BigButton("Follow-up bhejo", bg1 = Color(0xFFF7C071), bg2 = Amber) {
                    Wa.chat(ctx, base.phone, Msg.followup(name, area))
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        Clay(Modifier.fillMaxWidth()) {
            Cap("Note")
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text("Kuch bhi likho — 2BHK, 12 hazaar, visit kal") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(14.dp))

        BigButton("Save karo") { save() }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Pill("Lead delete karo", Color(0xFFF7E4E0), Color(0xFFB0564A), onClick = {
                db.deleteLead(id)
                Reminders.cancel(ctx, id)
                onBack()
            })
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
fun AlbumScreen(db: Db, id: Long, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val album = remember { db.albums().firstOrNull { it.id == id } }
    var photos by remember { mutableStateOf(db.photos(id)) }
    var sel by remember { mutableStateOf<Set<Long>>(emptySet()) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { u ->
                val p = copyPhoto(ctx, u)
                if (p != null) db.addPhoto(id, p)
            }
            photos = db.photos(id)
        }
    }

    if (album == null) {
        Column(Modifier.fillMaxSize().background(Cream).statusBarsPadding().padding(24.dp)) {
            Text("Album nahi mila", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Pill("Wapas", Ambers, Amber, onClick = onBack)
        }
        return
    }

    val caption = album.title +
        (if (album.area.isBlank()) "" else ", " + album.area) +
        (if (album.rent.isBlank()) "" else " — " + album.rent) +
        "\n— Amritpal Singh, Patiala Rental"

    Column(Modifier.fillMaxSize().background(Cream).statusBarsPadding().navigationBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Pill("← Wapas", White, Ink2, onClick = onBack)
            Spacer(Modifier.weight(1f))
            Pill(db.countPhotos(id).toString() + " photo", Teals, Teal, small = true)
        }
        Text(album.title, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
        Text(
            album.area + "  ·  " + album.rent,
            color = Ink2, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(10.dp))
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("+ Photo daalo", Ambers, Amber, onClick = { picker.launch(arrayOf("image/*")) })
            if (photos.isNotEmpty()) {
                Pill(sel.size.toString() + " chuni", Teals, Teal, onClick = {
                    sel = if (sel.size == photos.size) emptySet() else photos.map { it.id }.toSet()
                })
            }
        }
        Spacer(Modifier.height(10.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(photos, key = { it.id }) { p ->
                val on = sel.contains(p.id)
                Box(
                    Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            sel = if (on) sel - p.id else sel + p.id
                        }
                ) {
                    Thumb(p.path, Modifier.fillMaxSize())
                    if (on) {
                        Box(Modifier.fillMaxSize().background(Color(0x40169E8A)))
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp)
                                .clip(RoundedCornerShape(50)).background(Teal),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Column(Modifier.padding(16.dp)) {
            BigButton(
                if (sel.isEmpty()) "Photo chuno, phir WhatsApp" else "WhatsApp par bhejo"
            ) {
                if (sel.isNotEmpty()) {
                    val uris = photos.filter { sel.contains(it.id) }.mapNotNull { p ->
                        try {
                            FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", File(p.path))
                        } catch (e: Exception) {
                            null
                        }
                    }
                    Wa.shareImages(ctx, uris, caption)
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Pill("Album delete", Color(0xFFF7E4E0), Color(0xFFB0564A), onClick = {
                    db.deleteAlbum(id)
                    onBack()
                })
            }
        }
    }
}
