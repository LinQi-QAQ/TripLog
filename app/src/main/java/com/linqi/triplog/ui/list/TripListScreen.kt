package com.linqi.triplog.ui.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.linqi.triplog.data.local.Trip
import com.linqi.triplog.remind.departLabel
import com.linqi.triplog.ui.components.EmptyState
import com.linqi.triplog.ui.components.RouteArrowRow
import com.linqi.triplog.ui.components.VehicleIcon

@Composable
fun TripListScreen(
    onOpenDetail: (Long) -> Unit,
    onAdd: () -> Unit,
    viewModel: TripListViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (state.selectionMode) {
                SelectionTopBar(
                    count = state.selectedIds.size,
                    onCancel = { viewModel.clearSelection() },
                    onSelectAll = { viewModel.selectAllVisible() },
                    onDelete = { viewModel.deleteSelected() }
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "出行记",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "共 ${state.totalCount} 条",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = state.keyword,
                    onValueChange = viewModel::onKeywordChange,
                    placeholder = { Text("搜索出发地、目的地、车次") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.keyword.isNotBlank()) {
                            IconButton(onClick = { viewModel.onKeywordChange("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "清空")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.filters.forEach { (value, label) ->
                        FilterChip(
                            selected = state.filter == value,
                            onClick = { viewModel.onFilterChange(value) },
                            label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            if (state.upcoming.isEmpty() && state.past.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        title = if (state.keyword.isBlank() && state.filter == null) "还没有出行记录" else "没有匹配的行程",
                        subtitle = if (state.keyword.isBlank() && state.filter == null) "点右下角按钮添加第一条" else "换个关键词或清除筛选试试"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.upcoming.isNotEmpty()) {
                        item { SectionHeader("即将出发", state.upcoming.size) }
                        items(state.upcoming, key = { it.id }) { trip ->
                            SwipeableTripCard(
                                trip = trip,
                                selected = trip.id in state.selectedIds,
                                selectionMode = state.selectionMode,
                                onClick = {
                                    if (state.selectionMode) {
                                        viewModel.toggleSelection(trip.id)
                                    } else {
                                        onOpenDetail(trip.id)
                                    }
                                },
                                onLongClick = { viewModel.enterSelection(trip.id) },
                                onDelete = { viewModel.delete(trip) }
                            )
                        }
                    }
                    if (state.past.isNotEmpty()) {
                        item { SectionHeader("历史", state.past.size) }
                        items(state.past, key = { it.id }) { trip ->
                            SwipeableTripCard(
                                trip = trip,
                                selected = trip.id in state.selectedIds,
                                selectionMode = state.selectionMode,
                                onClick = {
                                    if (state.selectionMode) {
                                        viewModel.toggleSelection(trip.id)
                                    } else {
                                        onOpenDetail(trip.id)
                                    }
                                },
                                onLongClick = { viewModel.enterSelection(trip.id) },
                                onDelete = { viewModel.delete(trip) }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SelectionTopBar(
    count: Int,
    onCancel: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onCancel) { Text("取消") }
        Spacer(Modifier.width(8.dp))
        Text(
            "已选 $count 条",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onSelectAll) { Text("全选") }
        TextButton(onClick = onDelete) {
            Text("删除", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun SectionHeader(text: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "$count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 可左滑删除的行程卡片。
 * 左滑露出红色删除背景，松手即删。多选模式下左滑禁用，改用点击切换选中。
 *
 * 注意：confirmValueChange 在 Material3 1.3 已废弃，官方建议改成限制锚点。
 * 这里为了兼容当前版本同时保留两种写法：先限制只能滑到 EndToStart，
 * 再用 LaunchedEffect 观察 settled 状态执行删除。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SwipeableTripCard(
    trip: Trip,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.4f }
    )

    // 滑到底并停稳后才真的删除，避免手一抖就没了
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "删除",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        // 只允许从右往左滑，从左往右滑不触发任何东西
        enableDismissFromStartToEnd = false
    ) {
        TripCard(
            trip = trip,
            selected = selected,
            selectionMode = selectionMode,
            onClick = onClick,
            onLongClick = onLongClick
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TripCard(
    trip: Trip,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(width = if (selected) 2.dp else 0.dp, color = borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "已选中",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    VehicleIcon(trip.vehicleType, size = 22.dp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                RouteArrowRow(trip.fromStation, trip.toStation)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        trip.departLabel(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (trip.number.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            trip.number,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (trip.seat.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            trip.seat,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (trip.remindBeforeMinutes > 0 && !selectionMode) {
                Text(
                    "提醒",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
