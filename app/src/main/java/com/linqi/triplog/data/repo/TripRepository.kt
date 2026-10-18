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

    /** 关键词搜索：出发地、目的地、编号、备注里包含即命中。 */
    fun search(keyword: String, filter: String? = null): Flow<List<Trip>> =
        dao.observeAll().let { flow ->
            kotlinx.coroutines.flow.combine(flow, kotlinx.coroutines.flow.flowOf(keyword to filter)) { list, (kw, f) ->
                list.filter { trip ->
                    val matchFilter = f == null || trip.vehicleType == f
                    val kwOk = kw.isBlank() ||
                        trip.fromStation.contains(kw, true) ||
                        trip.toStation.contains(kw, true) ||
                        trip.number.contains(kw, true) ||
                        trip.note.contains(kw, true) ||
                        trip.passenger.contains(kw, true)
                    matchFilter && kwOk
                }
            }
        }

    fun isVehicleType(value: String): Boolean = VehicleType.all.contains(value)
}
