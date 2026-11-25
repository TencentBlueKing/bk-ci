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
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
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
 *    取回后仅在本次转换的内存中保留联动(rely)与默认值(default)部分，不做本地缓存。
 * 2. Element 转换后（AFTER）：按联动配置求值，把当前被隐藏的参数从 yaml step 的 with 中移除。
 *
 * 清理只是锦上添花的能力，两个切面都不会向外抛异常：内部异常一律记日志后放弃本次清理，
 * 不得影响转换主流程。
 *
 * 预取以插件为粒度：只有取到参数定义的插件才会参与清理，预取失败的插件在 Element 阶段跳过，
 * 不再按插件逐个请求 store，避免把一次失败放大成 N 次请求；
 * 批量请求整体失败（异常或 store 返回空）时所有插件都不做清理。
 *
 * 只处理第三方插件元素：构建环境类 MarketBuildAtomElement 与无构建环境类 MarketBuildLessAtomElement，
 * 两者是平级的 Element 实现，没有共同父类，需分别判定。
 *
 * 只处理配置了 rely 的参数，且只修改 yaml 侧结果，不改动入参 model。
 */
class RelyParamFilterAspect(private val transferCacheService: TransferCacheService) {

    companion object {
        private val logger = LoggerFactory.getLogger(RelyParamFilterAspect::class.java)

        // 插件参数定义中描述显隐联动的字段名。
        private const val KEY_RELY = "rely"

        // 插件参数定义中的默认值字段名。
        private const val KEY_DEFAULT = "default"
    }

    // key: 插件标识@版本，value: 参数名 -> rely 配置，仅本次转换期间使用。
    // 预取失败的插件不会写入该 map，Element 阶段据此跳过其清理。
    private val atomInputRelyMap = ConcurrentHashMap<String, Map<String, Any>>()

    // key: 插件标识@版本，value: 参数默认值，仅本次转换期间使用。
    // 预取失败的插件不会写入该 map。
    private val atomDefaultValueMap = ConcurrentHashMap<String, JSONObject>()

    private data class AtomElementInfo(
        val atomCode: String,
        val version: String,
        // 插件参数数据，取 data["input"]。
        val input: Any?
    ) {
        val key: String get() = "$atomCode@$version"
    }

    fun modelAspect(): IPipelineTransferAspectModel = object : IPipelineTransferAspectModel {
        override fun before(jp: PipelineTransferJoinPoint) {
            kotlin.runCatching { prefetch(jp.model()) }.onFailure {
                logger.warn("prefetch atom input props failed, skip filtering", it)
            }
        }
    }

    fun elementAspect(): IPipelineTransferAspectElement = object : IPipelineTransferAspectElement {
        override fun after(jp: PipelineTransferJoinPoint) {
            kotlin.runCatching { filter(jp.modelElement(), jp.yamlPreStep()) }.onFailure {
                logger.warn("filter atom rely invalid params failed, skip it", it)
            }
        }
    }

    private fun prefetch(model: Model?) {
        if (model == null || model.stages.isEmpty()) return

        // 同一插件可能出现在多个 step 中，按「插件标识@版本」去重后再请求，避免请求体膨胀。
        val paramMap = LinkedHashMap<String, ElementThirdPartySearchParam>()
        for ((containers) in model.stages) {
            for (container in containers) {
                container.elements.mapNotNull { atomElementOf(it) }.forEach { atom ->
                    paramMap[atom.key] = ElementThirdPartySearchParam(atom.atomCode, atom.version)
                }
            }
        }
        if (paramMap.isEmpty()) return

        // 一次性批量请求 store 获取参数定义，取回后仅保留本次清理需要的数据，不做本地缓存。
        val inputProps = transferCacheService.getAtomInputProps(paramMap.values.toList())
        // 预取整体失败（异常或返回空）时不写入任何数据，Element 阶段所有插件都不做清理。
        if (inputProps.isEmpty()) {
            logger.warn("prefetch atom input props empty, skip filtering|size:${paramMap.size}")
            return
        }

        // 仅取到参数定义的插件参与清理，预取失败的插件不写入内存，Element 阶段按插件粒度跳过。
        paramMap.keys.filterNot { inputProps.containsKey(it) }.let { failedKeys ->
            if (failedKeys.isNotEmpty()) {
                logger.warn("prefetch atom input props failed, skip filtering them|keys:$failedKeys")
            }
        }
        inputProps.forEach { (key, props) ->
            atomInputRelyMap[key] = extractRely(props)
            atomDefaultValueMap[key] = JSONObject(extractDefault(props))
        }
    }

    /**
     * 取出第三方插件元素的标识与参数，非插件元素返回 null。
     *
     * 构建环境类(marketBuild)与无构建环境类(marketBuildLess)插件是平级的 Element 实现，
     * 没有共同父类，需分别判定；两者的参数都放在各自的 data["input"] 中。
     */
    private fun atomElementOf(element: Element?): AtomElementInfo? = when (element) {
        is MarketBuildAtomElement -> AtomElementInfo(
            atomCode = element.getAtomCode(),
            version = element.version,
            input = element.data["input"]
        )

        is MarketBuildLessAtomElement -> AtomElementInfo(
            atomCode = element.getAtomCode(),
            version = element.version,
            input = element.data["input"]
        )

        else -> null
    }

    /**
     * 从插件参数定义中提取各参数的联动配置，只保留配置了 rely 的参数。
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
     * 从插件参数定义中提取各参数的默认值。
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
        val atom = atomElementOf(element) ?: return
        if (step !is PreStep) return

        // with 由 TransferUtil.simplifyParams 生成，为可变 map。
        val with = step.with as? MutableMap<String, Any?> ?: return
        if (with.isEmpty()) return

        // 预取未取到该插件的参数定义（获取失败）时不清理，也不再回源 store。
        val key = atom.key
        val relyMap = atomInputRelyMap[key] ?: return
        if (relyMap.isEmpty()) return

        // 与默认值相同的参数不会出现在 with 中，求值时需还原插件表单的全量值。
        val input = atom.input as? Map<String, Any?>
        val values = TransferUtil.mixParams(atomDefaultValueMap[key], input)
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
}
