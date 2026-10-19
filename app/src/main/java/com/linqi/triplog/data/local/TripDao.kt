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

    /**
     * 带搜索和类型筛选的查询，过滤交给 SQLite 做，不在内存里筛。
     * keyword 传空串表示不筛关键词；vehicleType 传 null 表示不限类型。
     * 用 LIKE 配 % 通配，SQLite 的 LIKE 对 ASCII 不区分大小写，
     * 中文本身没有大小写概念，所以不需要额外 lower()。
     */
    @androidx.room.Query(
        """
        SELECT * FROM trips
        WHERE (:vehicleType IS NULL OR vehicleType = :vehicleType)
          AND (
                :keyword = ''
                OR fromStation LIKE '%' || :keyword || '%'
                OR toStation   LIKE '%' || :keyword || '%'
                OR number      LIKE '%' || :keyword || '%'
                OR note        LIKE '%' || :keyword || '%'
                OR passenger   LIKE '%' || :keyword || '%'
              )
        ORDER BY departTime IS NULL, departTime ASC, createdAt DESC
        """
    )
    fun observeFiltered(keyword: String, vehicleType: String?): Flow<List<Trip>>

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
