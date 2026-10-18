package com.linqi.triplog.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Room 数据层测试。用 Robolectric 在 JVM 上跑真实 SQLite，
 * 不需要模拟器或真机。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TripDaoTest {

    private lateinit var db: TripDatabase
    private lateinit var dao: TripDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TripDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.tripDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `插入后能按 id 读回`() = runTest {
        val id = dao.insert(Trip(fromStation = "上海", toStation = "北京", departTime = 1000L))
        val loaded = dao.getById(id)
        assertNotNull(loaded)
        assertEquals("上海", loaded!!.fromStation)
        assertEquals("北京", loaded.toStation)
        assertEquals(1000L, loaded.departTime)
    }

    @Test
    fun `默认车辆类型是火车`() = runTest {
        val id = dao.insert(Trip(fromStation = "A", toStation = "B"))
        assertEquals(VehicleType.TRAIN, dao.getById(id)!!.vehicleType)
    }

    @Test
    fun `列表按出发时间正序`() = runTest {
        dao.insert(Trip(fromStation = "早", toStation = "X", departTime = 100L))
        dao.insert(Trip(fromStation = "晚", toStation = "X", departTime = 900L))
        dao.insert(Trip(fromStation = "中", toStation = "X", departTime = 500L))

        val list = dao.observeAll().first()
        assertEquals(3, list.size)
        // 正序：最近的排最前，这样「即将出发」里最紧急的在上面
        assertEquals("早", list[0].fromStation)
        assertEquals("中", list[1].fromStation)
        assertEquals("晚", list[2].fromStation)
    }

    @Test
    fun `空时间的排在有时间的后面`() = runTest {
        dao.insert(Trip(fromStation = "有时间", toStation = "X", departTime = 100L))
        dao.insert(Trip(fromStation = "无时间", toStation = "X", departTime = null))

        val list = dao.observeAll().first()
        assertEquals("有时间", list[0].fromStation)
        assertEquals("无时间", list[1].fromStation)
    }

    @Test
    fun `更新生效`() = runTest {
        val id = dao.insert(Trip(fromStation = "旧", toStation = "B", departTime = 1L))
        val original = dao.getById(id)!!
        dao.update(original.copy(fromStation = "新", seat = "08车 12A"))

        val updated = dao.getById(id)!!
        assertEquals("新", updated.fromStation)
        assertEquals("08车 12A", updated.seat)
    }

    @Test
    fun `删除生效`() = runTest {
        val id = dao.insert(Trip(fromStation = "A", toStation = "B", departTime = 1L))
        dao.delete(dao.getById(id)!!)
        assertNull(dao.getById(id))
        assertEquals(0, dao.count())
    }

    @Test
    fun `批量删除只删选中的`() = runTest {
        val id1 = dao.insert(Trip(fromStation = "留", toStation = "B", departTime = 1L))
        val id2 = dao.insert(Trip(fromStation = "删1", toStation = "B", departTime = 2L))
        val id3 = dao.insert(Trip(fromStation = "删2", toStation = "B", departTime = 3L))

        dao.deleteByIds(listOf(id2, id3))
        assertEquals(1, dao.count())
        assertNotNull(dao.getById(id1))
    }

    @Test
    fun `统计条数`() = runTest {
        assertEquals(0, dao.count())
        dao.insert(Trip(fromStation = "A", toStation = "B", departTime = 1L))
        dao.insert(Trip(fromStation = "C", toStation = "D", departTime = 2L))
        assertEquals(2, dao.count())
    }

    @Test
    fun `车票图路径可存可取`() = runTest {
        val path = "/data/user/0/com.linqi.triplog/files/tickets/ticket_123.jpg"
        val id = dao.insert(Trip(fromStation = "A", toStation = "B", ticketImagePath = path))
        assertEquals(path, dao.getById(id)!!.ticketImagePath)
    }

    @Test
    fun `提醒字段默认关闭`() = runTest {
        val id = dao.insert(Trip(fromStation = "A", toStation = "B", departTime = 1L))
        val t = dao.getById(id)!!
        assertEquals(0, t.remindBeforeMinutes)
        assertTrue(!t.reminded)
    }

    @Test
    fun `getAll 返回快照`() = runTest {
        dao.insert(Trip(fromStation = "A", toStation = "B", departTime = 1L))
        dao.insert(Trip(fromStation = "C", toStation = "D", departTime = 2L))
        assertEquals(2, dao.getAll().size)
    }
}
