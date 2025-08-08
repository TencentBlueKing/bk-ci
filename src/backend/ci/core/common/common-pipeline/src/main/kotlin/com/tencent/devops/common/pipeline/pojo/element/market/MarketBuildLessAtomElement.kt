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

package com.tencent.devops.common.pipeline.pojo.element.market

import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.transfer.PreStep
import com.tencent.devops.common.pipeline.utils.TransferUtil
import io.swagger.v3.oas.annotations.media.Schema
import org.json.JSONObject

@Schema(title = "流水线模型-插件市场第三方无构建环境类插件", description = MarketBuildLessAtomElement.classType)
data class MarketBuildLessAtomElement(
    @get:Schema(title = "任务名称", required = true)
    override val name: String = "任务名称由用户自己填写",
    @get:Schema(title = "id将由后台生成", required = false)
    override var id: String? = null,
    @get:Schema(title = "状态", required = false)
    override var status: String? = null,
    @get:Schema(title = "插件的唯一标识", required = true)
    @get:JvmName("getAutoAtomCode")
    var atomCode: String = "",
    @get:Schema(title = "插件版本", required = false)
    override var version: String = "1.*",
    @get:Schema(title = "用户自定义ID", required = false)
    override var stepId: String? = null,
    @get:Schema(title = "用户自定义环境变量（插件运行时写入环境）", required = false)
    override var customEnv: List<NameAndValue>? = null,
    @get:Schema(title = "插件参数数据", required = true)
    var data: Map<String, Any> = mapOf()
) : Element(name, id, status) {

    companion object {
        const val classType = "marketBuildLess"
    }

    override fun getAtomCode(): String {
        return atomCode
    }

    override fun transferYaml(defaultValue: JSONObject?): PreStep {
        val input = data["input"] as Map<String, Any>? ?: emptyMap()
        return PreStep(
            name = name,
            id = stepId,
            uses = "${getAtomCode()}@$version",
            with = TransferUtil.simplifyParams(defaultValue, input).ifEmpty { null }
        )
    }

    override fun transferSensitiveParam(params: List<String>) {
        val input = data["input"] as? MutableMap<String, Any>? ?: return
        params.forEach {
            input[it] = "******"
        }
    }

    override fun getClassType() = classType
}
