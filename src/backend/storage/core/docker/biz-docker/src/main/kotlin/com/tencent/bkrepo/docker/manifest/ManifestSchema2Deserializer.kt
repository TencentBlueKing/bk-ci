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

import com.fasterxml.jackson.databind.JsonNode
import com.tencent.bkrepo.common.api.constant.StringPool.EMPTY
import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.docker.constant.DOCKER_CREATED_BY_CMD
import com.tencent.bkrepo.docker.constant.DOCKER_CREATED_CMD
import com.tencent.bkrepo.docker.constant.DOCKER_DIGEST
import com.tencent.bkrepo.docker.constant.DOCKER_EMPTY_CMD
import com.tencent.bkrepo.docker.constant.DOCKER_EMPTY_LAYER_CMD
import com.tencent.bkrepo.docker.constant.DOCKER_FOREIGN_KEY
import com.tencent.bkrepo.docker.constant.DOCKER_HISTORY_CMD
import com.tencent.bkrepo.docker.constant.DOCKER_LAYER_CMD
import com.tencent.bkrepo.docker.constant.DOCKER_MEDIA_TYPE
import com.tencent.bkrepo.docker.constant.DOCKER_NODE_SIZE
import com.tencent.bkrepo.docker.constant.DOCKER_NOP_CMD
import com.tencent.bkrepo.docker.constant.DOCKER_NOP_SPACE_CMD
import com.tencent.bkrepo.docker.constant.DOCKER_RUN_CMD
import com.tencent.bkrepo.docker.constant.DOCKER_URLS_CMD
import com.tencent.bkrepo.docker.exception.DockerManifestDeseriFailException
import com.tencent.bkrepo.docker.model.DockerBlobInfo
import com.tencent.bkrepo.docker.model.DockerDigest
import com.tencent.bkrepo.docker.model.DockerImageMetadata
import com.tencent.bkrepo.docker.model.ManifestMetadata
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.stream.StreamSupport

/**
 * deserialize manifest schema2 manifest
 */
object ManifestSchema2Deserializer : AbstractManifestDeserializer() {

    private val logger = LoggerFactory.getLogger(ManifestSchema2Deserializer::class.java)
    private val objectMapper = JsonUtils.objectMapper
    private const val CIRCUIT_BREAKER_THRESHOLD = 5000

    /**
     * deserialize manifest bytes from schema v2
     * @param manifestBytes byte array data of manifest
     * @param configBytes docker manifest file image
     * @param dockerRepo docker image repo name
     * @param tag docker image tag
     * @param digest docker manifest digest
     * @return ManifestMetadata the upload manifest digest
     */
    fun deserialize(
        manifestBytes: ByteArray,
        configBytes: ByteArray,
        dockerRepo: String,
        tag: String,
        digest: DockerDigest
    ): ManifestMetadata {
        try {
            val manifestMetadata = ManifestMetadata()
            manifestMetadata.tagInfo.title = "$dockerRepo:$tag"
            manifestMetadata.tagInfo.digest = digest
            return applyAttributesFromContent(manifestBytes, configBytes, manifestMetadata)
        } catch (exception: IOException) {
            logger.error("Unable to deserialize the manifest.json file: [$exception]")
            throw DockerManifestDeseriFailException(exception.message!!)
        }
    }

    /**
     * apply attributes from  manifest bytes  content
     * @param manifestBytes byte array data of manifest
     * @param configBytes docker manifest file image
     * @param manifestMetadata manifest meta data
     * @return ManifestMetadata the upload manifest digest
     */
    private fun applyAttributesFromContent(
        manifestBytes: ByteArray,
        configBytes: ByteArray,
        manifestMetadata: ManifestMetadata
    ): ManifestMetadata {

        // get config object and manifest object
        val config = objectMapper.readTree(configBytes)
        val manifest = objectMapper.readTree(manifestBytes)
        var totalSize = 0L
        val history = config.get(DOCKER_HISTORY_CMD)
        val layers = manifest.get(DOCKER_LAYER_CMD)
        val historySize = history?.size() ?: 0
        var historyCounter = 0L
        history?.let {
            val iterable = Iterable<JsonNode> { history.elements() }
            historyCounter = StreamSupport.stream(iterable.spliterator(), false).filter { notEmptyHistoryLayer(it) }.count()
        }
        val foreignHasHistory = layers.size().toLong() == historyCounter
        var iterationsCounter = 0
        var hisIndex = 0
        var layersIndex = 0

        // add layer blobs
        while (hisIndex < historySize || layersIndex < layers.size()) {

            val historyLayer = history?.get(hisIndex)
            val layer = layers.get(layersIndex)
            var size = 0L
            var digest: String? = null
            if (notEmptyHistoryLayer(historyLayer) || !foreignHasHistory && isForeignLayer(layer)) {
                size = layer.get(DOCKER_NODE_SIZE).asLong()
                totalSize += size
                digest = layer.get(DOCKER_DIGEST).asText()
                ++layersIndex
            }
            if (!isForeignLayer(layer) || foreignHasHistory) {
                ++hisIndex
            }

            // populate with command
            var created = config.get(DOCKER_CREATED_CMD).asText()
            if (historyLayer != null && !isForeignLayer(layer)) {
                created = historyLayer[DOCKER_CREATED_CMD].asText()
            }
            val blobInfo = DockerBlobInfo(EMPTY, digest, size, created)
            if (!isForeignLayer(layer)) {
                populateWithCommand(historyLayer, blobInfo)
            }

            populateWithMediaType(layer, blobInfo)
            manifestMetadata.blobsInfo.add(blobInfo)
            checkCircuitBreaker(manifestBytes, configBytes, iterationsCounter)
            ++iterationsCounter
        }

        manifestMetadata.blobsInfo.reverse()
        manifestMetadata.tagInfo.totalSize = totalSize
        val dockerMetadata = objectMapper.readValue(config.toString().toByteArray(), DockerImageMetadata::class.java)
        // populate ports volumes and labels from manifest metadata
        populatePorts(manifestMetadata, dockerMetadata)
        populateVolumes(manifestMetadata, dockerMetadata)
        populateLabels(manifestMetadata, dockerMetadata)
        return manifestMetadata
    }

    /**
     * circuit break to avoid loop
     * @param manifestBytes byte array data of manifest
     * @param configBytes docker manifest file image
     * @param iterationsCounter count
     */
    private fun checkCircuitBreaker(manifestBytes: ByteArray, configBytes: ByteArray, iterationsCounter: Int) {
        if (iterationsCounter > CIRCUIT_BREAKER_THRESHOLD) {
            val reason = "$CIRCUIT_BREAKER_THRESHOLD Iterations ware performed"
            val msg = "ManifestSchema2Deserializer CIRCUIT BREAKER: $reason breaking operation.Manifest: " +
                String(manifestBytes, StandardCharsets.UTF_8) + "jsonBytes:" +
                String(configBytes, StandardCharsets.UTF_8)
            logger.error(msg)
            throw IllegalArgumentException("Circuit Breaker Threshold Reached, Breaking Operation. see log output for manifest details.")
        }
    }

    private fun isForeignLayer(layer: JsonNode?): Boolean {
        return layer != null && layer.has(DOCKER_MEDIA_TYPE) && DOCKER_FOREIGN_KEY == layer.get(DOCKER_MEDIA_TYPE).asText()
    }

    private fun notEmptyHistoryLayer(historyLayer: JsonNode?): Boolean {
        return historyLayer != null && historyLayer.get(DOCKER_EMPTY_LAYER_CMD) == null
    }

    /**
     * populate with command
     * @param layerHistory the json node
     * @param blobInfo docker blob info
     */
    private fun populateWithCommand(layerHistory: JsonNode?, blobInfo: DockerBlobInfo) {
        if (layerHistory != null && layerHistory.has(DOCKER_CREATED_BY_CMD)) {
            var cmd = layerHistory.get(DOCKER_CREATED_BY_CMD).asText()
            if (StringUtils.contains(cmd, DOCKER_NOP_CMD)) {
                cmd = StringUtils.substringAfter(cmd, DOCKER_NOP_SPACE_CMD)
                val dockerCmd = StringUtils.substringBefore(cmd.trim { it <= ' ' }, DOCKER_EMPTY_CMD)
                cmd = StringUtils.substringAfter(cmd, DOCKER_EMPTY_CMD)
                blobInfo.command = dockerCmd
                blobInfo.commandText = cmd
            } else if (cmd.isNotBlank()) {
                blobInfo.command = DOCKER_RUN_CMD
                blobInfo.commandText = cmd
            }
        }
    }

    /**
     * populate with media type
     * @param layerNode the json node
     * @param blobInfo docker blob info
     */
    private fun populateWithMediaType(layerNode: JsonNode?, blobInfo: DockerBlobInfo) {
        layerNode?.let {
            if (layerNode.has(DOCKER_MEDIA_TYPE)) {
                blobInfo.mediaType = layerNode.get(DOCKER_MEDIA_TYPE).asText()
            }

            if (layerNode.has(DOCKER_URLS_CMD)) {
                blobInfo.urls = mutableListOf()
                layerNode.get(DOCKER_URLS_CMD).forEach { jsonNode -> blobInfo.urls!!.add(jsonNode.asText()) }
            }
        }
    }
}
