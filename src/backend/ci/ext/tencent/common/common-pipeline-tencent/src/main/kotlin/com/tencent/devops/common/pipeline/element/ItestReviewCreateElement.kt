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
@ApiModel("ITest-创建审核单", description = ItestReviewCreateElement.classType)
data class ItestReviewCreateElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "ITest创建自测单",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("ITest项目ID", required = true)
    val itestProjectId: String = "",
    @ApiModelProperty("ITest API凭证", required = true)
    val ticketId: String = "",
    @ApiModelProperty("版本类型(默认值为0，其中：'0' => '普通版本', '1' => '紧急版本', '2' => '免测版本')", required = true)
    val versionType: String = "",
    @ApiModelProperty("版本号", required = true)
    val versionName: String = "",
    @ApiModelProperty("基线号", required = true)
    val baselineName: String = "",
    @ApiModelProperty("预计发布时间(时间戳)", required = true)
    val releaseTime: Long,
    @ApiModelProperty("测试说明", required = true)
    val description: String = "",
    @ApiModelProperty("是否自定义归档", required = true)
    val customize: Boolean,
    @ApiModelProperty("待测试包", required = true)
    val targetPath: String = ""
) : Element(name, id, status) {
    companion object {
        const val classType = "itestReviewCreate"
    }

    override fun getTaskAtom(): String {
        return "itestReviewCreateTaskAtom"
    }

    override fun getClassType() = classType
}
