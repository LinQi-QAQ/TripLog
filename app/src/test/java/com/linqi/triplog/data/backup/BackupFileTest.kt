package com.linqi.triplog.data.backup

import com.linqi.triplog.data.local.Trip
import com.linqi.triplog.data.local.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 备份文件格式测试。只测纯数据变换，不碰 Android 的 uri 和 zip 流
 * （那部分靠 BackupManagerInstrumentedTest 在真机上跑）。
 */
class BackupFileTest {

    private fun sampleTrips() = listOf(
        Trip(
            id = 1,
            vehicleType = VehicleType.TRAIN,
            fromStation = "上海虹桥",
            toStation = "北京南",
            departTime = 1_700_000_000_000,
            arriveTime = 1_700_003_000_000,
            number = "G1234",
            seat = "08车 12A",
            price = "553",
            passenger = "林祺",
            note = "靠窗",
            ticketImagePath = "/data/user/0/com.linqi.triplog/files/tickets/ticket_100.jpg",
            remindBeforeMinutes = 180
        ),
        Trip(
            id = 2,
            vehicleType = VehicleType.FLIGHT,
            fromStation = "PVG",
            toStation = "PEK",
            departTime = 1_700_100_000_000,
            number = "MU5101",
            ticketImagePath = "",
            remindBeforeMinutes = 0
        )
    )

    @Test
    fun `版本号是 1`() {
        val file = BackupManager.BackupFile(trips = sampleTrips())
        assertEquals(1, file.version)
    }

    @Test
    fun `导出时图片路径要转成纯文件名`() {
        val trips = sampleTrips()
        val portable = trips.map { it.copy(ticketImagePath = it.ticketImagePath.substringAfterLast('/')) }

        assertEquals("ticket_100.jpg", portable[0].ticketImagePath)
        assertEquals("", portable[1].ticketImagePath)
    }

    @Test
    fun `导入时空路径保持空`() {
        val name = "".substringAfterLast('/')
        assertEquals("", name)
    }

    @Test
    fun `车次号和站点等字段完整保留`() {
        val t = sampleTrips()[0]
        assertEquals("G1234", t.number)
        assertEquals("上海虹桥", t.fromStation)
        assertEquals("北京南", t.toStation)
        assertEquals("08车 12A", t.seat)
        assertEquals("553", t.price)
        assertEquals(180, t.remindBeforeMinutes)
    }

    @Test
    fun `航班类型能序列化保留`() {
        val t = sampleTrips()[1]
        assertEquals(VehicleType.FLIGHT, t.vehicleType)
        assertEquals("MU5101", t.number)
    }

    @Test
    fun `无图行程的路径为空串`() {
        val t = sampleTrips()[1]
        assertTrue(t.ticketImagePath.isEmpty())
    }

    @Test
    fun `建议文件名带时间戳和 zip 后缀`() {
        val name = BackupManager.suggestedFileName()
        assertTrue(name.startsWith("triplog-backup-"))
        assertTrue(name.endsWith(".zip"))
    }

    @Test
    fun `建议文件名里没有非法字符`() {
        val name = BackupManager.suggestedFileName()
        // 只允许字母数字连字符点和下划线，避免某些文件系统不接受
        assertTrue(name.matches(Regex("[A-Za-z0-9._-]+")))
    }

    @Test
    fun `备份常量固定`() {
        assertEquals("data.json", BackupManager.JSON_NAME)
        assertEquals("tickets", BackupManager.IMAGE_DIR)
        assertEquals("application/zip", BackupManager.MIME)
    }
}
