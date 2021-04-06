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

package com.tencent.bkrepo.docker.helpers

import com.tencent.bkrepo.common.api.util.toJsonString
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.docker.artifact.DockerArtifactRepo
import com.tencent.bkrepo.docker.constant.DOCKER_FOREIGN_KEY
import com.tencent.bkrepo.docker.constant.DOCKER_TMP_UPLOAD_PATH
import com.tencent.bkrepo.docker.context.RequestContext
import com.tencent.bkrepo.docker.context.UploadContext
import com.tencent.bkrepo.docker.exception.DockerFileSaveFailedException
import com.tencent.bkrepo.docker.model.DockerBlobInfo
import com.tencent.bkrepo.docker.model.DockerDigest
import com.tencent.bkrepo.docker.model.ManifestMetadata
import com.tencent.bkrepo.docker.util.BlobUtil.getBlobByName
import com.tencent.bkrepo.docker.util.ResponseUtil.EMPTY_BLOB_CONTENT
import com.tencent.bkrepo.docker.util.ResponseUtil.emptyBlobDigest
import com.tencent.bkrepo.docker.util.ResponseUtil.isEmptyBlob
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream

/**
 * to synchronize blob when finish manifest upload
 * copy blob from temp path or other tag
 */
object DockerManifestSyncer {

    /**
     * sync blobs file describe in manifest file
     * @param context docker request context
     * @param repo docker storage interface
     * @param info manifest metadata
     * @param tag docker image tag
     * @return Boolean the sync result
     */
    fun syncBlobs(context: RequestContext, repo: DockerArtifactRepo, info: ManifestMetadata, tag: String): Boolean {
        logger.info("start to sync docker repository blobs [${info.toJsonString()}]")
        val manifestInfos = info.blobsInfo.iterator()
        while (manifestInfos.hasNext()) {
            val blobInfo = manifestInfos.next()
            logger.info("sync docker blob digest [${blobInfo.digest}]")

            // check digest
            if (blobInfo.digest == null || this.isForeignLayer(blobInfo)) {
                logger.info("blob format error [$blobInfo]")
                continue
            }
            val blobDigest = DockerDigest(blobInfo.digest!!)
            val fileName = blobDigest.fileName()
            val tempPath = "/${context.artifactName}/$DOCKER_TMP_UPLOAD_PATH/$fileName"
            val finalPath = "/${context.artifactName}/$tag/$fileName"

            // check path exist
            if (repo.exists(context.projectId, context.repoName, finalPath)) {
                logger.info("node [$finalPath] exist in the repo")
                continue
            }

            // check is empty digest
            if (isEmptyBlob(blobDigest)) {
                logger.info("found empty layer [$fileName] in manifest  ,create blob in path [$finalPath]")
                val blobContent = ByteArrayInputStream(EMPTY_BLOB_CONTENT)
                val artifactFile = ArtifactFileFactory.build(blobContent)
                try {
                    val uploadContext = UploadContext(context.projectId, context.repoName, finalPath)
                        .sha256(emptyBlobDigest().getDigestHex()).artifactFile(artifactFile)
                    if (!repo.upload(uploadContext)) {
                        logger.warn("save blob failed [$finalPath]")
                        throw DockerFileSaveFailedException(finalPath)
                    }
                } finally {
                    artifactFile.delete()
                }
                continue
            }

            // temp path exist, move from it to final
            if (repo.exists(context.projectId, context.repoName, tempPath)) {
                logger.info("move blob from the temp path [$context,$tempPath,$finalPath]")
                if (!repo.move(context, tempPath, finalPath)) {
                    logger.warn("move blob failed [$finalPath]")
                    throw DockerFileSaveFailedException(finalPath)
                }
                continue
            }

            // final copy from other blob
            logger.info("blob temp file [$tempPath] doesn't exist in temp, try to copy")
            if (!copyBlobFrom(context, repo, fileName, finalPath)) {
                logger.warn("copy file from other path failed [$finalPath]")
                throw DockerFileSaveFailedException(finalPath)
            }
        }
        logger.info("finish sync docker repository blobs")
        return true
    }

    /**
     * move blob file from temp path to final path
     * @param context docker request context
     * @param repo docker storage interface
     * @param fileName file name metadata
     * @param targetPath target final path
     * @return Boolean the sync result
     */
    private fun copyBlobFrom(
        context: RequestContext,
        repo: DockerArtifactRepo,
        fileName: String,
        targetPath: String
    ): Boolean {
        val blob = getBlobByName(repo, context, fileName) ?: return false
        val sourcePath = blob.fullPath
        if (StringUtils.equals(blob.fullPath, targetPath)) return true
        return repo.copy(context, sourcePath, targetPath)
    }

    private fun isForeignLayer(blobInfo: DockerBlobInfo): Boolean {
        return DOCKER_FOREIGN_KEY == blobInfo.mediaType
    }

    private val logger = LoggerFactory.getLogger(DockerManifestSyncer::class.java)
}
