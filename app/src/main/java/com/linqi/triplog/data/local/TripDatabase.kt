package com.linqi.triplog.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Trip::class], version = 2, exportSchema = false)
abstract class TripDatabase : RoomDatabase() {

    abstract fun tripDao(): TripDao

    companion object {
        @Volatile
        private var instance: TripDatabase? = null

        /**
         * v1 到 v2：新增 priceValue 列，用来存解析后的票价数值。
         *
         * 这一版不能再用破坏性迁移了，因为已经在正式分发，用户手上有真实行程数据，
         * 升级时清库是不可接受的。新增的是可空列，迁移只需要 ALTER TABLE 一条语句。
         *
         * priceValue 存 null 表示票价没有数值（比如「免费」或没填），
         * 统计时这类记录不计入求和，与「0 元」区分开。
         * 老数据升级后 priceValue 全是 null，统计会回退到解析 price 字符串，
         * 所以历史数据的统计结果不受影响。
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE trips ADD COLUMN priceValue REAL")
            }
        }

        fun get(context: Context): TripDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TripDatabase::class.java,
                    "triplog.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    // 保留破坏性迁移作为最后兜底：万一遇到没写迁移的版本跳跃，
                    // 宁可清库让 app 能用，也不要让用户卡在打不开的状态。
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
