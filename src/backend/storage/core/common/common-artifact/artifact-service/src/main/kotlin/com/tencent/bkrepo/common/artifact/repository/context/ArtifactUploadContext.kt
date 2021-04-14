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

package com.tencent.bkrepo.common.artifact.repository.context

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.api.ArtifactFileMap
import com.tencent.bkrepo.common.artifact.constant.OCTET_STREAM
import com.tencent.bkrepo.common.artifact.hash.HashAlgorithm
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.common.artifact.resolve.file.multipart.MultipartArtifactFile
import com.tencent.bkrepo.common.artifact.resolve.file.stream.OctetStreamArtifactFile
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail

/**
 * 构件上传context，依赖源可根据需求继承
 */
open class ArtifactUploadContext : ArtifactContext {

    private var artifactFileMap: ArtifactFileMap
    private var artifactFile: ArtifactFile? = null

    constructor(
        repo: RepositoryDetail,
        artifactFile: ArtifactFile
    ) {
        this.repositoryDetail = repo
        this.artifactFile = artifactFile
        this.artifactFileMap = ArtifactFileMap()
        this.artifactFileMap[OCTET_STREAM] = artifactFile
    }

    constructor(artifactFile: ArtifactFile) {
        this.artifactFile = artifactFile
        this.artifactFileMap = ArtifactFileMap()
        this.artifactFileMap[OCTET_STREAM] = artifactFile
    }

    constructor(artifactFileMap: ArtifactFileMap) {
        this.artifactFileMap = artifactFileMap
    }

    /**
     * 根据[name]获取构件文件[ArtifactFile]
     *
     * [name]为空则返回二进制流[OctetStreamArtifactFile]
     * [name]不为空则返回字段为[name]的[MultipartArtifactFile]
     * 如果[name]对应的构件文件不存在，则抛出[NullPointerException]
     */
    @Throws(NullPointerException::class)
    fun getArtifactFile(name: String? = null): ArtifactFile {
        return if (name.isNullOrBlank()) {
            artifactFile!!
        } else {
            artifactFileMap[name]!!
        }
    }

    /**
     * 根据[name]获取构件文件[ArtifactFile]
     *
     * [name]为空则返回二进制流[OctetStreamArtifactFile]
     * [name]不为空则返回字段为[name]的[MultipartArtifactFile]
     * 如果[name]对应的构件文件不存在，则返回null
     */
    @Throws(NullPointerException::class)
    fun getArtifactFileOrNull(name: String? = null): ArtifactFile? {
        return if (name.isNullOrBlank()) {
            artifactFile
        } else {
            artifactFileMap[name]
        }
    }

    /**
     * 获取[ArtifactFileMap]
     */
    fun getArtifactFileMap(): ArtifactFileMap {
        return artifactFileMap
    }

    /**
     * 获取[MultipartArtifactFile]，不存在则抛[NullPointerException]异常
     */
    fun getMultipartArtifactFile(name: String): MultipartArtifactFile {
        val artifactFile = artifactFileMap[name]
        require(artifactFile is MultipartArtifactFile)
        return artifactFile
    }

    /**
     * 获取[OctetStreamArtifactFile]，不存在则抛[NullPointerException]异常
     */
    @Suppress("UNCHECKED_CAST")
    fun getOctetStreamArtifactFile(): OctetStreamArtifactFile {
        return artifactFile as OctetStreamArtifactFile
    }

    /**
     * 如果名为[name]的构件存在则返回`true`
     */
    fun checkArtifactExist(name: String? = null): Boolean {
        return if (name.isNullOrBlank()) {
            return artifactFile != null
        } else {
            artifactFileMap[name] != null
        }
    }

    /**
     * 返回名为[name]的构件md5校验值
     *
     * [name]为`null`或不传值则返回二进制流文件的md5
     */
    fun getArtifactMd5(name: String? = null): String {
        return getArtifactFile(name).getFileMd5()
    }

    /**
     * 返回名为[name]的构件sha1校验值
     *
     * [name]为`null`或不传值则返回二进制流文件的sha1
     */
    fun getArtifactSha1(name: String? = null): String {
        return getArtifactFile(name).getFileSha1()
    }

    /**
     * 返回名为[name]的构件sha256校验值
     *
     * [name]为`null`或不传值则返回二进制流文件的sha256
     */
    fun getArtifactSha256(name: String? = null): String {
        return getArtifactFile(name).getFileSha256()
    }

    /**
     * 验证文件摘要是否正确，根据[uploadDigest]长度自动选择合适的摘要算法
     */
    fun validateDigest(uploadDigest: String, filename: String? = null) {
        val file = getArtifactFile(filename)
        val calculatedDigest = when (uploadDigest.length) {
            HashAlgorithm.MD5_LENGTH -> file.getFileMd5()
            HashAlgorithm.SHA1_LENGTH -> file.getFileSha1()
            HashAlgorithm.SHA256_LENGTH -> file.getFileSha256()
            else -> throw ErrorCodeException(CommonMessageCode.PARAMETER_INVALID, "digest")
        }
        if (uploadDigest != calculatedDigest) {
            throw ErrorCodeException(ArtifactMessageCode.DIGEST_CHECK_FAILED, "digest")
        }
    }
}
