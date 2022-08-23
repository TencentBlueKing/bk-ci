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

import com.tencent.devops.common.pipeline.enums.Platform
import com.tencent.devops.common.pipeline.pojo.element.Element
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@Deprecated("作废，由其他团队负责")
@ApiModel("unity3d构建任务", description = Unity3dBuildElement.classType)
data class Unity3dBuildElement(
    @ApiModelProperty("任务名称", required = true)
    override val name: String = "xcode构建任务",
    @ApiModelProperty("id", required = false)
    override var id: String? = null,
    @ApiModelProperty("指定是否采用Debug模式编译", required = false)
    val debug: Boolean? = false,
    @ApiModelProperty("状态", required = false)
    override var status: String? = null,
    @ApiModelProperty("指定工程相对于源码仓库的相对路径", required = false)
    val rootDir: String = "",
    @ApiModelProperty(
        "指定构建脚本的静态入口函数，本插件无需编写C#构建脚本，" +
            "不指定该参数将自动导入/Assets/Editor/SODABuild.cs脚本，否则使用工程代码中的静态入口函数方法", required = false
    )
    val executeMethod: String = "",
    @ApiModelProperty("指定一个构建包目标平台,填IPHONE或者是ANDROID（大写，默认ANDROID）", required = false)
    val platform: List<Platform> = listOf(Platform.ANDROID),

    /**executeMethod不为空时生效**/
    // 下面只针对iPhone
    @ApiModelProperty("指定生成的XCode工程名称，只有在executeMethod为空的时候才生效(默认生成到\${rootDir}/XCodeProject)", required = false)
    val xcodeProjectName: String = "XCodeProject",
    @ApiModelProperty("启用或禁用bitCode模式, 默认不启用", required = false)
    val enableBitCode: Boolean = false,

    // 下面只针对Android
    @ApiModelProperty("指定安卓平台下的证书id", required = false)
    val certId: String = "",
    @ApiModelProperty("指定安卓APK包的输出路径，相对于工作空间根目录的路径（默认bin/android）", required = false)
    val apkPath: String = "",
    @ApiModelProperty("指定安卓APK包的输出名称", required = false)
    val apkName: String = ""
    /**executeMethod不为空时生效**/
) : Element(name, id, status) {
    companion object {
        const val classType = "unity3dBuild"
    }

    override fun getClassType() = classType
}
