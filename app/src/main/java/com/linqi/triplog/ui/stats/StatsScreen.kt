package com.linqi.triplog.ui.stats

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.linqi.triplog.data.local.VehicleType
import com.linqi.triplog.data.stats.MonthlyStat
import com.linqi.triplog.ui.components.EmptyState
import com.linqi.triplog.ui.components.VehicleIcon
import com.linqi.triplog.ui.components.vehicleName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onBack: () -> Unit,
    viewModel: StatsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("统计", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { inner ->
        val stat = state.stat

        if (state.loading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (stat == null || stat.total == 0) {
            Box(
                modifier = Modifier.fillMaxSize().padding(inner),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = "还没有可统计的数据",
                    subtitle = "先添加几条行程，这里会显示月度分布和常去城市"
                )
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
            // 年份切换
            if (state.years.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.years.forEach { y ->
                        FilterChip(
                            selected = state.selectedYear == y,
                            onClick = { viewModel.selectYear(y) },
                            label = { Text("$y 年", style = MaterialTheme.typography.labelMedium) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // 总览卡片
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        state.selectedYear?.let { "$it 年总览" } ?: "总览",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        StatCell(
                            value = "${stat.total}",
                            label = "次出行",
                            modifier = Modifier.weight(1f)
                        )
                        StatCell(
                            value = "${stat.cityCount}",
                            label = "个城市",
                            modifier = Modifier.weight(1f)
                        )
                        StatCell(
                            value = formatFare(stat.totalFare),
                            label = "总花费",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (stat.topFromStation != null) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "最常从 ${stat.topFromStation} 出发",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (stat.busiestMonth != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "最忙是 ${stat.busiestMonth.monthValue} 月",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 交通工具占比
            if (stat.byType.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("交通工具", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        stat.byType.forEach { (type, count) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                VehicleIcon(type, size = 18.dp)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    vehicleName(type),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.width(48.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                // 占比条
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.outline,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                ) {
                                    val fraction = if (stat.total > 0) {
                                        count.toFloat() / stat.total.toFloat()
                                    } else {
                                        0f
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction)
                                            .height(8.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "$count 次",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 常去城市
            if (stat.topCities.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("常去城市", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        stat.topCities.forEachIndexed { index, (city, count) ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${index + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(20.dp)
                                )
                                Text(
                                    city,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "$count 次",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 月度明细
            if (stat.months.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("月度明细", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                stat.months.forEach { m ->
                    MonthRow(m)
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MonthRow(m: MonthlyStat) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${m.yearMonth.year} 年 ${m.yearMonth.monthValue} 月",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${m.total} 次 · ${m.cityCount} 个城市 · ${formatFare(m.totalFare)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatFare(value: Double): String {
    return if (value <= 0) {
        "—"
    } else if (value % 1.0 == 0.0) {
        value.toInt().toString()
    } else {
        // 显式指定 locale，避免部分语言环境下小数点变成逗号
        String.format(java.util.Locale.ROOT, "%.1f", value)
    }
}
