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

package com.tencent.devops.common.pipeline.pojo.element.trigger

import com.tencent.devops.common.api.enums.TriggerRepositoryType
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.element.ElementProp
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.CodeEventType
import com.tencent.devops.common.pipeline.utils.TriggerElementPropUtils.vuexInput
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "p4事件触发", description = CodeP4WebHookTriggerElement.classType)
data class CodeP4WebHookTriggerElement(
    @get:Schema(title = "任务名称", required = true)
    override val name: String = "P4事件触发",
    @get:Schema(title = "id", required = false)
    override var id: String? = null,
    @get:Schema(title = "状态", required = false)
    override var status: String? = null,
    @get:Schema(title = "数据", required = true)
    val data: CodeP4WebHookTriggerData
) : WebHookTriggerElement(name, id, status) {
    companion object {
        const val classType = "codeP4WebHookTrigger"
    }

    override fun getClassType() = classType

    override fun findFirstTaskIdByStartType(startType: StartType): String {
        return if (startType.name == StartType.WEB_HOOK.name) {
            this.id!!
        } else {
            super.findFirstTaskIdByStartType(startType)
        }
    }

    // 增加条件这里也要补充上,不然代码库触发器列表展示会不对
    override fun triggerCondition(): List<ElementProp> {
        with(data.input) {
            val props = when (eventType) {
                CodeEventType.CHANGE_COMMIT, CodeEventType.CHANGE_SUBMIT, CodeEventType.CHANGE_CONTENT -> {
                    listOf(
                        vuexInput(name = "includePaths", value = includePaths),
                        vuexInput(name = "excludePaths", value = excludePaths)
                    )
                }

                CodeEventType.SHELVE_COMMIT, CodeEventType.SHELVE_SUBMIT, CodeEventType.SHELVE_DELETE -> {
                    listOf(
                        vuexInput(name = "includePaths", value = includePaths),
                        vuexInput(name = "excludePaths", value = excludePaths)
                    )
                }

                else ->
                    emptyList()
            }
            return props.filterNotNull()
        }
    }
}

data class CodeP4WebHookTriggerData(
    val input: CodeP4WebHookTriggerInput
)

data class CodeP4WebHookTriggerInput(
    @get:Schema(title = "仓库ID", required = true)
    val repositoryHashId: String? = null,
    @get:Schema(title = "新版的git原子的类型")
    val repositoryType: TriggerRepositoryType? = null,
    @get:Schema(title = "新版的git代码库名")
    val repositoryName: String? = null,
    @get:Schema(title = "用于包含的路径", required = false)
    val includePaths: String?,
    @get:Schema(title = "用于排除的路径", required = false)
    val excludePaths: String?,
    @get:Schema(title = "事件类型", required = false)
    val eventType: CodeEventType?
)
