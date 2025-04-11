package com.tencent.devops.common.util

import com.tencent.devops.common.util.LoopUtil.DEFAULT_THRESHOLD_COUNT
import com.tencent.devops.common.util.LoopUtil.DEFAULT_THRESHOLD_MILLS
import com.tencent.devops.common.util.LoopUtil.SLEEP_INTERVAL
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

@Suppress("MagicNumber")
class LoopUtilTest {

    private val userDataBaseRecordSize = DEFAULT_THRESHOLD_COUNT * 100L // 假设
    private val userDataBase = mutableListOf<FakeStuPojo>()

    @BeforeEach
    fun initFakeDb() {
        userDataBase.clear()
        for (i in 1..userDataBaseRecordSize) {
            val pojo = randomGenCollegeStuPojo(i)
            userDataBase.add(pojo)
        }
        println("max_id: ${userDataBase.last().id}")
    }

    @ParameterizedTest
    @ValueSource(
        ints = [
            -1000 /*负数非法*/,
            -1 /*负数非法*/,
            0 /* 0不合理*/,
            DEFAULT_THRESHOLD_COUNT + 1 /*超出上限*/
        ]
    )
    fun testCheckThresholdCountIllegal(threshold: Int) {
        val vo = LoopUtil.LoopVo<Long, Any>(id = 0L, data = true, thresholdCount = threshold)
        assertEquals(threshold, vo.thresholdCount)
        vo.correctCtrlArgs()
        assertEquals(DEFAULT_THRESHOLD_COUNT, vo.thresholdCount)
    }

    @Test
    fun testCheckThresholdCountLegal() {
        for (count in 1..DEFAULT_THRESHOLD_COUNT) {
            val vo = LoopUtil.LoopVo<Long, Any>(id = 0L, data = true, thresholdCount = count)
            assertEquals(count, vo.thresholdCount)
            vo.correctCtrlArgs()
            assertEquals(count, vo.thresholdCount)
        }
    }

    @ParameterizedTest
    @ValueSource(
        longs = [
            -1000 /*负数非法*/,
            -1L /*负数非法*/,
            0 /* 0不合理*/,
            DEFAULT_THRESHOLD_MILLS + 1 /*超出上限*/
        ]
    )
    fun testCheckThresholdMillsIllegal(thresholdMills: Long) {
        val vo = LoopUtil.LoopVo<Long, Any>(id = 0L, data = true, thresholdMills = thresholdMills)
        assertEquals(thresholdMills, vo.thresholdMills)
        vo.correctCtrlArgs()
        assertEquals(DEFAULT_THRESHOLD_MILLS, vo.thresholdMills)
    }

    @Test
    fun testCheckThresholdMillsLegal() {
        for (thresholdMills in 1..DEFAULT_THRESHOLD_MILLS) {
            val vo = LoopUtil.LoopVo<Long, Any>(id = 0L, data = true, thresholdMills = thresholdMills)
            assertEquals(thresholdMills, vo.thresholdMills)
            vo.correctCtrlArgs()
            assertEquals(thresholdMills, vo.thresholdMills)
        }
    }

    @ParameterizedTest
    @ValueSource(
        longs = [
            -3 /*负数非法*/,
            -1 /*负数非法*/,
            0 /* 0不合理*/,
            SLEEP_INTERVAL + 1 /*超出上限*/
        ]
    )
    fun testCheckSleepIntervalMillsIllegal(sleepIntervalMills: Long) {
        val vo = LoopUtil.LoopVo<Long, Any>(id = 0L, data = true, sleepIntervalMills = sleepIntervalMills)
        assertEquals(sleepIntervalMills, vo.sleepIntervalMills)
        vo.correctCtrlArgs()
        assertEquals(SLEEP_INTERVAL, vo.sleepIntervalMills)
    }

    @Test
    fun testCheckSleepIntervalMillsLegal() {
        for (sleepIntervalMills in 1..SLEEP_INTERVAL) {
            val vo = LoopUtil.LoopVo<Long, Any>(id = 0L, data = true, sleepIntervalMills = sleepIntervalMills)
            assertEquals(sleepIntervalMills, vo.sleepIntervalMills)
            vo.correctCtrlArgs()
            assertEquals(sleepIntervalMills, vo.sleepIntervalMills)
        }
    }

    @Test
    fun read101RecsInLoopAndBreakByCount() {
        val thresholdCount = 5
        val vo = LoopUtil.LoopVo<Long, MutableList<FakeStuPojo>>(
            thresholdCount = thresholdCount, // doLoop 最多循环 10 次
            id = 0L, // 从0开始
            data = mutableListOf()
        )

        val metrics = read101RecsInLoop(vo, mockTime = 100L)
        println("metrics:$metrics")
        assertEquals(0, metrics.sleepCount) // 不会休息
    }

    @Test
    fun read101RecsInLoopAndBreakBySleepMills() {
        val thresholdCount = 10
        val vo = LoopUtil.LoopVo<Long, MutableList<FakeStuPojo>>(
            thresholdCount = thresholdCount, // doLoop 最多循环 10 次
            thresholdMills = 1100L, // 循环累计超过 1100ms 会被退出，不一定精确, 超过多少由runFunc本身的耗时决定
            sleepIntervalMills = 1000L, // 1000ms 休息一次
            id = 0L, // 从0开始
            data = mutableListOf()
        )

        val metrics = read101RecsInLoop(vo, mockTime = 150L) // 150 * 10 = 1500 > 1000 所以会休息一次
        println("metrics:$metrics")
        assertEquals(1, metrics.sleepCount) // 设置休息1次
    }

    private fun read101RecsInLoop(
        inputVo: LoopUtil.LoopVo<Long, MutableList<FakeStuPojo>>,
        mockTime: Long
    ): LoopUtil.LoopMetrics {
        val size = 101 // 目标要读取101条记录
        val pageSizeLimit = 10 // 每次读10条
        val metrics = LoopUtil.LoopMetrics(startTime = System.currentTimeMillis())
        val queryLimit = min(pageSizeLimit, size - inputVo.data.size)
        do {
            val m2 = LoopUtil.doLoop(inputVo) { loopVo ->
                val dbRecs = queryFakeDB(loopVo.id, queryLimit, mockTime)
                dbRecs.ifEmpty {
                    loopVo.finish = true
                    return@doLoop
                }.forEach { rec ->
                    loopVo.data.add(rec)
                    loopVo.id = max(loopVo.id, rec.id)
                    if (size - loopVo.data.size == 0) { // 拿到想要的数据条数
                        loopVo.finish = true
                        return@doLoop
                    }
                }
                println("nextRecId: ${loopVo.id}, dbRecs: ${dbRecs.size}, datas: ${loopVo.data.size}")
            }
            println("************************************************************************************************")
            println("LOOP $m2")
            println("************************************************************************************************")
            metrics.add(m2)
        } while (!inputVo.finish)

        assertEquals(size, inputVo.data.size)
        assertEquals(size / pageSizeLimit + 1, metrics.loopCount)
        return metrics
    }

    private fun queryFakeDB(startDBRecId: Long, limit: Int, mockTime: Long): List<FakeStuPojo> {
        return if (startDBRecId >= userDataBase.size) {
            emptyList()
        } else {
            val endIdx = startDBRecId + min(userDataBase.size - startDBRecId.toInt(), limit)
            println("DBQuery ======= startDBRecId: $startDBRecId, endIdx: $endIdx, limit: $limit ======= ")
            Thread.sleep(mockTime) // 假设数据库查询耗时mockTime 毫秒
            // 这里演示假装在查询数据库，假设id都是连续的（不重要，这里只是展示）
            // select * from tableX where id > $startDBRecId order by id asc limit $limit
            userDataBase.subList(startDBRecId.toInt(), endIdx.toInt())
        }
    }

    companion object {

        data class FakeStuPojo(
            val id: Long,
            val userName: String,
            val age: Int
        )

        private const val NAME_RANDOM_CHAR = "abcdefghijklmnopqrstuvwxyz"
        private const val MIN_NAME_LENGTH = 5

        fun randomGenCollegeStuPojo(id: Long): FakeStuPojo {
            val random = Random(id)
            val nameLength = random.nextInt(MIN_NAME_LENGTH, NAME_RANDOM_CHAR.length) // 名称最短5，最长26
            var cnt = 0
            val name = StringBuilder(nameLength)
            do {
                name.append(NAME_RANDOM_CHAR[random.nextInt(NAME_RANDOM_CHAR.length)])
            } while (++cnt < nameLength)
            return FakeStuPojo(
                id = id,
                userName = name.toString(),
                age = random.nextInt(20, 26) // 年龄20-26岁
            )
        }
    }

    @Test
    fun testSetIfEmptyReturn() {
        println(ceil(-1f / 1.5f))
        val data = mutableSetOf<String>()
        data.add("1")
        testSetIfEmptyReturn0(data)
        assertEquals(0, data.size)
        data.add("2")
        data.add("2") // dup
        testSetIfEmptyReturn2(data)
        assertEquals(2, data.size)
    }

    private fun testSetIfEmptyReturn0(data: MutableCollection<String>) {
        val set = mutableListOf<String>()
        set.ifEmpty {
            data.clear()
            return
        }.forEach {
            data.add(it)
        }
        println("end, if empty won't see it. size: ${data.size}")
    }

    private fun testSetIfEmptyReturn2(data: MutableCollection<String>) {
        val set = mutableListOf<String>()
        set.add("1")
        set.add("2")
        set.add("1") // dup
        set.ifEmpty {
            data.clear()
            return
        }.forEach {
            data.add(it)
        }
        println("end, if empty won't see it. size: ${data.size}")
    }
}
