package com.linqi.triplog.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.linqi.triplog.data.local.Trip
import com.linqi.triplog.remind.departLabel
import com.linqi.triplog.ui.components.RouteArrowRow
import com.linqi.triplog.ui.components.VehicleIcon
import com.linqi.triplog.ui.components.vehicleName
import com.linqi.triplog.ui.nav.Routes
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    tripId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: TripDetailViewModel = viewModel()
) {
    val trip by viewModel.observe(tripId).collectAsState(initial = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("行程详情", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
                actions = { TextButton(onClick = { onEdit(tripId) }) { Text("编辑") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { inner ->
        val t = trip
        if (t == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (t.ticketImagePath.isNotBlank() && File(t.ticketImagePath).exists()) {
                AsyncImage(
                    model = File(t.ticketImagePath),
                    contentDescription = "车票图",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.height(16.dp))
            }

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VehicleIcon(t.vehicleType, size = 22.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(vehicleName(t.vehicleType), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.weight(1f))
                        if (t.remindBeforeMinutes > 0) {
                            Text(
                                "提前 ${t.remindBeforeMinutes} 分钟提醒",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    RouteArrowRow(t.fromStation, t.toStation)
                }
            }

            Spacer(Modifier.height(16.dp))

            DetailRow("出发时间", t.departTime?.let { formatMillis(it) } ?: "未设置")
            DetailRow("到达时间", t.arriveTime?.let { formatMillis(it) } ?: "未设置")
            DetailRow("车次 / 航班号", t.number.ifBlank { "未填写" })
            DetailRow("座位", t.seat.ifBlank { "未填写" })
            DetailRow("票价", t.price.ifBlank { "未填写" })
            DetailRow("乘车人", t.passenger.ifBlank { "未填写" })

            if (t.note.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text("备注", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                Text(t.note, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(96.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun formatMillis(millis: Long): String {
    val dt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime()
    return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
}
