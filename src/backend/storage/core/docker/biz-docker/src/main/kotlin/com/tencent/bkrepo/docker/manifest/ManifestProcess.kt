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

package com.tencent.bkrepo.docker.manifest

import com.fasterxml.jackson.databind.JsonNode
import com.tencent.bkrepo.common.api.constant.StringPool.EMPTY
import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.docker.artifact.DockerArtifact
import com.tencent.bkrepo.docker.artifact.DockerArtifactRepo
import com.tencent.bkrepo.docker.constant.DOCKER_API_VERSION
import com.tencent.bkrepo.docker.constant.DOCKER_CONTENT_DIGEST
import com.tencent.bkrepo.docker.constant.DOCKER_DIGEST
import com.tencent.bkrepo.docker.constant.DOCKER_HEADER_API_VERSION
import com.tencent.bkrepo.docker.constant.DOCKER_MANIFEST
import com.tencent.bkrepo.docker.constant.DOCKER_MANIFEST_LIST
import com.tencent.bkrepo.docker.constant.DOCKER_MANIFEST_TYPE
import com.tencent.bkrepo.docker.context.DownloadContext
import com.tencent.bkrepo.docker.context.RequestContext
import com.tencent.bkrepo.docker.errors.DockerV2Errors
import com.tencent.bkrepo.docker.exception.DockerFileSaveFailedException
import com.tencent.bkrepo.docker.exception.DockerNotFoundException
import com.tencent.bkrepo.docker.exception.DockerSyncManifestException
import com.tencent.bkrepo.docker.helpers.DockerManifestDigester
import com.tencent.bkrepo.docker.helpers.DockerManifestSyncer
import com.tencent.bkrepo.docker.manifest.ManifestContext.buildManifestListUploadContext
import com.tencent.bkrepo.docker.manifest.ManifestContext.buildPropertyMap
import com.tencent.bkrepo.docker.manifest.ManifestContext.buildUploadContext
import com.tencent.bkrepo.docker.model.DockerBlobInfo
import com.tencent.bkrepo.docker.model.DockerDigest
import com.tencent.bkrepo.docker.model.ManifestMetadata
import com.tencent.bkrepo.docker.response.DockerResponse
import com.tencent.bkrepo.docker.util.BlobUtil
import com.tencent.bkrepo.docker.util.BlobUtil.getBlobByName
import com.tencent.bkrepo.docker.util.BlobUtil.getManifestConfigBlob
import com.tencent.bkrepo.docker.util.ResponseUtil
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.ResponseEntity

/**
 * docker image manifest process
 * to upload the manifest to repository and store it
 */
class ManifestProcess constructor(val repo: DockerArtifactRepo) {

    companion object {
        private val logger = LoggerFactory.getLogger(ManifestProcess::class.java)
        private val objectMapper = JsonUtils.objectMapper
        private const val MANIFESTS = "manifests"
        private const val CONFIG = "config"
        private const val PLATFORM = "platform"
        private const val ARCHITECTURE = "architecture"
        private const val OS = "os"
        private const val AMD64 = "amd64"
        private const val LINUX = "linux"
    }

    /**
     * upload manifest by manifest type
     * @param context docker image request context
     * @param tag docker tag
     * @param manifestPath the current path of manifest
     * @param manifestType docker image manifest type
     * @param artifactFile file object of docker image
     * @return DockerDigest the upload manifest digest
     */
    fun uploadManifestByType(
        context: RequestContext,
        tag: String,
        manifestPath: String,
        manifestType: ManifestType,
        artifactFile: ArtifactFile
    ): Pair<DockerDigest, Long> {
        val manifestBytes = artifactFile.getInputStream().readBytes()
        val digest = DockerManifestDigester.calcDigest(manifestBytes)
        logger.info("manifest file digest content digest : [$digest]")
        if (ManifestType.Schema2List == manifestType) {
            processManifestList(context, tag, manifestPath, digest!!, manifestBytes)
            return Pair(digest, 0L)
        }

        // process scheme2 manifest
        val metadata = ManifestDeserializer.deserialize(repo, context, tag, manifestType, manifestBytes, digest!!)
        val size = metadata.tagInfo.totalSize
        addManifestsBlobs(context, manifestType, manifestBytes, metadata)
        if (!DockerManifestSyncer.syncBlobs(context, repo, metadata, tag)) {
            logger.warn("fail to sync manifest blobs, cancel manifest upload")
            throw DockerSyncManifestException(manifestPath)
        }

        logger.info("start to upload manifest : [$manifestType]")
        val uploadContext = buildUploadContext(context, manifestType, metadata, manifestPath, artifactFile)
        val params = buildPropertyMap(context.artifactName, tag, digest, manifestType)
        val labels = metadata.tagInfo.labels
        labels.entries().forEach {
            params[it.key] = it.value
        }
        uploadContext.metadata(params)
        if (!repo.upload(uploadContext)) {
            logger.warn("upload manifest fail [$uploadContext]")
            throw DockerFileSaveFailedException(manifestPath)
        }
        return Pair(digest, size)
    }

    /**
     * get manifest from repo storage
     * @param projectId the project id
     * @param repoName the repository name
     * @param manifestPath manifest path
     * @return String the type of manifest
     */
    fun getManifestType(projectId: String, repoName: String, manifestPath: String): String? {
        return repo.getAttribute(projectId, repoName, manifestPath, DOCKER_MANIFEST_TYPE)
    }

    /**
     * get manifest config content which the schema is v2
     * @param context the  request context
     * @param manifestBytes the byte data of manifest
     * @param tag the tag of docker images
     * @return ByteArray byte data of manifest config file
     */
    fun getSchema2ConfigContent(context: RequestContext, manifestBytes: ByteArray, tag: String): ByteArray {
        val manifest = objectMapper.readTree(manifestBytes)
        val digest = manifest.get(CONFIG).get(DOCKER_DIGEST).asText()
        val fileName = DockerDigest(digest).fileName()
        val configFile = getManifestConfigBlob(repo, fileName, context, tag) ?: return ByteArray(0)
        logger.info("get manifest config file [$configFile]")
        val downloadContext = DownloadContext(context).sha256(configFile.sha256!!).length(configFile.length)
        val stream = repo.download(downloadContext)
        stream.use {
            return it.readBytes()
        }
    }

    /**
     * get manifest file content which the schema is v2
     * @param context the  request context
     * @param schema2Path the path of the file
     * @return ByteArray byte data of manifest
     */
    fun getSchema2ManifestContent(context: RequestContext, schema2Path: String): ByteArray {
        val manifest = getManifestByName(context, schema2Path) ?: return ByteArray(0)
        val downloadContext = DownloadContext(context).sha256(manifest.sha256!!).length(manifest.length)
        val stream = repo.download(downloadContext)
        stream.use {
            return it.readBytes()
        }
    }

    /**
     * get manifest path which the schema is v2
     * @param context the  request context
     * @param bytes the byte data of manifest
     * @return String the path of manifest
     */
    fun getSchema2Path(context: RequestContext, bytes: ByteArray): String {
        val manifestList = objectMapper.readTree(bytes)
        val manifests = manifestList.get(MANIFESTS)
        val maniIter = manifests.iterator()
        while (maniIter.hasNext()) {
            val manifest = maniIter.next() as JsonNode
            val platform = manifest.get(PLATFORM)
            val architecture = platform.get(ARCHITECTURE).asText()
            val os = platform.get(OS).asText()
            if (StringUtils.equals(architecture, AMD64) && StringUtils.equals(os, LINUX)) {
                val digest = manifest.get(DOCKER_DIGEST).asText()
                val fileName = DockerDigest(digest).fileName()
                val manifestFile = getBlobByName(repo, context, fileName) ?: return EMPTY
                return BlobUtil.getFullPath(manifestFile)
            }
        }
        return EMPTY
    }

    /**
     * add metadata to manifest
     * @param context the  request context
     * @param type the type of manifest file
     * @param bytes the byte data of manifest
     * @param metadata the manifest metadata
     */
    private fun addManifestsBlobs(
        context: RequestContext,
        type: ManifestType,
        bytes: ByteArray,
        metadata: ManifestMetadata
    ) {
        if (ManifestType.Schema2 == type) {
            addSchema2Blob(bytes, metadata)
        } else if (ManifestType.Schema2List == type) {
            addSchema2ListBlobs(context, bytes, metadata)
        }
    }

    /**
     * determine the manifest type of the file
     * @param context the  request context
     * @param tag the tag of manifest
     * @param headers request head
     * @return ManifestType type of manifest
     */
    fun chooseManifestType(context: RequestContext, tag: String, headers: HttpHeaders): ManifestType {
        val acceptable = ResponseUtil.getAcceptableManifestTypes(headers)
        if (acceptable.contains(ManifestType.Schema2List)) {
            with(context) {
                val manifestPath = ResponseUtil.buildManifestPath(artifactName, tag, ManifestType.Schema2List)
                if (repo.exists(projectId, repoName, manifestPath)) {
                    return ManifestType.Schema2List
                }
            }
        }

        return if (acceptable.contains(ManifestType.Schema2)) {
            ManifestType.Schema2
        } else if (acceptable.contains(ManifestType.Schema1Signed)) {
            ManifestType.Schema1Signed
        } else {
            if (acceptable.contains(ManifestType.Schema1)) ManifestType.Schema1 else ManifestType.Schema1Signed
        }
    }

    /**
     * first get manifest from digest
     * @param context the  request context
     * @param digest the digest of docker image
     * @param headers request head
     * @return DockerResponse  http repsponse of manifest
     */
    fun getManifestByDigest(context: RequestContext, digest: DockerDigest, headers: HttpHeaders): DockerResponse {
        logger.info("fetch docker manifest [$context] and digest [$digest] ")
        var artifact = getManifestByName(context, DOCKER_MANIFEST)
        artifact?.let {
            val acceptable = ResponseUtil.getAcceptableManifestTypes(headers)
            if (acceptable.contains(ManifestType.Schema2List)) {
                artifact = getManifestByName(context, DOCKER_MANIFEST_LIST) ?: run {
                    logger.warn("get manifest by name fail [$context,$digest]")
                    return DockerV2Errors.manifestUnknown(digest.toString())
                }
            }
        }
        return buildManifestResponse(context, context.artifactName, digest, artifact!!.length, headers)
    }

    /**
     * then get manifest with tag
     * @param context the  request context
     * @param tag the tag of docker image
     * @param headers request head
     * @return DockerResponse  http response of manifest
     */
    fun getManifestByTag(context: RequestContext, tag: String, headers: HttpHeaders): DockerResponse {
        val useManifestType = chooseManifestType(context, tag, headers)
        val manifestPath = ResponseUtil.buildManifestPath(context.artifactName, tag, useManifestType)
        logger.info("get manifest by tag params [$context,$manifestPath]")
        if (!repo.canRead(context)) {
            logger.warn("do not have permission to get [$context,$manifestPath]")
            return DockerV2Errors.unauthorizedManifest(manifestPath)
        }
        val manifest = repo.getArtifact(context.projectId, context.repoName, manifestPath) ?: run {
            logger.warn("the node not exist [$context,$manifestPath]")
            return DockerV2Errors.manifestUnknown(manifestPath)
        }
        logger.info("get manifest by tag result [$manifest]")
        val digest = DockerDigest.fromSha256(manifest.sha256!!)
        return buildManifestResponse(context, manifestPath, digest, manifest.length, headers)
    }

    /**
     * process with manifest list
     * @param context the  request context
     * @param tag the tag of docker image
     * @param manifestPath the path of manifest
     * @param digest the digest of docker image
     * @param manifestBytes the byte data of manifest
     */
    private fun processManifestList(
        context: RequestContext,
        tag: String,
        manifestPath: String,
        digest: DockerDigest,
        manifestBytes: ByteArray
    ) {
        val manifestList = ManifestListSchema2Deserializer.deserialize(manifestBytes)
        manifestList?.let {
            val iter = manifestList.manifests.iterator()
            // check every manifest in the repo
            while (iter.hasNext()) {
                val manifest = iter.next()
                val mDigest = manifest.digest
                val manifestFileName = DockerDigest(mDigest!!).fileName()
                getBlobByName(repo, context, manifestFileName) ?: run {
                    logger.warn("manifest not found [$context, $digest]")
                    throw DockerNotFoundException("manifest list [$digest] miss manifest digest $mDigest. ==>$manifest")
                }
            }
        }
        val uploadContext = buildManifestListUploadContext(context, digest, manifestPath, manifestBytes)
        val params = buildPropertyMap(context.artifactName, tag, digest, ManifestType.Schema2List)
        uploadContext.metadata(params)
        if (!repo.upload(uploadContext)) {
            logger.warn("upload manifest list fail [$uploadContext]")
            throw DockerFileSaveFailedException(manifestPath)
        }
    }

    /**
     * add schema 2 blob data
     * @param bytes the manifest byte data
     * @param metadata the metadata of manifest
     */
    private fun addSchema2Blob(bytes: ByteArray, metadata: ManifestMetadata) {
        val manifest = objectMapper.readTree(bytes)
        val config = manifest.get(CONFIG)
        config?.let {
            val digest = config.get(DOCKER_DIGEST).asText()
            val blobInfo = DockerBlobInfo(EMPTY, digest, 0L, EMPTY)
            metadata.blobsInfo.add(blobInfo)
        }
    }

    /**
     * add schema  list 2 blob data
     * @param context the request context
     * @param bytes the manifest byte data
     * @param metadata the metadata of manifest
     */
    private fun addSchema2ListBlobs(context: RequestContext, bytes: ByteArray, metadata: ManifestMetadata) {
        val manifestList = JsonUtils.objectMapper.readTree(bytes)
        val manifests = manifestList.get(MANIFESTS)
        val manifest = manifests.iterator()

        while (manifest.hasNext()) {
            val manifestNode = manifest.next() as JsonNode
            val digestString = manifestNode.get(PLATFORM).get(DOCKER_DIGEST).asText()
            val dockerBlobInfo = DockerBlobInfo(EMPTY, digestString, 0L, EMPTY)
            metadata.blobsInfo.add(dockerBlobInfo)
            val manifestFileName = DockerDigest(digestString).fileName()
            val manifestFile = getManifestByName(context, manifestFileName)
            manifestFile?.let {
                val fullPath = BlobUtil.getFullPath(manifestFile)
                val configBytes = getSchema2ManifestContent(context, fullPath)
                addSchema2Blob(configBytes, metadata)
            }
        }
    }

    /**
     * build manifest response
     * @param context the request context
     * @param manifestPath the manifest path of file
     * @param digest the digest of docker image
     * @param length length of file
     * @param httpHeaders the request http header
     */
    private fun buildManifestResponse(
        context: RequestContext,
        manifestPath: String,
        digest: DockerDigest,
        length: Long,
        httpHeaders: HttpHeaders
    ): DockerResponse {
        val downloadContext = DownloadContext(context).length(length).sha256(digest.getDigestHex())
        val inputStream = repo.download(downloadContext)
        val inputStreamResource = InputStreamResource(inputStream)
        val contentType = getManifestType(context.projectId, context.repoName, manifestPath)
        httpHeaders.apply {
            set(DOCKER_HEADER_API_VERSION, DOCKER_API_VERSION)
        }.apply {
            set(DOCKER_CONTENT_DIGEST, digest.toString())
        }.apply {
            set(CONTENT_TYPE, contentType)
        }
        logger.info("file [$digest] result length [$length] type [$contentType]")
        return ResponseEntity.ok().headers(httpHeaders).contentLength(length).body(inputStreamResource)
    }

    /**
     * build manifest artifact
     * @param context the request context
     * @param fileName file name
     * @param DockerArtifact the docker artifact object
     */
    private fun getManifestByName(context: RequestContext, fileName: String): DockerArtifact? {
        val fullPath = "/${context.artifactName}/$fileName"
        return repo.getArtifact(context.projectId, context.repoName, fullPath)
    }
}
