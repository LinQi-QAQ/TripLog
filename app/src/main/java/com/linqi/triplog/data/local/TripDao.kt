package com.linqi.triplog.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    // 时间正序：最近的排最前。这样「即将出发」里最紧急的在上面，
    // 「历史」里刚结束的也在上面。空时间的排最后。
    @androidx.room.Query("SELECT * FROM trips ORDER BY departTime IS NULL, departTime ASC, createdAt DESC")
    fun observeAll(): Flow<List<Trip>>

    @androidx.room.Query("SELECT * FROM trips WHERE id = :id")
    fun observeById(id: Long): Flow<Trip?>

    @androidx.room.Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getById(id: Long): Trip?

    @Insert
    suspend fun insert(trip: Trip): Long

    @Update
    suspend fun update(trip: Trip)

    @Delete
    suspend fun delete(trip: Trip)

    @androidx.room.Query("DELETE FROM trips WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @androidx.room.Query("DELETE FROM trips")
    suspend fun clear()

    @androidx.room.Query("SELECT COUNT(*) FROM trips")
    suspend fun count(): Int

    @androidx.room.Query("SELECT * FROM trips")
    suspend fun getAll(): List<Trip>
}
