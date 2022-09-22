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

package com.tencent.devops.common.pipeline.pojo.element.market

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("插件发布归档", description = AtomBuildArchiveElement.classType)
data class AtomBuildArchiveElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "插件发布归档",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("插件发布包名称", required = true)
    val packageName: String = "\${packageName}",
    @ApiModelProperty("插件发布包所在相对路径", required = true)
    val filePath: String = "\${filePath}",
    @ApiModelProperty("插件发布包上传至仓库的目标路径", required = true)
    val destPath: String = "\${atomCode}/\${version}/\${packageName}",
    @ApiModelProperty("插件自定义UI前端文件所在相对路径", required = false)
    val frontendFilePath: String? = "\${BK_CI_CUSTOM_FRONTEND_DIST_PATH}",
    @ApiModelProperty("插件自定义UI前端文件上传至仓库的目标路径", required = false)
    val frontendDestPath: String? = "\${atomCode}/\${version}",
    @ApiModelProperty("操作系统名称", required = false)
    val osName: String? = "\${matrixOsName}",
    @ApiModelProperty("操作系统cpu架构", required = false)
    val osArch: String? = "\${matrixOsArch}",
    @ApiModelProperty("是否有可用的操作系统名称配置", required = false)
    val validOsNameFlag: String? = null,
    @ApiModelProperty("是否有可用的操作系统cpu架构配置", required = false)
    val validOsArchFlag: String? = null
) : Element(name, id, status) {

    companion object {
        const val classType = "atomBuildArchive"
    }

    override fun getClassType() = classType
}
