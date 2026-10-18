package com.linqi.triplog.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.linqi.triplog.data.local.Trip
import com.linqi.triplog.data.local.TripDatabase
import com.linqi.triplog.data.local.VehicleType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TripRepositoryTest {

    private lateinit var db: TripDatabase
    private lateinit var repo: TripRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TripDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = TripRepository(db.tripDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `搜索命中出发地`() = runTest {
        repo.insert(Trip(fromStation = "上海虹桥", toStation = "北京南", departTime = 1L))
        repo.insert(Trip(fromStation = "广州南", toStation = "深圳北", departTime = 2L))

        val result = repo.observeAll().first().filter { it.fromStation.contains("上海", true) }
        assertEquals(1, result.size)
        assertEquals("上海虹桥", result[0].fromStation)
    }

    @Test
    fun `搜索命中车次号`() = runTest {
        repo.insert(Trip(fromStation = "上海", toStation = "北京", number = "G1234", departTime = 1L))
        repo.insert(Trip(fromStation = "上海", toStation = "南京", number = "G2", departTime = 2L))

        val result = repo.observeAll().first().filter { it.number.contains("1234", true) }
        assertEquals(1, result.size)
    }

    @Test
    fun `搜索大小写不敏感`() = runTest {
        repo.insert(Trip(fromStation = "PVG", toStation = "PEK", number = "MU5101", departTime = 1L))
        val result = repo.observeAll().first().filter { it.number.contains("mu5101", true) }
        assertEquals(1, result.size)
    }

    @Test
    fun `按交通工具筛选`() = runTest {
        repo.insert(Trip(vehicleType = VehicleType.TRAIN, fromStation = "A", toStation = "B", departTime = 1L))
        repo.insert(Trip(vehicleType = VehicleType.FLIGHT, fromStation = "C", toStation = "D", departTime = 2L))
        repo.insert(Trip(vehicleType = VehicleType.BUS, fromStation = "E", toStation = "F", departTime = 3L))

        val flights = repo.observeAll().first().filter { it.vehicleType == VehicleType.FLIGHT }
        assertEquals(1, flights.size)
        assertEquals("C", flights[0].fromStation)
    }

    @Test
    fun `计数与快照一致`() = runTest {
        repo.insert(Trip(fromStation = "A", toStation = "B", departTime = 1L))
        repo.insert(Trip(fromStation = "C", toStation = "D", departTime = 2L))
        repo.insert(Trip(fromStation = "E", toStation = "F", departTime = 3L))

        assertEquals(3, repo.countAll())
        assertEquals(3, repo.getAllSnapshot().size)
    }
}
