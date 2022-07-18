/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.common.scanner.pojo.scanner.arrowhead

import com.tencent.bkrepo.common.scanner.pojo.scanner.Scanner
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Arrowhead扫描器配置")
class ArrowheadScanner(
    override val name: String,
    /**
     * 格式为ArrowheadImageVersion::KnowledgeBaseVervion::StandaloneConfigTemplateVersion
     * 或者ArrowheadImageVersion::KnowledgeBaseVervion
     */
    @ApiModelProperty("扫描器版本")
    override val version: String,
    @ApiModelProperty("扫描器根目录")
    val rootPath: String,
    @ApiModelProperty("扫描器配置文件路径，相对于工作目录")
    val configFilePath: String = DEFAULT_CONFIG_FILE_PATH,
    @ApiModelProperty("扫描结束后是否清理工作目录")
    val cleanWorkDir: Boolean = true,
    @ApiModelProperty("漏洞知识库配置")
    val knowledgeBase: KnowledgeBase,
    @ApiModelProperty("使用的容器镜像")
    val container: ArrowheadDockerImage
) : Scanner(name, TYPE, version) {
    companion object {
        /**
         * 扫描器和漏洞库版本号分隔符
         */
        const val VERSION_SPLIT = "::"
        const val TYPE = "arrowhead"
        const val DEFAULT_CONFIG_FILE_PATH = "/standalone.toml"
    }
}

@ApiModel("arrowhead容器镜像配置")
data class ArrowheadDockerImage(
    @ApiModelProperty("使用的镜像名和版本")
    val image: String,
    @ApiModelProperty("容器启动参数")
    val args: String = "",
    @ApiModelProperty("容器内的工作目录")
    val workDir: String = "/data",
    @ApiModelProperty("输入目录，相对于workDir的路径")
    val inputDir: String = "/package",
    @ApiModelProperty("输出目录，相对于workDir的路径")
    val outputDir: String = "/output"
)

@ApiModel("v2 arrowhead漏洞知识库配置")
data class KnowledgeBase(
    @ApiModelProperty("漏洞知识库地址，例如http://127.0.0.1:1234")
    val endpoint: String,
    @ApiModelProperty("漏洞知识库认证id")
    val secretId: String = "",
    @ApiModelProperty("漏洞知识库认证密钥")
    val secretKey: String = ""
)
