package com.linqi.triplog.data.repo

import com.linqi.triplog.data.local.Trip
import com.linqi.triplog.data.local.TripDao
import com.linqi.triplog.data.local.VehicleType
import kotlinx.coroutines.flow.Flow

class TripRepository(private val dao: TripDao) {

    fun observeAll(): Flow<List<Trip>> = dao.observeAll()

    fun observeById(id: Long): Flow<Trip?> = dao.observeById(id)

    suspend fun getById(id: Long): Trip? = dao.getById(id)

    suspend fun countAll(): Int = dao.count()

    /** 取一份快照，供开机重建提醒等非 Flow 场景用。 */
    suspend fun getAllSnapshot(): List<Trip> = dao.getAll()

    suspend fun insert(trip: Trip): Long = dao.insert(trip)

    suspend fun update(trip: Trip) = dao.update(trip)

    suspend fun delete(trip: Trip) = dao.delete(trip)

    suspend fun deleteByIds(ids: List<Long>) = dao.deleteByIds(ids)

    /**
     * 关键词搜索 + 类型筛选，条件直接下发到 SQLite。
     * 之前是全表加载后在内存里 filter，几千条时会卡；现在由数据库按索引筛。
     * 关键词前后空白会被去掉，空关键词等价于不筛。
     */
    fun search(keyword: String, filter: String? = null): Flow<List<Trip>> =
        dao.observeFiltered(keyword.trim(), filter)

    fun isVehicleType(value: String): Boolean = VehicleType.all.contains(value)
}
