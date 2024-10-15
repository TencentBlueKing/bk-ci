package com.tencent.devops.common.pipeline.pojo.cascade

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
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
        // 链式处理
        for (i in 0 until chain.size - 1) {
            map[chain[i]]!!.children = map[chain[i + 1]]
        }
        return map[chain[0]]!!
    }

    private fun getDefaultValue(prop: BuildFormProperty): Map<String, String> {
        return try {
            val defaultValue =
                JsonUtil.to(prop.defaultValue.toString(), object : TypeReference<Map<String, String>>() {})
            if (!chain.find { !defaultValue.containsKey(it) }.isNullOrBlank()) {
                mapOf()
            } else {
                defaultValue
            }
        } catch (e: JsonProcessingException) {
            logger.warn("parse default value|prop=$prop", e)
            mapOf()
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

