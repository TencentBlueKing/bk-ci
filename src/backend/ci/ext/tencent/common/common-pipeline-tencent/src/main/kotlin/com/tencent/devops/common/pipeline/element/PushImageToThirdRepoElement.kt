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

@ApiModel("推送镜像到第三方仓库", description = PushImageToThirdRepoElement.classType)
data class PushImageToThirdRepoElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "推送镜像到第三方仓库",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("源镜像名称", required = true)
    val srcImageName: String,
    @ApiModelProperty("源镜像tag", required = true)
    val srcImageTag: String,
    @ApiModelProperty("第三方仓库地址", required = false)
    val repoAddress: String?,
    @ApiModelProperty("凭证ID", required = true)
    val ticketId: String = "",
    @ApiModelProperty("镜像名称", required = true)
    val targetImageName: String,
    @ApiModelProperty("镜像tag", required = true)
    val targetImageTag: String,
    @ApiModelProperty("镜像关联的cmdb的ID", required = false)
    val cmdbId: String?,
    @ApiModelProperty("启用oa验证", required = false)
    val verifyByOa: Boolean?

) : Element(name, id, status) {
    companion object {
        const val classType = "pushImageToThirdRepo"
    }

    override fun getTaskAtom() = "pushImageToThirdRepoTaskAtom"

    override fun getClassType() = classType
}
