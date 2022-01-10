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

@ApiModel("海外蓝鲸-脚本执行", description = JobCloudsFastExecuteScriptElement.classType)
data class JobCloudsFastExecuteScriptElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "混合云版-作业平台-脚本执行",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("目标业务ID", required = true)
    val targetAppId: Int,
    @ApiModelProperty("脚本内容", required = true)
    val content: String = "",
    @ApiModelProperty("超时时间", required = true)
    var scriptTimeout: Int,
    @ApiModelProperty("脚本参数", required = true)
    var scriptParams: String? = null,
    @ApiModelProperty("脚本参数", required = true)
    var paramSensitive: Boolean,
    @ApiModelProperty("脚本类型", required = true)
    val type: Int,
    @ApiModelProperty("目标机器账户名", required = true)
    var account: String = "",
    @ApiModelProperty("openstate的值", required = true)
    var openState: String
) : Element(name, id, status) {
    companion object {
        const val classType = "jobCloudsFastExecuteScript"
    }

    override fun getTaskAtom(): String {
        return "jobCloudsFastExecuteScriptTaskAtom"
    }

    override fun getClassType() = classType
}
