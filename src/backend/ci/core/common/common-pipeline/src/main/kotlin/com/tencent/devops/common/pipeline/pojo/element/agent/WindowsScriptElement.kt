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

package com.tencent.devops.common.pipeline.pojo.element.agent

import com.tencent.devops.common.pipeline.NameAndValue
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.enums.CharsetType
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.transfer.PreStep
import io.swagger.v3.oas.annotations.media.Schema
import java.net.URLEncoder
import org.json.JSONObject

@Schema(title = "脚本任务（windows环境）", description = WindowsScriptElement.classType)
data class WindowsScriptElement(
    @get:Schema(title = "任务名称", required = true)
    override val name: String = "执行Windows的bat脚本",
    @get:Schema(title = "id", required = false)
    override var id: String? = null,
    @get:Schema(title = "状态", required = false)
    override var status: String? = null,
    @get:Schema(title = "用户自定义ID", required = false)
    override var stepId: String? = null,
    @get:Schema(title = "用户自定义环境变量（插件运行时写入环境）", required = false)
    override var customEnv: List<NameAndValue>? = null,
    @get:Schema(title = "FAQ url链接", required = false)
    val errorFAQUrl: String? = null,
    @get:Schema(title = "脚本内容", required = true)
    val script: String,
    @get:Schema(title = "脚本类型", required = true)
    val scriptType: BuildScriptType,
    @get:Schema(title = "字符集类型", required = false)
    val charsetType: CharsetType? = null
) : Element(name, id, status) {

    companion object {
        const val classType = "windowsScript"
    }

    override fun genTaskParams(): MutableMap<String, Any> {
        val mutableMap = super.genTaskParams()
        // 帮助转化
        mutableMap["script"] = URLEncoder.encode(script, "UTF-8")
        return mutableMap
    }

    override fun transferYaml(defaultValue: JSONObject?): PreStep = PreStep(
        name = name,
        id = stepId,
        uses = "${getAtomCode()}@$version",
        with = batchParams()
    )

    private fun batchParams(): Map<String, Any> {
        val res = mutableMapOf<String, Any>(WindowsScriptElement::script.name to script)
        if (charsetType != null && charsetType != CharsetType.DEFAULT) {
            res[WindowsScriptElement::charsetType.name] = charsetType.name
        }
        return res
    }

    override fun getClassType() = classType
}
