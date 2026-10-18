package com.linqi.triplog.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.linqi.triplog.data.local.Trip
import com.linqi.triplog.data.repo.TripRepository
import com.linqi.triplog.di.AppContainer

class TripDetailViewModel(
    app: Application,
    private val repo: TripRepository
) : AndroidViewModel(app) {

    /** 反射创建用：AndroidViewModelFactory 只认 (Application) 单参构造函数。 */
    constructor(app: Application) : this(app, AppContainer.of(app).tripRepository)

    fun observe(id: Long): kotlinx.coroutines.flow.Flow<Trip?> = repo.observeById(id)
}
