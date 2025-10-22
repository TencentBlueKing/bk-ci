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
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.pipeline.pojo.transfer.PreStep
import io.swagger.v3.oas.annotations.media.Schema
import java.net.URLEncoder
import org.json.JSONObject

@Schema(title = "脚本任务（linux和macOS环境）", description = LinuxScriptElement.classType)
data class LinuxScriptElement(
    @get:Schema(title = "任务名称", required = true)
    override val name: String = "执行Linux脚本",
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
    @get:Schema(title = "脚本类型", required = true)
    val scriptType: BuildScriptType,
    @get:Schema(title = "脚本内容", required = true)
    val script: String,
    @get:Schema(title = "某次执行为非0时（失败）是否继续执行脚本", required = false)
    val continueNoneZero: Boolean?,
    @get:Schema(title = "启用脚本执行失败时归档的文件", required = false)
    val enableArchiveFile: Boolean? = false,
    @get:Schema(title = "脚本执行失败时归档的文件", required = false)
    val archiveFile: String? = null,
    @get:Schema(title = "附加参数", required = false)
    override var additionalOptions: ElementAdditionalOptions? = null
) : Element(name, id, status, additionalOptions = additionalOptions) {

    companion object {
        const val classType = "linuxScript"
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
        // bat插件上的
        uses = "${getAtomCode()}@$version",
        with = bashParams()
    )

    private fun bashParams(): Map<String, Any> {
        val res = mutableMapOf<String, Any>(LinuxScriptElement::script.name to script)
        if (continueNoneZero == true) {
            res[LinuxScriptElement::continueNoneZero.name] = true
        }
        if (enableArchiveFile == true && archiveFile != null) {
            res[LinuxScriptElement::enableArchiveFile.name] = true
            res[LinuxScriptElement::archiveFile.name] = archiveFile
        }
        return res
    }

    override fun getClassType() = classType
}
