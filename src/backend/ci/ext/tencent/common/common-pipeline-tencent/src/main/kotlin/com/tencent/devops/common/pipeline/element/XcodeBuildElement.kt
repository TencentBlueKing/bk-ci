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
@ApiModel("xcode构建任务", description = XcodeBuildElement.classType)
data class XcodeBuildElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "xcode构建任务",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("xcodeproj的相对路径,例如project-aa/Unity-iPhone.xcodeproj", required = true)
    val project: String = "",
    @ApiModelProperty("xcodebuild的-scheme参数值", required = false)
    val scheme: String = "",
    @ApiModelProperty("xcodebuild的-configuration参数值", required = false)
    val configuration: String = "",
    @ApiModelProperty("IOS IPA所在的目录， 默认是result", required = false)
    val iosPath: String = "result",
    @ApiModelProperty("IOS IPA名, 默认是output.ipa", required = false)
    val iosName: String = "output.ipa",
    @ApiModelProperty("XCode 工程的根目录", required = false)
    val rootDir: String = "",
    @ApiModelProperty("启用或禁用bitCode模式, 默认禁止", required = false)
    val enableBitCode: Boolean = false,
    @ApiModelProperty("证书id", required = true)
    val certId: String = "",
    @ApiModelProperty("xcode编译额外参数", required = false)
    val extraParams: String = ""
) : Element(name, id, status) {
    companion object {
        const val classType = "xcodeBuild"
    }

    override fun getClassType() = classType
}
