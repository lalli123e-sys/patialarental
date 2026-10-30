package com.patialarental.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun Root() {
    val ctx = LocalContext.current
    val db = remember { Db(ctx.applicationContext) }
    var tab by remember { mutableStateOf(0) }
    var calls by remember { mutableStateOf<List<CallRow>>(emptyList()) }
    var leads by remember { mutableStateOf<List<Lead>>(emptyList()) }
    var albums by remember { mutableStateOf<List<Album>>(emptyList()) }
    var leadId by remember { mutableStateOf(0L) }
    var albumId by remember { mutableStateOf(0L) }
    var bump by remember { mutableStateOf(0) }

    fun load() {
        calls = db.calls()
        leads = db.leads()
        albums = db.albums()
    }

    val perms = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        bump++
    }

    LaunchedEffect(bump) {
        withContext(Dispatchers.IO) { CallSync.import(ctx) }
        load()
    }

    LaunchedEffect(Unit) {
        val ask = ArrayList<String>()
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            ask.add(Manifest.permission.READ_CALL_LOG)
        }
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ask.add(Manifest.permission.READ_CONTACTS)
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ask.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (ask.isNotEmpty()) perms.launch(ask.toTypedArray())
    }

    when {
        leadId != 0L -> LeadScreen(db, leadId) {
            leadId = 0L
            bump++
        }
        albumId != 0L -> AlbumScreen(db, albumId) {
            albumId = 0L
            bump++
        }
        else -> MainTabs(
            ctx, db, tab, { tab = it }, calls, leads, albums,
            onLead = { leadId = it },
            onAlbum = { albumId = it },
            onChanged = { bump++ }
        )
    }
}

@Composable
fun MainTabs(
    ctx: Context,
    db: Db,
    tab: Int,
    onTab: (Int) -> Unit,
    calls: List<CallRow>,
    leads: List<Lead>,
    albums: List<Album>,
    onLead: (Long) -> Unit,
    onAlbum: (Long) -> Unit,
    onChanged: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(Cream).statusBarsPadding()) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> CallsScreen(calls, leads, db, ctx, onLead)
                1 -> LeadsScreen(leads, db, onLead, onChanged)
                else -> AlbumsScreen(albums, db, onAlbum, onChanged)
            }
        }
        TabBar(tab, onTab)
    }
}

@Composable
fun TabBar(sel: Int, onSel: (Int) -> Unit) {
    val items = listOf("Calls", "Leads", "Photos")
    Row(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(White)
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        items.forEachIndexed { i, s ->
            val on = i == sel
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (on) Ambers else Color.Transparent)
                    .clickable { onSel(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    s,
                    color = if (on) Ink else Ink3,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun CallsScreen(calls: List<CallRow>, leads: List<Lead>, db: Db, ctx: Context, onLead: (Long) -> Unit) {
    val newCount = calls.count { c -> leads.none { it.phone == norm(c.phone) } }
    Column(Modifier.fillMaxSize()) {
        ScreenTitle("Aaj ki calls", calls.size.toString() + " calls  ·  " + newCount + " naye lead")
        LazyColumn(Modifier.fillMaxSize()) {
            items(calls, key = { it.id }) { c ->
                val lead = leads.firstOrNull { it.phone == norm(c.phone) }
                Clay(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(c.name.ifBlank { c.phone }, avatarBg(c.phone), avatarFg(c.phone))
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(
                                if (c.name.isBlank()) c.phone else c.name,
                                color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                            )
                            Text(c.phone + "  ·  " + timeStr(c.ts), color = Ink3, fontSize = 11.sp)
                        }
                        if (lead == null) {
                            Pill("+ Lead", Ambers, Amber, small = true, onClick = {
                                val id = db.saveLead(Lead(phone = norm(c.phone), name = c.name))
                                onLead(id)
                            })
                        } else {
                            Pill(lead.status, Lavs, Lav, small = true, onClick = { onLead(lead.id) })
                        }
                        Spacer(Modifier.width(6.dp))
                        Pill("WA", Greens, Green, small = true, onClick = {
                            Wa.chat(ctx, c.phone, Msg.intro(c.name, "", ""))
                        })
                    }
                }
            }
            if (calls.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Abhi koi call nahi mili", color = Ink2, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Phone permission do — call list yahan apne aap aa jayegi.",
                            color = Ink3, fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LeadsScreen(leads: List<Lead>, db: Db, onLead: (Long) -> Unit, onChanged: () -> Unit) {
    var show by remember { mutableStateOf(false) }
    val deals = leads.count { it.status == "Deal" }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Leads", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(leads.size.toString() + " total  ·  " + deals + " deal", color = Ink2, fontSize = 13.sp)
            }
            Pill("+ Naya", Ambers, Amber, onClick = { show = true })
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(leads, key = { it.id }) { l ->
                LeadRow(l) { onLead(l.id) }
            }
            if (leads.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Koi lead nahi", color = Ink2, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("Calls se + Lead dabao, ya + Naya se add karo.", color = Ink3, fontSize = 12.sp)
                    }
                }
            }
        }
    }
    if (show) {
        NewLeadDialog(
            onClose = { show = false },
            onSave = { n, ph, ar, bu ->
                val id = db.saveLead(Lead(phone = norm(ph), name = n, area = ar, budget = bu))
                show = false
                onChanged()
                onLead(id)
            }
        )
    }
}

@Composable
fun NewLeadDialog(onClose: () -> Unit, onSave: (String, String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var budget by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Naya lead") },
        text = {
            Column {
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it },
                    label = { Text("Mobile number") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Naam") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = area, onValueChange = { area = it },
                    label = { Text("Area (jaise Model Town)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = budget, onValueChange = { budget = it },
                    label = { Text("Budget (₹12,000)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (phone.isNotBlank()) onSave(name, phone, area, budget) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("Cancel") }
        }
    )
}

@Composable
fun AlbumsScreen(albums: List<Album>, db: Db, onAlbum: (Long) -> Unit, onChanged: () -> Unit) {
    var show by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Photos", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(albums.size.toString() + " album", color = Ink2, fontSize = 13.sp)
            }
            Pill("+ Album", Ambers, Amber, onClick = { show = true })
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(albums, key = { it.id }) { a ->
                Clay(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
                    onClick = { onAlbum(a.id) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .width(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.verticalGradient(listOf(Color(0xFFFFD6AC), Color(0xFFB0C8EC))))
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("3D", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(a.title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                a.area + "  ·  " + a.rent + "  ·  " + db.countPhotos(a.id) + " photo",
                                color = Ink3, fontSize = 12.sp
                            )
                        }
                        Pill("Kholo", Teals, Teal, small = true)
                    }
                }
            }
            if (albums.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Koi album nahi", color = Ink2, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("+ Album dabao, phir flat ki photo daalo.", color = Ink3, fontSize = 12.sp)
                    }
                }
            }
        }
    }
    if (show) {
        NewAlbumDialog(
            onClose = { show = false },
            onSave = { t, ar, rn ->
                val id = db.addAlbum(t, ar, rn)
                show = false
                onChanged()
                onAlbum(id)
            }
        )
    }
}

@Composable
fun NewAlbumDialog(onClose: () -> Unit, onSave: (String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var rent by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Naya album") },
        text = {
            Column {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Title (2BHK Model Town)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = area, onValueChange = { area = it },
                    label = { Text("Area") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rent, onValueChange = { rent = it },
                    label = { Text("Rent (₹12,000)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (title.isNotBlank()) onSave(title, area, rent) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("Cancel") }
        }
    )
}
