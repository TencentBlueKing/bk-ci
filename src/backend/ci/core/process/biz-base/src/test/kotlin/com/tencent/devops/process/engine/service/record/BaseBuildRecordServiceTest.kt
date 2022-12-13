package com.tencent.devops.process.engine.service.record

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
import com.tencent.devops.common.pipeline.pojo.time.BuildTimestampType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class BaseBuildRecordServiceTest {

    @Test
    fun mergeTimestamps() {
        val t1 = LocalDateTime.now().timestampmilli() - 6
        val t2 = LocalDateTime.now().timestampmilli()
        val t3 = LocalDateTime.now().timestampmilli() + 6

        val oldTimestamps = mutableMapOf(
            BuildTimestampType.STAGE_CHECK_IN_WAITING to BuildRecordTimeStamp(t1, null),
            BuildTimestampType.STAGE_CHECK_OUT_WAITING to BuildRecordTimeStamp(t2, null)
        )
        val newTimestamps = mutableMapOf(
            BuildTimestampType.STAGE_CHECK_IN_WAITING to
                BuildRecordTimeStamp(null, t3)
        )
        Assertions.assertEquals(
            mapOf(
                BuildTimestampType.STAGE_CHECK_IN_WAITING to BuildRecordTimeStamp(t1, t3),
                BuildTimestampType.STAGE_CHECK_OUT_WAITING to BuildRecordTimeStamp(t2, null)
            ),
            BaseBuildRecordService.mergeTimestamps(newTimestamps, oldTimestamps)
        )
    }
}
