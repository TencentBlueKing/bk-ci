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

@Deprecated("已作废, 替代品插件ID：OverSeaCloudStoneTest")
@ApiModel("分发至海外蓝鲸", description = JobCloudsFastPushElement.classType)
data class JobCloudsFastPushElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "混合云版-作业平台-构件分发",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("源类型(CUSTOMIZE, PIPELINE,REMOTE)", required = false)
    val srcType: String = "",
    @ApiModelProperty("源路径", required = false)
    var srcPath: String = "",
    @ApiModelProperty("源节点id", required = false)
    var srcNodeId: String = "",
    @ApiModelProperty("源服务器账户", required = false)
    var srcAccount: String = "",
    @ApiModelProperty("上传最长时间（单位：分钟）,默认10小时", required = false)
    var maxRunningMins: Int = 600,
    @ApiModelProperty("文件上传的目标路径", required = true)
    var targetPath: String = "",
    @ApiModelProperty("目标业务ID", required = true)
    var targetAppId: Int,
    @ApiModelProperty("openState的值", required = true)
    var openState: String = "",
    @ApiModelProperty("目标服务器-指定IP(格式:云区域ID:IP1)，多个ip之间用逗号分隔。", required = true)
    var ipList: String = ""
) : Element(name, id, status) {
    companion object {
        const val classType = "jobCloudsFastPush"
    }

    override fun getTaskAtom() = "jobCloudsFastPushTaskAtom"

    override fun getClassType() = classType
}
