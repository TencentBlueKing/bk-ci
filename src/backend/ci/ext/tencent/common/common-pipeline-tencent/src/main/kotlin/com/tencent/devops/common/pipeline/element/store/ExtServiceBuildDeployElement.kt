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

package com.tencent.devops.common.pipeline.element.store

import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("扩展服务构建部署", description = ExtServiceBuildDeployElement.classType)
data class ExtServiceBuildDeployElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "扩展服务发布归档",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("扩展服务标识", required = true)
    val serviceCode: String = "\${serviceCode}",
    @ApiModelProperty("扩展服务版本号", required = true)
    val serviceVersion: String = "\${version}",
    @ApiModelProperty("扩展服务发布包名称", required = true)
    val packageName: String = "\${packageName}",
    @ApiModelProperty("扩展服务发布包所在相对路径", required = true)
    val filePath: String = "\${filePath}",
    @ApiModelProperty("目标", required = false)
    val destPath: String = "\${serviceCode}/\${version}/\${packageName}"
) : Element(name, id, status) {

    companion object {
        const val classType = "extServiceBuildDeploy"
    }

    override fun getClassType() = classType
}
