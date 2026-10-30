package com.patialarental.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

fun monthKey(ts: Long): String = SimpleDateFormat("yyyy-MM", Locale.US).format(Date(ts))

fun lastMonths(n: Int): List<Pair<String, String>> {
    val out = mutableListOf<Pair<String, String>>()
    val c = Calendar.getInstance()
    c.set(Calendar.DAY_OF_MONTH, 1)
    c.set(Calendar.HOUR_OF_DAY, 0)
    c.set(Calendar.MINUTE, 0)
    c.set(Calendar.SECOND, 0)
    c.set(Calendar.MILLISECOND, 0)
    val kf = SimpleDateFormat("yyyy-MM", Locale.US)
    val lf = SimpleDateFormat("MMM", Locale.US)
    repeat(n) {
        out.add(0, kf.format(c.time) to lf.format(c.time))
        c.add(Calendar.MONTH, -1)
    }
    return out
}

fun inr(v: Long): String {
    val a = if (v < 0) -v else v
    val s = a.toString()
    val body: String = if (s.length <= 3) {
        s
    } else {
        val last3 = s.substring(s.length - 3)
        var rest = s.substring(0, s.length - 3)
        val parts = ArrayList<String>()
        while (rest.length > 2) {
            parts.add(0, rest.substring(rest.length - 2))
            rest = rest.substring(0, rest.length - 2)
        }
        if (rest.isNotEmpty()) parts.add(0, rest)
        parts.joinToString(",") + "," + last3
    }
    return (if (v < 0) "-₹" else "₹") + body
}

@Composable
fun KamaiChart(values: List<Long>, labels: List<String>) {
    val maxV = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Column {
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            val w = size.width
            val h = size.height
            if (values.size < 2) return@Canvas
            val step = w / (values.size - 1)
            val pts = values.mapIndexed { i, v ->
                Offset(i * step, h - (v.toFloat() / maxV.toFloat()) * (h * 0.70f) - h * 0.16f)
            }
            val line = Path()
            line.moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) line.lineTo(pts[i].x, pts[i].y)
            val fill = Path()
            fill.addPath(line)
            fill.lineTo(pts[pts.size - 1].x, h)
            fill.lineTo(pts[0].x, h)
            fill.close()
            drawPath(
                fill,
                Brush.verticalGradient(listOf(Teal.copy(alpha = 0.30f), Color.Transparent), startY = 0f, endY = h)
            )
            drawPath(line, color = Teal, style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            for (p in pts) {
                drawCircle(Color.White, radius = 8f, center = p)
                drawCircle(Teal, radius = 5f, center = p)
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { Text(it, color = Ink3, fontSize = 10.sp) }
        }
    }
}

@Composable
fun PaisaScreen(db: Db, refreshKey: Int, onChanged: () -> Unit) {
    val ctx = LocalContext.current
    var pays by remember { mutableStateOf(db.pays()) }
    var leads by remember { mutableStateOf(db.leads()) }
    var showAdd by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var confirmRestore by remember { mutableStateOf(false) }

    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            note = Restore.run(ctx, db, uri)
            pays = db.pays()
            leads = db.leads()
            onChanged()
        }
    }

    LaunchedEffect(refreshKey) {
        pays = db.pays()
        leads = db.leads()
    }

    val months = lastMonths(6)
    val keys = months.map { it.first }
    val labels = months.map { it.second }
    val curKey = keys[keys.size - 1]
    val earnings = pays.filter { it.kind == "Brokerage" || it.kind == "Other" }
    val vals = keys.map { k -> earnings.filter { monthKey(it.ts) == k }.sumOf { it.amount } }
    val thisM = vals[vals.size - 1]
    val lastM = if (vals.size >= 2) vals[vals.size - 2] else 0L
    val diff = thisM - lastM
    val pct = if (lastM > 0) diff * 100.0 / lastM else 0.0

    val rentPaid = pays.filter { it.kind == "Rent" && monthKey(it.ts) == curKey }
    val tenants = leads.filter { it.kind == "Tenant" && it.budget.isNotBlank() }

    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val ok = Backup.write(ctx, db, uri)
            note = if (ok) "Backup save ho gaya ✓" else "Backup fail hua — dobara try karo"
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(6.dp))
        Text("PAISA", color = Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
        Text("Kamai aur rent", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        Clay(Modifier.fillMaxWidth()) {
            Cap("Is mahine ki kamai")
            Text(inr(thisM), color = Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val up = diff >= 0
                Pill(
                    (if (up) "↑ " else "↓ ") + inr(if (diff < 0) -diff else diff),
                    if (up) Greens else Color(0xFFF7E4E0),
                    if (up) Green else Color(0xFFB0564A),
                    small = true
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (lastM > 0) String.format(Locale.US, "%.0f%% pichhle mahine se", pct) else "pichhla data nahi",
                    color = Ink3, fontSize = 11.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Pichhle mahine " + inr(lastM) + "   ·   Ab tak " + inr(earnings.sumOf { it.amount }),
                color = Ink2, fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(10.dp))
        Clay(Modifier.fillMaxWidth()) {
            Cap("6 mahine ka chart")
            Spacer(Modifier.height(12.dp))
            KamaiChart(vals, labels)
        }

        Spacer(Modifier.height(10.dp))
        Clay(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Cap("Rent ledger · is mahine")
                Spacer(Modifier.weight(1f))
                Text(inr(rentPaid.sumOf { it.amount }), color = Green, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            if (tenants.isEmpty()) {
                Text("Koi tenant nahi. Leads mein kind = Tenant chuno.", color = Ink3, fontSize = 12.sp)
            } else {
                tenants.take(6).forEach { t ->
                    val paid = rentPaid.filter {
                        (it.phone.isNotBlank() && norm(it.phone) == t.phone) || it.name.equals(t.name, ignoreCase = true)
                    }.sumOf { it.amount }
                    val due = t.budget.filter { ch -> ch.isDigit() }.toLongOrNull() ?: 0L
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (t.name.isBlank()) t.phone else t.name,
                            color = Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(inr(if (due > 0) due else paid), color = Ink2, fontSize = 12.sp)
                        Spacer(Modifier.width(8.dp))
                        val okPaid = due > 0 && paid >= due
                        Pill(
                            if (okPaid) "Aaya" else if (paid > 0) "Aadha" else "Baaki",
                            if (okPaid) Greens else if (paid > 0) Ambers else Color(0xFFF7E4E0),
                            if (okPaid) Green else if (paid > 0) Amber else Color(0xFFB0564A),
                            small = true
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Cap("Payments")
            Spacer(Modifier.weight(1f))
            Pill("+ Payment", Ambers, Amber, onClick = { showAdd = true })
        }
        Spacer(Modifier.height(6.dp))

        if (pays.isEmpty()) {
            Text("Abhi koi payment nahi. + Payment dabao.", color = Ink3, fontSize = 12.sp)
        }
        pays.take(20).forEach { p ->
            Clay(Modifier.fillMaxWidth().padding(vertical = 4.dp), pad = 12.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (p.name.isBlank()) "—" else p.name,
                            color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            p.kind + "  ·  " + SimpleDateFormat("d MMM yy", Locale.US).format(Date(p.ts)),
                            color = Ink3, fontSize = 11.sp
                        )
                    }
                    Text(inr(p.amount), color = Green, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Pill("✕", Color(0xFFF7E4E0), Color(0xFFB0564A), small = true, onClick = {
                        db.deletePay(p.id)
                        pays = db.pays()
                        onChanged()
                    })
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) {
                BigButton("Backup banao", bg1 = Ink2, bg2 = Ink) {
                    save.launch("amritpal-property-backup.json")
                }
            }
            Box(Modifier.weight(1f)) {
                BigButton("Restore karo", bg1 = Color(0xFF3E8FD0), bg2 = Color(0xFF1F5E92)) {
                    confirmRestore = true
                }
            }
        }
        if (note.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(note, color = Teal, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Backup mein leads, payments, albums aur calls aate hain. Photo files nahi — unhe WhatsApp par bhej ke rakh lo.",
            color = Ink3, fontSize = 10.5.sp
        )
        Spacer(Modifier.height(30.dp))
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Backup se restore?") },
            text = {
                Text(
                    "Jo data abhi app mein hai wo delete ho jayega aur backup file wala data aa jayega.\n\n" +
                        "Pehle se backup liya hua hai na? Warna pehle 'Backup banao' dabao.",
                    color = Ink2, fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    open.launch(arrayOf("*/*"))
                }) { Text("Haan, restore karo") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = false }) { Text("Cancel") }
            }
        )
    }

    if (showAdd) {
        PayDialog(
            onClose = { showAdd = false },
            onSave = { n, ph, amt, kind, nt, ts ->
                db.addPay(Pay(name = n, phone = ph, amount = amt, kind = kind, note = nt, ts = ts))
                showAdd = false
                pays = db.pays()
                onChanged()
            }
        )
    }
}

@Composable
fun PayDialog(onClose: () -> Unit, onSave: (String, String, Long, String, String, Long) -> Unit) {
    val ctx = LocalContext.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf("Brokerage") }
    var note by remember { mutableStateOf("") }
    var ts by remember { mutableStateOf(System.currentTimeMillis()) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Payment") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Customer ka naam") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone, onValueChange = { phone = it },
                    label = { Text("Mobile (optional)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount, onValueChange = { v -> amount = v.filter { it.isDigit() } },
                    label = { Text("Amount ₹") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Brokerage", "Rent", "Advance", "Other").forEach { k ->
                        val on = k == kind
                        Pill(
                            k,
                            if (on) Amber else Cream2,
                            if (on) Color.White else Ink2,
                            small = true,
                            onClick = { kind = k }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        SimpleDateFormat("d MMM yy", Locale.US).format(Date(ts)),
                        color = Ink2, fontSize = 13.sp
                    )
                    Spacer(Modifier.width(10.dp))
                    Pill("Date badlo", Ambers, Amber, small = true, onClick = {
                        pickDate(ctx, ts) { ts = it }
                    })
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Note (jaise 2BHK Model Town)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (amount.isNotBlank()) onSave(name, phone, amount.toLongOrNull() ?: 0L, kind, note, ts)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Cancel") } }
    )
}
