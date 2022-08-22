/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.devops.common.pipeline.element

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@Deprecated("作废，由其他团队负责")
@ApiModel("wetest原子", description = WetestElement.classType)
data class WetestElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "wetest原子",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("任务id", required = true)
    val taskId: Int,
    @ApiModelProperty(
        "测试类型(install（快速兼容测试）、unity（Unity兼容测试）、app（app功能测试）、ios(ios测试)、othermonkey(第三方脚本测试))", required = true
    )
    val testType: String,
    @ApiModelProperty("源文件(PIPELINE-流水线仓库, CUSTOMIZE-自定义仓库)", required = true)
    val sourceType: String,
    @ApiModelProperty("待测试应用路径（不支持支持路径通配，只支持单个路径）", required = true)
    val sourcePath: String,
    @ApiModelProperty("脚本文件仓库类型(PIPELINE-流水线仓库, CUSTOMIZE-自定义仓库)", required = false)
    val scriptSourceType: String?,
    @ApiModelProperty("待测试应用脚本路径（不支持路径通配，只支持单个路径）", required = false)
    val scriptPath: String?,
    @ApiModelProperty("测试脚本类型", required = false)
    val scriptType: String?,
    @ApiModelProperty("账号文件仓库类型(PIPELINE-流水线仓库, CUSTOMIZE-自定义仓库)", required = false)
    val accountFileSourceType: String?,
    @ApiModelProperty("测试账号文件", required = false)
    val testAccountFile: String?,
    @ApiModelProperty("执行方式(ASYNC-异步， SYNC-同步)", required = true)
    val synchronized: String,
    @ApiModelProperty("通知方式", required = true)
    val notifyType: Int,
    @ApiModelProperty("运行多开登录 false-不允许 true-允许", required = false)
    val multiLogin: Boolean?,
    @ApiModelProperty("团队ID，需要向WeTest小助手咨询查询ID", required = false)
    val weTestProjectId: String?,
    @ApiModelProperty("预装apk仓库类型", required = false)
    val preTestArchiveType: String?,
    @ApiModelProperty("预装apk仓库路径，多个可以逗号分隔", required = false)
    val preTestApkFiles: String?
) : Element(name, id, status) {
    companion object {
        const val classType = "wetestElement"
    }

    override fun getTaskAtom(): String {
        return "wetestTaskAtom"
    }

    override fun getClassType() = classType
}
