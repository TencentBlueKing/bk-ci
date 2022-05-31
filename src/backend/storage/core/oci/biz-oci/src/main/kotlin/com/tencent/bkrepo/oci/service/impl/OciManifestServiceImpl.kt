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

import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.oci.constant.TAG_INVALID_CODE
import com.tencent.bkrepo.oci.constant.TAG_INVALID_DESCRIPTION
import com.tencent.bkrepo.oci.exception.OciBadRequestException
import com.tencent.bkrepo.oci.pojo.artifact.OciManifestArtifactInfo
import com.tencent.bkrepo.oci.service.OciManifestService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OciManifestServiceImpl : OciManifestService {

    override fun uploadManifest(artifactInfo: OciManifestArtifactInfo, artifactFile: ArtifactFile) {
        with(artifactInfo) {
            logger.info(
                "Handling upload manifest request for package [$packageName] " +
                    "with reference [$reference] in repo [${getRepoIdentify()}]"
            )
            val context = ArtifactUploadContext(artifactFile)
            ArtifactContextHolder.getRepository().upload(context)
        }
    }

    override fun downloadManifests(artifactInfo: OciManifestArtifactInfo) {
        logger.info(
            "Handling download manifest request for package [${artifactInfo.packageName}] " +
                "with reference [${artifactInfo.reference}] in repo [${artifactInfo.getRepoIdentify()}]"
        )
        val context = ArtifactDownloadContext()
        ArtifactContextHolder.getRepository().download(context)
    }

    override fun deleteManifests(artifactInfo: OciManifestArtifactInfo) {
        logger.info(
            "Handling delete manifest request for package [${artifactInfo.packageName}] " +
                "with digest [${artifactInfo.reference}] in repo [${artifactInfo.getRepoIdentify()}]"
        )
        if (!artifactInfo.isValidDigest) throw OciBadRequestException(
            "Manifest file only can be deleted by digest..", TAG_INVALID_CODE, TAG_INVALID_DESCRIPTION
        )
        val context = ArtifactRemoveContext()
        ArtifactContextHolder.getRepository().remove(context)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OciManifestServiceImpl::class.java)
    }
}
