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

@Deprecated("已作废")
@ApiModel("xcode构建任务二(支持xcode10)", description = XcodeBuildElement2.classType)
data class XcodeBuildElement2(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "xcode构建任务",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("xcode工程代码根目录", required = true)
    val project: String = "",
    @ApiModelProperty("证书id", required = true)
    val certId: String = "",
    @ApiModelProperty("xcodebuild的-scheme参数值", required = true)
    val scheme: String = "",
    @ApiModelProperty("xcodebuild的-configuration参数值", required = false)
    val configuration: String = "",
    @ApiModelProperty("生成的IPA包存放目录， 默认是result", required = false)
    val ipaPath: String = "result",
    @ApiModelProperty("xcodebuild对应的method(默认Development，另外还有enterprise，app-store，ad-hoc，package，development-id，mac-application)", required = false)
    val method: String = ""
) : Element(name, id, status) {
    companion object {
        const val classType = "xcodeBuild2"
    }

    override fun getClassType() = classType
}
