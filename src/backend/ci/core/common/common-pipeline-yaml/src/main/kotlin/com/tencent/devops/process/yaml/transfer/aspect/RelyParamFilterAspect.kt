/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.yaml.transfer.aspect

import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.transfer.IPreStep
import com.tencent.devops.common.pipeline.pojo.transfer.PreStep
import com.tencent.devops.common.pipeline.utils.TransferUtil
import com.tencent.devops.process.yaml.transfer.TransferCacheService
import com.tencent.devops.process.yaml.utils.RelyExpressionEvaluator
import com.tencent.devops.store.pojo.atom.ElementThirdPartySearchParam
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * 按插件参数联动配置(rely)清理无效参数的转换切面，仅用于 MODEL2YAML 方向。
 *
 * 背景：插件参数存在显隐联动（如枚举选 A 展示参数 p2，选 B 展示参数 p3），
 * UI 切换选项后被隐藏的参数值仍留在 model 中，转出的 YAML 会一直携带这些无效参数。
 *
 * 处理分两步：
 * 1. Model 转换前（BEFORE）：扫描整个 Model 收集所涉及的插件，一次性批量请求 store 获取参数定义，
 *    取回后仅在本次转换的内存中保留联动(rely)部分，不做本地缓存。
 * 2. Element 转换后（AFTER）：按联动配置求值，把当前被隐藏的参数从 yaml step 的 with 中移除。
 *
 * 只处理配置了 rely 的参数，且只修改 yaml 侧结果，不改动入参 model。
 */
class RelyParamFilterAspect(private val transferCacheService: TransferCacheService) {

    companion object {
        private val logger = LoggerFactory.getLogger(RelyParamFilterAspect::class.java)
        // 插件参数定义中描述显隐联动的字段名
        private const val KEY_RELY = "rely"
        // 插件参数定义中的默认值字段名
        private const val KEY_DEFAULT = "default"
    }

    // key: 插件标识@版本，value: 参数名 -> rely 配置，仅本次转换期间使用。
    private val atomInputRelyMap = ConcurrentHashMap<String, Map<String, Any>>()
    // key: 插件标识@版本，value: 参数默认值，仅本次转换期间使用。
    private val atomDefaultValueMap = ConcurrentHashMap<String, JSONObject>()

    fun modelAspect(): IPipelineTransferAspectModel = object : IPipelineTransferAspectModel {
        override fun before(jp: PipelineTransferJoinPoint) {
            prefetch(jp.model())
        }
    }

    fun elementAspect(): IPipelineTransferAspectElement = object : IPipelineTransferAspectElement {
        override fun after(jp: PipelineTransferJoinPoint) {
            filter(jp.modelElement(), jp.yamlPreStep())
        }
    }

    private fun prefetch(model: Model?) {
        val params = mutableListOf<ElementThirdPartySearchParam>()
        model?.stages?.forEach { stage ->
            stage.containers.forEach { container ->
                container.elements.forEach { element ->
                    if (element is MarketBuildAtomElement) {
                        params.add(ElementThirdPartySearchParam(element.getAtomCode(), element.version))
                    }
                }
            }
        }
        if (params.isEmpty()) return
        kotlin.runCatching {
            // 一次性批量请求 store 获取参数定义，取回后仅保留本次清理需要的数据，不做本地缓存
            transferCacheService.getAtomInputProps(params).forEach { (key, inputProps) ->
                atomInputRelyMap[key] = extractRely(inputProps)
                atomDefaultValueMap[key] = JSONObject(extractDefault(inputProps))
            }
        }.onFailure {
            logger.warn("prefetch atom input props failed, size:${params.size}", it)
        }
    }

    /**
     * 从插件参数定义中提取各参数的联动配置，只保留配置了 rely 的参数
     */
    private fun extractRely(inputProps: Map<String, Any>): Map<String, Any> {
        val relyMap = mutableMapOf<String, Any>()
        inputProps.forEach { (paramKey, paramDefine) ->
            val rely = (paramDefine as? Map<*, *>)?.get(KEY_RELY)
            if (rely != null) {
                relyMap[paramKey] = rely
            }
        }
        return relyMap
    }

    /**
     * 从插件参数定义中提取各参数的默认值
     */
    private fun extractDefault(inputProps: Map<String, Any>): Map<String, Any> {
        val defaultValue = mutableMapOf<String, Any>()
        inputProps.forEach { (paramKey, paramDefine) ->
            val value = (paramDefine as? Map<*, *>)?.get(KEY_DEFAULT)
            if (value != null) {
                defaultValue[paramKey] = value
            }
        }
        return defaultValue
    }

    @Suppress("UNCHECKED_CAST")
    private fun filter(element: Element?, step: IPreStep?) {
        if (element !is MarketBuildAtomElement || step !is PreStep) return

        // with 由 TransferUtil.simplifyParams 生成，为可变 map。
        val with = step.with as? MutableMap<String, Any?> ?: return
        if (with.isEmpty()) return

        val key = "${element.getAtomCode()}@${element.version}"
        val relyMap = getAtomInputRely(key)
        if (relyMap.isEmpty()) return
        // 与默认值相同的参数不会出现在 with 中，求值时需还原插件表单的全量值
        val input = element.data["input"] as? Map<String, Any?>
        val values = TransferUtil.mixParams(getAtomDefaultValue(key), input)
        val invalidParams = mutableListOf<String>()
        relyMap.forEach { (param, rely) ->
            if (with.containsKey(param) && !RelyExpressionEvaluator.satisfy(rely, values)) {
                invalidParams.add(param)
            }
        }
        if (invalidParams.isEmpty()) return
        invalidParams.forEach { with.remove(it) }
        logger.info("filter rely invalid params|$key|size:${invalidParams.size}")
    }

    /**
     * 取指定插件的联动配置：优先用 Model 阶段预取的结果，
     * 缺失时（如未走 Model 阶段的单插件转换场景）再发一次请求，同样只保留本次需要的数据
     */
    private fun getAtomInputRely(key: String): Map<String, Any> {
        if (!atomInputRelyMap.containsKey(key)) {
            loadAtomInputProps(key)
        }
        return atomInputRelyMap[key] ?: emptyMap()
    }

    /**
     * 取插件参数默认值：优先用本次已获取的参数定义中的默认值，
     * 缺失时回退到通用的默认值查询（用于还原插件表单全量值）
     */
    private fun getAtomDefaultValue(key: String): JSONObject =
        atomDefaultValueMap[key] ?: transferCacheService.getAtomDefaultValue(key)

    private fun loadAtomInputProps(key: String) {
        val parts = key.split("@", limit = 2)
        if (parts.size != 2) return
        kotlin.runCatching {
            val inputProps = transferCacheService.getAtomInputProps(
                listOf(ElementThirdPartySearchParam(parts[0], parts[1]))
            )[key] ?: emptyMap()
            atomInputRelyMap[key] = extractRely(inputProps)
            atomDefaultValueMap[key] = JSONObject(extractDefault(inputProps))
        }.onFailure {
            logger.warn("get atom input props failed|$key", it)
        }
    }
}
