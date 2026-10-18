package com.linqi.triplog.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * 一条出行记录。
 * 时间统一用毫秒时间戳（Long），展示时再格式化，方便排序和算倒计时。
 * 票价存字符串，因为会出现「¥123.5」「123.50 CNY」这种混合格式。
 *
 * @Serializable 是为了导出备份时能直接序列化成 JSON。
 * 新增字段时给默认值，这样旧备份文件反序列化不会炸。
 */
@Serializable
@Entity(
    tableName = "trips",
    indices = [
        Index("departTime"),
        Index("fromStation"),
        Index("toStation"),
        Index("number")
    ]
)
data class Trip(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val vehicleType: String = VehicleType.TRAIN,
    val fromStation: String = "",
    val toStation: String = "",
    val departTime: Long? = null,
    val arriveTime: Long? = null,
    val number: String = "",
    val seat: String = "",
    val price: String = "",
    val passenger: String = "",
    val note: String = "",
    /** 压缩后的车票图在应用私有目录下的路径，空表示没有图。 */
    val ticketImagePath: String = "",
    /** 提前多少分钟提醒，0 表示不提醒。 */
    val remindBeforeMinutes: Int = 0,
    /** 是否已经提醒过，改时间后重置。 */
    val reminded: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

object VehicleType {
    const val TRAIN = "train"
    const val FLIGHT = "flight"
    const val BUS = "bus"
    const val OTHER = "other"

    val all = listOf(TRAIN, FLIGHT, BUS, OTHER)
}
