/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.oci.service

import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.oci.pojo.artifact.OciBlobArtifactInfo

interface OciBlobService {
    /**
     * 上传blob文件
     * 分为两种情况：
     * 1 当digest参数存在时，是使用single post直接上传文件
     * 2 当digest参数不存在时，使用post and put方式上传文件,此接口返回追加uuid
     */
    fun startUploadBlob(artifactInfo: OciBlobArtifactInfo, artifactFile: ArtifactFile)

    /**
     * 根据[artifactInfo]的信息来上传[artifactFile]文件
     */
    fun uploadBlob(artifactInfo: OciBlobArtifactInfo, artifactFile: ArtifactFile)

    /**
     * 根据[artifactInfo]的信息下载blob文件
     */
    fun downloadBlob(artifactInfo: OciBlobArtifactInfo)

    /**
     * 根据[artifactInfo]的信息下载blob文件
     */
    fun deleteBlob(artifactInfo: OciBlobArtifactInfo)
}
