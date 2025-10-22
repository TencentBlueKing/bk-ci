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
import com.tencent.devops.common.pipeline.pojo.element.trigger.enums.PathFilterType
import com.tencent.devops.common.pipeline.utils.TriggerElementPropUtils.staffInput
import com.tencent.devops.common.pipeline.utils.TriggerElementPropUtils.vuexInput
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "SVN仓库代码提交触发", description = CodeSVNWebHookTriggerElement.classType)
data class CodeSVNWebHookTriggerElement(
    @get:Schema(title = "任务名称", required = true)
    override val name: String = "SVN事件触发",
    @get:Schema(title = "id", required = false)
    override var id: String? = null,
    @get:Schema(title = "状态", required = false)
    override var status: String? = null,
    @get:Schema(title = "仓库ID", required = true)
    val repositoryHashId: String? = null,
    @get:Schema(title = "路径过滤类型", required = true)
    val pathFilterType: PathFilterType? = PathFilterType.NamePrefixFilter,
    @get:Schema(title = "相对路径", required = true)
    val relativePath: String?,
    @get:Schema(title = "排除的路径", required = false)
    val excludePaths: String?,
    @get:Schema(title = "用户黑名单", required = false)
    val excludeUsers: List<String>?,
    @get:Schema(title = "用户白名单", required = false)
    val includeUsers: List<String>?,
    @get:Schema(title = "新版的svn原子的类型")
    val repositoryType: TriggerRepositoryType? = null,
    @get:Schema(title = "新版的svn代码库名")
    val repositoryName: String? = null
) : WebHookTriggerElement(name, id, status) {
    companion object {
        const val classType = "codeSVNWebHookTrigger"
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
        return listOfNotNull(
            vuexInput(name = "relativePath", value = relativePath),
            vuexInput(name = "excludePaths", value = excludePaths),
            staffInput(name = "includeUsers", value = includeUsers),
            staffInput(name = "excludeUsers", value = excludeUsers)
        )
    }
}
