package com.linqi.triplog.data.stats

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 车站名归一化到城市。
 * 目标是「上海虹桥」和「上海南」都算上海，但不要误把不同城市合并。
 */
class CityNormalizerTest {

    @Test
    fun `方位后缀被剥掉`() {
        assertEquals("北京", CityNormalizer.toCity("北京南"))
        assertEquals("上海", CityNormalizer.toCity("上海虹桥"))
        assertEquals("广州", CityNormalizer.toCity("广州东"))
        assertEquals("杭州", CityNormalizer.toCity("杭州东"))
    }

    @Test
    fun `站字也被剥掉`() {
        assertEquals("南京", CityNormalizer.toCity("南京站"))
        assertEquals("北京", CityNormalizer.toCity("北京南站"))
    }

    @Test
    fun `机场名归到城市`() {
        assertEquals("上海", CityNormalizer.toCity("浦东机场"))
        assertEquals("上海", CityNormalizer.toCity("虹桥机场"))
        assertEquals("北京", CityNormalizer.toCity("首都机场"))
        assertEquals("广州", CityNormalizer.toCity("白云机场"))
        assertEquals("深圳", CityNormalizer.toCity("宝安机场"))
    }

    @Test
    fun `已是城市名则原样返回`() {
        assertEquals("上海", CityNormalizer.toCity("上海"))
        assertEquals("北京", CityNormalizer.toCity("北京"))
        assertEquals("南京", CityNormalizer.toCity("南京"))
        assertEquals("广州", CityNormalizer.toCity("广州"))
    }

    @Test
    fun `不会把城市名结尾的字当后缀`() {
        // 南京的「京」、杭州的「州」都不该被剥
        assertEquals("南京", CityNormalizer.toCity("南京"))
        assertEquals("杭州", CityNormalizer.toCity("杭州"))
    }

    @Test
    fun `空输入返回空串`() {
        assertEquals("", CityNormalizer.toCity(""))
        assertEquals("", CityNormalizer.toCity("   "))
    }

    @Test
    fun `前后空白被去掉`() {
        assertEquals("北京", CityNormalizer.toCity(" 北京南 "))
    }
}
