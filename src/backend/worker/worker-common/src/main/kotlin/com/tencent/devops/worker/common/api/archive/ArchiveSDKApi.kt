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

package com.tencent.devops.worker.common.api.archive

import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.api.WorkerRestApiSDK
import java.io.File

interface ArchiveSDKApi : WorkerRestApiSDK {

    /**
     * 归档构件到仓库中自定义路径
     * @param file 构件
     * @param destPath 要上传的文件而指定的自定义路径
     * @param buildVariables 构建变量
     */
    fun uploadCustomize(file: File, destPath: String, buildVariables: BuildVariables)

    /**
     * 归档构件到流水线仓库
     * @param file 构件
     * @param buildVariables 构建变量
     */
    fun uploadPipeline(file: File, buildVariables: BuildVariables)

    /**
     * 下载仓库中指定路径的文件
     *
     * @param uri       下载路径
     * @param destPath  下载后存放的文件
     */
    fun downloadCustomizeFile(uri: String, destPath: File)

    /**
     * 按流水线，构建ID来下载仓库中构件
     * @param pipelineId  流水线id
     * @param buildId     构建id
     * @param uri         下载uri
     * @param destPath    下载后存放的文件
     */
    fun downloadPipelineFile(pipelineId: String, buildId: String, uri: String, destPath: File)

    /**
     * 获取下载地址
     * @param pipelineId        流水线id
     * @param buildId           构建id
     * @param fileType          分流水线构件和自定义归档
     * @param customFilePath    要下载的文件路径，支持如 *.jar 模糊匹配
     * @return 下载地址
     */
    fun getFileDownloadUrls(
        pipelineId: String,
        buildId: String,
        fileType: FileTypeEnum,
        customFilePath: String?
    ): List<String>
}