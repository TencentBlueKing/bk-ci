package com.tencent.devops.process.engine.atom.plugin

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.atom.BeforeDeleteParam
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.plugin.codecc.CodeccApi
import com.tencent.devops.process.plugin.ElementBizPlugin
import com.tencent.devops.process.plugin.annotation.ElementBiz

@ElementBiz
class MarketBuildLessAtomElementBizPlugin constructor(
    private val codeccApi: CodeccApi
) : ElementBizPlugin<MarketBuildLessAtomElement> {

    override fun elementClass(): Class<MarketBuildLessAtomElement> {
        return MarketBuildLessAtomElement::class.java
    }

    override fun afterCreate(element: MarketBuildLessAtomElement, projectId: String, pipelineId: String, pipelineName: String, userId: String, channelCode: ChannelCode, create: Boolean) {
    }

    override fun beforeDelete(element: MarketBuildLessAtomElement, param: BeforeDeleteParam) {
        val inputMap = element.data["input"] as Map<*, *>
        MarketBuildUtils.beforeDelete(inputMap, element.getAtomCode(), param, codeccApi)
    }

    override fun check(element: MarketBuildLessAtomElement, appearedCnt: Int) {
    }
}