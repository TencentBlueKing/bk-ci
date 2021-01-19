package com.tencent.devops.process.engine.atom.plugin

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.element.atom.BeforeDeleteParam
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.plugin.codecc.CodeccApi
import com.tencent.devops.process.plugin.ElementBizPlugin
import com.tencent.devops.process.plugin.annotation.ElementBiz

@ElementBiz
class MarketBuildAtomElementBizPlugin constructor(
    private val codeccApi: CodeccApi
) : ElementBizPlugin<MarketBuildAtomElement> {

    override fun elementClass(): Class<MarketBuildAtomElement> {
        return MarketBuildAtomElement::class.java
    }

    override fun afterCreate(
        element: MarketBuildAtomElement,
        projectId: String,
        pipelineId: String,
        pipelineName: String,
        userId: String,
        channelCode: ChannelCode,
        create: Boolean
    ) {}

    override fun beforeDelete(element: MarketBuildAtomElement, param: BeforeDeleteParam) {
        val inputMap = element.data["input"] as Map<*, *>
        MarketBuildUtils.beforeDelete(inputMap, element.getAtomCode(), param, codeccApi)
    }

    override fun check(element: MarketBuildAtomElement, appearedCnt: Int) {}
}