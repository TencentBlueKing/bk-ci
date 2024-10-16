package com.tencent.devops.common.pipeline.pojo.cascade

import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import org.slf4j.LoggerFactory

abstract class CascadeParam constructor(
    open val type: BuildFormPropertyType,
    open val chain: List<String>
) {
    fun getProps(prop: BuildFormProperty, projectId: String): BuildCascadeProps {
        if (chain.size < 2 || chain.size != chainHandler().size) {
            // 最少两个链路节点，且节点数和链式处理器数量相等
            throw IllegalArgumentException("chain size must be 2 and equal to chainHandler size")
        }
        val defaultValue = getDefaultValue(prop)
        val map = chain.associateBy({ it }) {
            val propsHandler =
                chainHandler()[it] ?: throw IllegalArgumentException("can not find handler for $it|$type")
            propsHandler.handle(
                key = it,
                defaultValue = defaultValue[it] ?: "",
                projectId = projectId
            )
        }
        // 链式关系处理
        for (i in chain.size - 1 downTo 1) {
            map[chain[i-1]]?.children = map[chain[i]]
        }
        return map[chain.first()]!!
    }

    private fun getDefaultValue(prop: BuildFormProperty): Map<String, String> {
        val defaultValue = prop.defaultValue as Map<String, String>
        return if (!chain.find { !defaultValue.containsKey(it) }.isNullOrBlank()) {
            mapOf()
        } else {
            defaultValue
        }
    }

    abstract fun chainHandler(): Map<String, CascadeParamPropsHandler>

    companion object {
        val logger = LoggerFactory.getLogger(CascadeParam::class.java)
    }
}

interface CascadeParamPropsHandler {
    fun handle(key: String, defaultValue: String, projectId: String): BuildCascadeProps
}
