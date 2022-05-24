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

package com.tencent.bkrepo.oci.service.impl

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.oci.config.OciProperties
import com.tencent.bkrepo.oci.exception.OciBadRequestException
import com.tencent.bkrepo.oci.pojo.artifact.OciBlobArtifactInfo
import com.tencent.bkrepo.oci.service.OciBlobService
import com.tencent.bkrepo.oci.util.OciLocationUtils
import com.tencent.bkrepo.oci.util.OciResponseUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OciBlobServiceImpl(
    private val ociProperties: OciProperties
) : OciBlobService {

    override fun startUploadBlob(artifactInfo: OciBlobArtifactInfo, artifactFile: ArtifactFile) {
        with(artifactInfo) {
            logger.info("Handling bolb upload request $artifactInfo in ${getRepoIdentify()} .")
            if (digest.isNullOrBlank()) {
                logger.info("Will use post then put to upload blob...")
                obtainSessionIdForUpload(artifactInfo)
            } else {
                logger.info("Will use single post to upload blob...")
                singlePostUpload(artifactFile)
            }
        }
    }

    /**
     * 使用单个post请求直接上传文件
     */
    private fun singlePostUpload(artifactFile: ArtifactFile) {
        val context = ArtifactUploadContext(artifactFile)
        ArtifactContextHolder.getRepository().upload(context)
    }

    /**
     * 获取上传文件uuid
     */
    private fun obtainSessionIdForUpload(artifactInfo: OciBlobArtifactInfo) {
        with(artifactInfo) {
            if (mount.isNullOrBlank()) {
                logger.info("Will obtain uuid for uploading blobs in repo ${artifactInfo.getRepoIdentify()}.")
                val uuidCreated = StringPool.uniqueId()
                OciResponseUtils.buildBlobUploadUUIDResponse(
                    ociProperties.domain,
                    uuidCreated,
                    OciLocationUtils.blobUUIDLocation(uuidCreated, artifactInfo),
                    HttpContextHolder.getResponse()
                )
            } else {
                // TODO 如果mount不为空，这里需要处理
            }
        }
    }

    override fun uploadBlob(artifactInfo: OciBlobArtifactInfo, artifactFile: ArtifactFile) {
        logger.info("handing request upload blob [$artifactInfo] in repo ${artifactInfo.getRepoIdentify()}.")
        uploadBlobFromPut(artifactInfo, artifactFile)
        // TODO 三段式追加上传逻辑需要处理
    }

    /**
     * 上传blob文件
     */
    private fun uploadBlobFromPut(artifactInfo: OciBlobArtifactInfo, artifactFile: ArtifactFile) {
        with(artifactInfo) {
            logger.info("Will upload blob [${getArtifactFullPath()}] into [${getRepoIdentify()}]")
            // TODO mount不为空的时候逻辑需要确认
            val context = ArtifactUploadContext(artifactFile)
            ArtifactContextHolder.getRepository().upload(context)
        }
    }

    override fun downloadBlob(artifactInfo: OciBlobArtifactInfo) {
        with(artifactInfo) {
            logger.info(
                "Handling blob download request for blob [${getDigest()}] in repo [${artifactInfo.getRepoIdentify()}]"
            )
            val context = ArtifactDownloadContext()
            ArtifactContextHolder.getRepository().download(context)
        }
    }

    override fun deleteBlob(artifactInfo: OciBlobArtifactInfo) {
        logger.info(
            "Handling delete blob request for package [${artifactInfo.packageName}] " +
                "with digest [${artifactInfo.digest}] in repo [${artifactInfo.getRepoIdentify()}]"
        )
        if (artifactInfo.digest.isNullOrBlank())
            throw OciBadRequestException("Blob file only can be deleted by digest..")
        val context = ArtifactRemoveContext()
        ArtifactContextHolder.getRepository().remove(context)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OciBlobServiceImpl::class.java)
    }
}
