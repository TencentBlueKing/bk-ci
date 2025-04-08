package com.tencent.devops.common.util

import com.tencent.devops.common.util.LoopUtil.DEFAULT_THRESHOLD_COUNT
import com.tencent.devops.common.util.LoopUtil.DEFAULT_THRESHOLD_MILLS
import com.tencent.devops.common.util.LoopUtil.SLEEP_INTERVAL
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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

    @Test
    fun testCheckThresholdCount() {
        val expectedThrowMsg = "thresholdCount must be > 1 and < $DEFAULT_THRESHOLD_COUNT"
        assertEquals(
            /* expected = */ expectedThrowMsg,
            /* actual = */ assertThrows<IllegalArgumentException> {
            LoopUtil.LoopVo<Long, MutableList<FakeStuPojo>>(
                thresholdCount = -1, // 不合理
                id = 0L,
                data = mutableListOf()
            ).check()
        }.message
        )
        assertEquals(
            /* expected = */ expectedThrowMsg,
            /* actual = */
            assertThrows<IllegalArgumentException> {
                LoopUtil.LoopVo<Long, MutableList<FakeStuPojo>>(
                    thresholdCount = DEFAULT_THRESHOLD_COUNT + 1, // 不合理超过
                    id = 0L,
                    data = mutableListOf()
                ).check()
            }.message
        )
        assertEquals(
            /* expected = */ expectedThrowMsg,
            /* actual = */
            assertThrows<IllegalArgumentException> {
                LoopUtil.LoopVo<Long, MutableList<FakeStuPojo>>(
                    thresholdCount = 1, // 不合理， 仅1次无需要用此循环
                    id = 0L,
                    data = mutableListOf()
                ).check()
            }.message
        )
    }

    @Test
    fun testCheckThresholdMills() {
        val expectedThrowMsg = "thresholdMills must be > 0 and < $DEFAULT_THRESHOLD_MILLS"
        assertEquals(
            /* expected = */ expectedThrowMsg,
            /* actual = */
            assertThrows<IllegalArgumentException> {
                LoopUtil.LoopVo<Long, MutableList<FakeStuPojo>>(
                    thresholdMills = -1L, // 不合理
                    id = 0L,
                    data = mutableListOf()
                ).check()
            }.message
        )
        assertEquals(
            /* expected = */ expectedThrowMsg,
            /* actual = */
            assertThrows<IllegalArgumentException> {
                LoopUtil.LoopVo<Long, MutableList<FakeStuPojo>>(
                    thresholdMills = DEFAULT_THRESHOLD_MILLS + 1, // 不合理 超过
                    id = 0L,
                    data = mutableListOf()
                ).check()
            }.message
        )
    }

    @Test
    fun testCheckSleepIntervalMills() {
        val expectedThrowMsg = "sleepIntervalMills must be > 0 and < $SLEEP_INTERVAL"
        assertEquals(
            /* expected = */ expectedThrowMsg,
            /* actual = */
            assertThrows<IllegalArgumentException> {
                LoopUtil.LoopVo<Long, MutableList<FakeStuPojo>>(
                    sleepIntervalMills = -1,  // 不合理
                    id = 0L,
                    data = mutableListOf()
                ).check()
            }.message
        )
        assertEquals(
            /* expected = */ expectedThrowMsg,
            /* actual = */
            assertThrows<IllegalArgumentException> {
                LoopUtil.LoopVo<Long, MutableList<FakeStuPojo>>(
                    sleepIntervalMills = SLEEP_INTERVAL + 1,  // 不合理 超过
                    id = 0L,
                    data = mutableListOf()
                ).check()
            }.message
        )
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
                age = random.nextInt(20, 26)  // 大学生年龄20-26岁
            )
        }
    }
}
