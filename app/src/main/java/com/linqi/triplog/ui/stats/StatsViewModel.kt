package com.linqi.triplog.ui.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.linqi.triplog.data.stats.OverallStat
import com.linqi.triplog.data.stats.StatsCalculator
import com.linqi.triplog.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StatsUiState(
    val loading: Boolean = true,
    val stat: OverallStat? = null,
    val years: List<Int> = emptyList(),
    val selectedYear: Int? = null
)

class StatsViewModel(
    app: Application,
    private val repo: com.linqi.triplog.data.repo.TripRepository
) : AndroidViewModel(app) {

    /** 反射创建用：AndroidViewModelFactory 只认 (Application) 单参构造函数。 */
    constructor(app: Application) : this(app, AppContainer.of(app).tripRepository)

    private val _state = MutableStateFlow(StatsUiState())
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeAll().collect { trips ->
                val now = System.currentTimeMillis()
                val years = StatsCalculator.years(trips, now)
                val current = _state.value.selectedYear ?: years.firstOrNull()
                val filtered = current?.let { y -> StatsCalculator.tripsOfYear(trips, y, now) } ?: trips
                _state.value = StatsUiState(
                    loading = false,
                    stat = StatsCalculator.overall(filtered, now),
                    years = years,
                    selectedYear = current
                )
            }
        }
    }

    fun selectYear(year: Int) {
        viewModelScope.launch {
            val trips = repo.getAllSnapshot()
            val now = System.currentTimeMillis()
            val filtered = StatsCalculator.tripsOfYear(trips, year, now)
            _state.value = _state.value.copy(
                selectedYear = year,
                stat = StatsCalculator.overall(filtered, now)
            )
        }
    }
}
