package com.tencent.devops.process.engine.service.record

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
import com.tencent.devops.common.pipeline.pojo.time.BuildTimestampType
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.time.LocalDateTime

internal class BaseBuildRecordServiceTest {

    @Test
    fun mergeTimestamps() {
        val oldTimestamps = mutableMapOf(
            BuildTimestampType.STAGE_CHECK_IN_WAITING to
                BuildRecordTimeStamp(LocalDateTime.now().timestampmilli() - 6, null),
            BuildTimestampType.STAGE_CHECK_OUT_WAITING to
                BuildRecordTimeStamp(LocalDateTime.now().timestampmilli(), null),
        )
        val newTimestamps = mutableMapOf(
            BuildTimestampType.STAGE_CHECK_IN_WAITING to
                BuildRecordTimeStamp(null, LocalDateTime.now().timestampmilli())
        )
        // 针对各时间戳的开始结束时间分别写入，避免覆盖
        val result = mutableMapOf<BuildTimestampType, BuildRecordTimeStamp>()
        result.putAll(oldTimestamps)
        newTimestamps.forEach { (type, new) ->
            val old = oldTimestamps[type]
            result[type] = if (old != null) {
                // 如果时间戳已存在，则将新的值覆盖旧的值
                BuildRecordTimeStamp(
                    startTime = new.startTime ?: old.startTime,
                    endTime = new.endTime ?: old.endTime
                )
            } else {
                // 如果时间戳不存在，则直接新增
                new
            }
        }
        println(result)
    }
}
