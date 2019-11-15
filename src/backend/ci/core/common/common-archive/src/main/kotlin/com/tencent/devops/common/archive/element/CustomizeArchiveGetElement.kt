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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.archive.element

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("拉取自定义仓库构件", description = CustomizeArchiveGetElement.classType)
data class CustomizeArchiveGetElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "拉取自定义仓库构件",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("待下载文件路径（支持正则表达式，多个用逗号隔开）", required = true)
    val downloadPaths: String = "",
    @ApiModelProperty("下载到本地的路径（默认为当前工作空间）", required = false)
    val destPath: String = "",
    @ApiModelProperty("是否找不到文件报404退出", required = false)
    val notFoundContinue: Boolean? = false
) : Element(name, id, status) {

    companion object {
        const val classType = "customizeArchiveGet"
    }

    override fun getClassType() = classType
}
