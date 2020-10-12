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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.docker.manifest

import com.tencent.bkrepo.common.api.constant.StringPool.COLON
import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.docker.constant.DOCKER_BLOB_SUM
import com.tencent.bkrepo.docker.constant.DOCKER_COM_CMD
import com.tencent.bkrepo.docker.constant.DOCKER_EMPTY_CMD
import com.tencent.bkrepo.docker.constant.DOCKER_FS_LAYER
import com.tencent.bkrepo.docker.constant.DOCKER_HISTORY_CMD
import com.tencent.bkrepo.docker.constant.DOCKER_NODE_NAME
import com.tencent.bkrepo.docker.constant.DOCKER_NOP_CMD
import com.tencent.bkrepo.docker.constant.DOCKER_NOP_SPACE_CMD
import com.tencent.bkrepo.docker.constant.DOCKER_RUN_CMD
import com.tencent.bkrepo.docker.constant.DOCKER_TAG
import com.tencent.bkrepo.docker.exception.DockerManifestDeseriFailException
import com.tencent.bkrepo.docker.model.DockerBlobInfo
import com.tencent.bkrepo.docker.model.DockerDigest
import com.tencent.bkrepo.docker.model.DockerImageMetadata
import com.tencent.bkrepo.docker.model.ManifestMetadata
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory
import java.io.IOException

/**
 * deserialize manifest schema1 manifest
 */
object ManifestSchema1Deserializer : AbstractManifestDeserializer() {

    private val logger = LoggerFactory.getLogger(ManifestSchema1Deserializer::class.java)
    private val objectMapper = JsonUtils.objectMapper

    /**
     * deserialize schema v2
     * @param manifestBytes byte array data of manifest
     * @param digest docker manifest file image
     * @return ManifestMetadata the upload manifest digest
     */
    fun deserialize(manifestBytes: ByteArray, digest: DockerDigest): ManifestMetadata {
        try {
            return applyAttributesFromContent(manifestBytes, digest)
        } catch (exception: IOException) {
            logger.error("Unable to deserialize the manifest.json file: [$exception]")
            throw DockerManifestDeseriFailException(exception.message!!)
        }
    }

    /**
     * apply attributes from manifest byte array data
     * @param manifestBytes byte array data of manifest
     * @param digest docker manifest file image
     * @return ManifestMetadata the upload manifest digest
     */
    private fun applyAttributesFromContent(manifestBytes: ByteArray?, digest: DockerDigest): ManifestMetadata {
        val manifestMetadata = ManifestMetadata()
        manifestBytes ?: return manifestMetadata

        val manifest = objectMapper.readTree(manifestBytes)
        val title = manifest.get(DOCKER_NODE_NAME).asText() + COLON + manifest.get(DOCKER_TAG).asText()
        manifestMetadata.tagInfo.title = title
        manifestMetadata.tagInfo.digest = digest
        var totalSize = 0L
        val history = manifest.get(DOCKER_HISTORY_CMD)

        // range history blobs
        for (i in 0 until history.size()) {
            val fsLayer = history.get(i)
            val v1Compatibility = fsLayer.get(DOCKER_COM_CMD).asText()
            val dockerMetadata = objectMapper.readValue(v1Compatibility.toByteArray(), DockerImageMetadata::class.java)
            val blobDigest = manifest.get(DOCKER_FS_LAYER).get(i).get(DOCKER_BLOB_SUM).asText()
            val size = dockerMetadata.size
            totalSize += dockerMetadata.size
            val blobInfo = DockerBlobInfo(dockerMetadata.id!!, blobDigest, size, dockerMetadata.created!!)

            populateWithCommand(dockerMetadata, blobInfo)
            manifestMetadata.blobsInfo.add(blobInfo)

            // populate ports volumes, labels from manifest file
            populatePorts(manifestMetadata, dockerMetadata)
            populateVolumes(manifestMetadata, dockerMetadata)
            populateLabels(manifestMetadata, dockerMetadata)
        }

        manifestMetadata.tagInfo.totalSize = totalSize
        return manifestMetadata
    }

    /**
     * populate with command
     * @param dockerMetadata image metadata
     * @param blobInfo docker blob info
     */
    private fun populateWithCommand(dockerMetadata: DockerImageMetadata, blobInfo: DockerBlobInfo) {
        var cmd = getCommand(dockerMetadata)
        if (StringUtils.contains(cmd, DOCKER_NOP_CMD)) {
            cmd = StringUtils.substringAfter(cmd, DOCKER_NOP_SPACE_CMD)
            val dockerCmd = StringUtils.substringBefore(cmd, DOCKER_EMPTY_CMD)
            cmd = StringUtils.substringAfter(cmd, DOCKER_EMPTY_CMD)
            blobInfo.command = dockerCmd
            blobInfo.commandText = cmd
        } else if (cmd!!.isNotBlank()) {
            blobInfo.command = DOCKER_RUN_CMD
            blobInfo.commandText = cmd
        }
    }

    /**
     * get command from docker meta data
     * @param dockerMetadata docker image metadata
     * @return String the command get from metadata
     */
    private fun getCommand(dockerMetadata: DockerImageMetadata): String? {
        var cmd: String? = null
        if (dockerMetadata.containerConfig != null && dockerMetadata.containerConfig!!.cmd != null) {
            cmd = if (dockerMetadata.containerConfig!!.cmd!!.size == 3) {
                dockerMetadata.containerConfig!!.cmd!![2]
            } else {
                dockerMetadata.containerConfig!!.cmd.toString()
            }
        }

        if (dockerMetadata.config != null && cmd!!.isBlank() && dockerMetadata.config!!.cmd != null) {
            cmd = if (dockerMetadata.config!!.cmd!!.size == 3) {
                dockerMetadata.config!!.cmd!![2]
            } else {
                dockerMetadata.config!!.cmd.toString()
            }
        }
        return cmd
    }
}
