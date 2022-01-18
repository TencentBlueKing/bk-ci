package com.tencent.devops.common.pipeline.pojo

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.VMBaseOS
import com.tencent.devops.common.pipeline.option.MatrixControlOption
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.atom.ManualReviewParam
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.common.pipeline.pojo.element.matrix.MatrixStatusElement
import com.tencent.devops.common.pipeline.pojo.element.trigger.ManualTriggerElement
import org.junit.Assert
import org.junit.Test

internal class StagePauseCheckTest {

    @Test
    fun parseReviewParams() {
        val check = StagePauseCheck(
            manualTrigger = true,
            reviewParams = mutableListOf(
                ManualReviewParam(key = "p1", value = "111"),
                ManualReviewParam(key = "p2", value = "222")
            )
        )
        val originKeys = check.reviewParams?.map { it.key }?.toList()
        val params = mutableListOf(
            ManualReviewParam(key = "p1", value = "123"),
            ManualReviewParam(key = "p2", value = "222")
        )
        Assert.assertEquals(
            mutableListOf(ManualReviewParam(key = "p1", value = "123")),
            check.parseReviewParams(params)
        )
        Assert.assertEquals(
            check.reviewParams?.map { it.key }?.toList(),
            originKeys
        )
    }
}
