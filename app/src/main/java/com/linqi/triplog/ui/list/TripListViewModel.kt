package com.linqi.triplog.ui.list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.linqi.triplog.di.AppContainer
import com.linqi.triplog.data.local.Trip
import com.linqi.triplog.data.local.VehicleType
import com.linqi.triplog.data.repo.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ListUiState(
    val upcoming: List<Trip> = emptyList(),
    val past: List<Trip> = emptyList(),
    val keyword: String = "",
    val filter: String? = null,
    val totalCount: Int = 0,
    /** 多选模式下选中的行程 id。空集合表示不在多选模式。 */
    val selectedIds: Set<Long> = emptySet()
) {
    val selectionMode: Boolean get() = selectedIds.isNotEmpty()
}

/**
 * 列表页 ViewModel。
 *
 * 注意那个只有 Application 一个参数的次构造函数：
 * AndroidX 的 AndroidViewModelFactory 用反射找 `(Application)` 构造函数来创建实例，
 * 如果只有带默认值的多参构造函数，Kotlin 不会生成 `(Application)` 版本，
 * 运行时会抛 NoSuchMethodException。这个坑只有在真机/模拟器上跑才会暴露，
 * Robolectric 测试里是手动注入仓库的，绕过了反射创建路径。
 */
class TripListViewModel(
    app: Application,
    private val repo: TripRepository
) : AndroidViewModel(app) {

    constructor(app: Application) : this(app, AppContainer.of(app).tripRepository)

    private val repository = repo

    private val keyword = MutableStateFlow("")
    private val filter = MutableStateFlow<String?>(null)
    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<ListUiState> =
        combine(
            repository.observeAll(),
            keyword,
            filter,
            selectedIds
        ) { trips, kw, f, selected ->
            val filtered = trips.filter { trip ->
                val kwOk = kw.isBlank() ||
                    trip.fromStation.contains(kw, true) ||
                    trip.toStation.contains(kw, true) ||
                    trip.number.contains(kw, true) ||
                    trip.note.contains(kw, true) ||
                    trip.passenger.contains(kw, true)
                val fOk = f == null || trip.vehicleType == f
                kwOk && fOk
            }
            val now = System.currentTimeMillis()
            ListUiState(
                upcoming = filtered.filter { (it.departTime ?: now) >= now },
                past = filtered.filter { (it.departTime ?: now) < now },
                keyword = kw,
                filter = f,
                totalCount = trips.size,
                selectedIds = selected
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListUiState())

    fun onKeywordChange(v: String) {
        keyword.value = v
    }

    fun onFilterChange(v: String?) {
        filter.value = if (v == filter.value) null else v
    }

    fun clearFilter() {
        filter.value = null
    }

    /** 长按进入多选，或切换某一项的选中状态。 */
    fun toggleSelection(id: Long) {
        val current = selectedIds.value
        selectedIds.value = if (current.contains(id)) current - id else current + id
    }

    fun enterSelection(id: Long) {
        selectedIds.value = setOf(id)
    }

    fun clearSelection() {
        selectedIds.value = emptySet()
    }

    fun selectAllVisible() {
        val all = (uiState.value.upcoming + uiState.value.past).map { it.id }.toSet()
        selectedIds.value = all
    }

    fun delete(trip: Trip) {
        viewModelScope.launch {
            repository.delete(trip)
            com.linqi.triplog.remind.ReminderScheduler.cancel(getApplication(), trip.id)
        }
    }

    fun deleteSelected() {
        val ids = selectedIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.deleteByIds(ids)
            ids.forEach {
                com.linqi.triplog.remind.ReminderScheduler.cancel(getApplication(), it)
            }
            selectedIds.value = emptySet()
        }
    }

    fun deleteAll(ids: List<Long>) {
        viewModelScope.launch {
            repository.deleteByIds(ids)
            ids.forEach {
                com.linqi.triplog.remind.ReminderScheduler.cancel(getApplication(), it)
            }
        }
    }

    val filters: List<Pair<String?, String>>
        get() = listOf(
            null to "全部",
            VehicleType.TRAIN to "火车",
            VehicleType.FLIGHT to "飞机",
            VehicleType.BUS to "汽车",
            VehicleType.OTHER to "其他"
        )
}
