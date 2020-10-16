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

package com.tencent.bkrepo.docker.service

import com.tencent.bkrepo.common.api.constant.StringPool.EMPTY
import com.tencent.bkrepo.common.api.constant.StringPool.SLASH
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.docker.artifact.DockerArtifactRepo
import com.tencent.bkrepo.docker.constant.BLOB_PATTERN
import com.tencent.bkrepo.docker.constant.DOCKER_API_VERSION
import com.tencent.bkrepo.docker.constant.DOCKER_CONTENT_DIGEST
import com.tencent.bkrepo.docker.constant.DOCKER_HEADER_API_VERSION
import com.tencent.bkrepo.docker.constant.DOCKER_LENGTH_EMPTY
import com.tencent.bkrepo.docker.constant.DOCKER_LINK
import com.tencent.bkrepo.docker.constant.DOCKER_MANIFEST
import com.tencent.bkrepo.docker.constant.DOCKER_NODE_FULL_PATH
import com.tencent.bkrepo.docker.constant.DOCKER_NODE_PATH
import com.tencent.bkrepo.docker.constant.DOCKER_NODE_SIZE
import com.tencent.bkrepo.docker.constant.DOCKER_TMP_UPLOAD_PATH
import com.tencent.bkrepo.docker.constant.DOCKER_TMP_UPLOAD_PATH
import com.tencent.bkrepo.docker.constant.DOCKER_UPLOAD_UUID
import com.tencent.bkrepo.docker.context.DownloadContext
import com.tencent.bkrepo.docker.context.RequestContext
import com.tencent.bkrepo.docker.context.UploadContext
import com.tencent.bkrepo.docker.errors.DockerV2Errors
import com.tencent.bkrepo.docker.exception.DockerRepoNotFoundException
import com.tencent.bkrepo.docker.helpers.DockerCatalogTagsSlicer
import com.tencent.bkrepo.docker.helpers.DockerPaginationElementsHolder
import com.tencent.bkrepo.docker.manifest.ManifestProcess
import com.tencent.bkrepo.docker.manifest.ManifestType
import com.tencent.bkrepo.docker.model.DockerDigest
import com.tencent.bkrepo.docker.response.CatalogResponse
import com.tencent.bkrepo.docker.response.DockerResponse
import com.tencent.bkrepo.docker.response.TagsResponse
import com.tencent.bkrepo.docker.util.BlobUtil
import com.tencent.bkrepo.docker.util.RepoUtil
import com.tencent.bkrepo.docker.util.ResponseUtil
import com.tencent.bkrepo.docker.util.ResponseUtil.emptyBlobGetResponse
import com.tencent.bkrepo.docker.util.ResponseUtil.emptyBlobHeadResponse
import com.tencent.bkrepo.docker.util.ResponseUtil.isEmptyBlob
import org.apache.commons.lang.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.CONTENT_LENGTH
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.nio.charset.Charset

/**
 * docker v2 protocol to work with
 * local storage
 */
@Service
class DockerV2LocalRepoService @Autowired constructor(val repo: DockerArtifactRepo) : DockerV2RepoService {

    var httpHeaders: HttpHeaders = HttpHeaders()
    val manifestProcess = ManifestProcess(repo)

    override fun ping(): DockerResponse {
        return ResponseEntity.ok().apply {
            header(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        }.apply {
            header(DOCKER_HEADER_API_VERSION, DOCKER_API_VERSION)
        }.body("{}")
    }

    override fun getTags(context: RequestContext, maxEntries: Int, lastEntry: String): DockerResponse {
        RepoUtil.loadContext(repo, context)
        val elementsHolder = DockerPaginationElementsHolder()
        val manifests = repo.getArtifactListByName(context.projectId, context.repoName, DOCKER_MANIFEST)

        if (manifests.isEmpty()) {
            return DockerV2Errors.nameUnknown(context.artifactName)
        }
        manifests.forEach {
            val path = it[DOCKER_NODE_PATH] as String
            val tagName = path.apply {
                replaceAfterLast(SLASH, EMPTY)
            }.apply {
                removeSuffix(SLASH)
            }.apply {
                removePrefix(SLASH + context.artifactName + SLASH)
            }
            elementsHolder.elements.add(tagName)
        }

        if (elementsHolder.elements.isEmpty()) {
            return DockerV2Errors.nameUnknown(context.artifactName)
        }
        DockerCatalogTagsSlicer.sliceCatalog(elementsHolder, maxEntries, lastEntry)
        val shouldAddLinkHeader = elementsHolder.hasMoreElements
        val tagsResponse = TagsResponse(elementsHolder, context.artifactName)
        httpHeaders.set(DOCKER_HEADER_API_VERSION, DOCKER_API_VERSION)
        if (shouldAddLinkHeader) {
            val last = tagsResponse.tags.last() as String
            val name = context.artifactName
            val link = "</v2/$name/tags/list?last=$last&n=$maxEntries>; rel=\"next\""
            httpHeaders.set(DOCKER_LINK, link)
        }
        return ResponseEntity(tagsResponse, httpHeaders, HttpStatus.OK)
    }

    override fun catalog(context: RequestContext, maxEntries: Int, lastEntry: String): DockerResponse {
        RepoUtil.loadContext(repo, context)
        val manifests = repo.getArtifactListByName(context.projectId, context.repoName, DOCKER_MANIFEST)
        val elementsHolder = DockerPaginationElementsHolder()

        manifests.forEach {
            val path = it[DOCKER_NODE_PATH] as String
            val repoName = path.replaceAfterLast(SLASH, EMPTY).replaceAfterLast(SLASH, EMPTY).removeSuffix(SLASH)
            if (repoName.isNotBlank()) {
                elementsHolder.addElement(repoName)
            }
            DockerCatalogTagsSlicer.sliceCatalog(elementsHolder, maxEntries, lastEntry)
        }
        val shouldAddLinkHeader = elementsHolder.hasMoreElements
        val catalogResponse = CatalogResponse(elementsHolder)
        httpHeaders.set(DOCKER_HEADER_API_VERSION, DOCKER_API_VERSION)
        if (shouldAddLinkHeader) {
            val last = catalogResponse.repositories.last() as String
            httpHeaders.set(DOCKER_LINK, "</v2/_catalog?last=$last&n=$maxEntries>; rel=\"next\"")
        }
        return ResponseEntity(catalogResponse, httpHeaders, HttpStatus.OK)
    }

    override fun getManifest(context: RequestContext, reference: String): DockerResponse {
        RepoUtil.loadContext(repo, context)
        logger.info("get manifest params [$context,$reference]")
        return try {
            val digest = DockerDigest(reference)
            manifestProcess.getManifestByDigest(context, digest, httpHeaders)
        } catch (exception: IllegalArgumentException) {
            logger.warn("unable to parse digest, get manifest by tag [$context,$reference]")
            manifestProcess.getManifestByTag(context, reference, httpHeaders)
        }
    }

    override fun getManifestString(context: RequestContext, tag: String): String {
        RepoUtil.loadContext(repo, context)
        val useManifestType = manifestProcess.chooseManifestType(context, tag, httpHeaders)
        val manifestPath = ResponseUtil.buildManifestPath(context.artifactName, tag, useManifestType)
        val manifest = repo.getArtifact(context.projectId, context.repoName, manifestPath) ?: run {
            logger.warn("node not exist [$context]")
            return EMPTY
        }
        val downloadContext = DownloadContext(context).sha256(manifest.sha256!!).length(manifest.length)
        val inputStream = repo.download(downloadContext)
        return inputStream.readBytes().toString(Charset.defaultCharset())
    }

    override fun getRepoList(context: RequestContext): List<String> {
        RepoUtil.loadContext(repo, context)
        return repo.getDockerArtifactList(context.projectId, context.repoName)
    }

    override fun getRepoTagList(context: RequestContext): Map<String, String> {
        RepoUtil.loadContext(repo, context)
        return repo.getRepoTagList(context)
    }

    override fun buildLayerResponse(context: RequestContext, layerId: String): DockerResponse {
        RepoUtil.loadContext(repo, context)
        val digest = DockerDigest(layerId)
        val artifact = repo.getBlobListByDigest(context.projectId, context.repoName, digest.fileName()) ?: run {
            logger.warn("user [$context]  get artifact  [$this] fail: [$layerId] not found")
            throw DockerRepoNotFoundException(layerId)
        }
        logger.info("get blob info [$context, $artifact]")
        val length = artifact[0][DOCKER_NODE_SIZE] as Int
        val downloadContext = DownloadContext(context).sha256(digest.getDigestHex()).length(length.toLong())
        val inputStreamResource = InputStreamResource(repo.download(downloadContext))
        with(context) {
            val contentType = manifestProcess.getManifestType(projectId, repoName, artifactName)
            httpHeaders.apply {
                set(DOCKER_HEADER_API_VERSION, DOCKER_API_VERSION)
            }.apply {
                set(DOCKER_HEADER_API_VERSION, DOCKER_API_VERSION)
            }.apply {
                set(CONTENT_TYPE, contentType)
            }
            return ResponseEntity.ok().headers(httpHeaders).contentLength(downloadContext.length).body(inputStreamResource)
        }
    }

    override fun deleteManifest(context: RequestContext, reference: String): DockerResponse {
        RepoUtil.loadContext(repo, context)
        return try {
            deleteManifestByDigest(context, DockerDigest(reference))
        } catch (exception: IllegalArgumentException) {
            logger.warn("unable to parse digest, delete manifest by tag [$context,$reference]")
            deleteManifestByTag(context, reference)
        }
    }

    override fun uploadManifest(
        context: RequestContext,
        tag: String,
        mediaType: String,
        file: ArtifactFile
    ): DockerResponse {
        RepoUtil.loadContext(repo, context)
        if (!repo.canWrite(context)) {
            logger.warn("unable to upload manifest [$context]")
            return DockerV2Errors.unauthorizedUpload()
        }
        val manifestType = ManifestType.from(mediaType)
        val manifestPath = ResponseUtil.buildManifestPath(context.artifactName, tag, manifestType)
        logger.info("upload manifest path [$context,$tag] ,media [$mediaType , manifestPath]")
        val digest = manifestProcess.uploadManifestByType(context, tag, manifestPath, manifestType, file)
        return ResponseEntity.status(HttpStatus.CREATED).apply {
            header(DOCKER_HEADER_API_VERSION, DOCKER_API_VERSION)
        }.apply {
            header(DOCKER_CONTENT_DIGEST, digest.toString())
        }.build()
    }

    // check is a blob file exist in this repo
    override fun isBlobExists(context: RequestContext, digest: DockerDigest): DockerResponse {
        RepoUtil.loadContext(repo, context)
        logger.info("check blob exist [$context, $digest]")
        if (isEmptyBlob(digest)) {
            logger.info("check is empty blob [$context, $digest]")
            return emptyBlobHeadResponse()
        }
        val blob = BlobUtil.getBlobFromRepo(repo, context, digest.fileName()) ?: run {
            logger.info("get blob from repo [$context, $digest] empty")
            return DockerV2Errors.blobUnknown(digest.toString())
        }
        return ResponseEntity.ok().apply {
            header(DOCKER_HEADER_API_VERSION, DOCKER_API_VERSION)
        }.apply {
            header(DOCKER_CONTENT_DIGEST, digest.toString())
        }.apply {
            header(CONTENT_LENGTH, blob.length.toString())
        }.apply {
            header(CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
        }.build<Any>()
    }

    // get a blob file
    override fun getBlob(context: RequestContext, digest: DockerDigest): DockerResponse {
        RepoUtil.loadContext(repo, context)
        if (isEmptyBlob(digest)) {
            logger.info("get empty layer for image [$context, $digest]")
            return emptyBlobGetResponse()
        }
        val blob = BlobUtil.getBlobByName(repo, context, digest.fileName()) ?: run {
            logger.info("get blob globally [$context,$digest] empty")
            return DockerV2Errors.blobUnknown(digest.toString())
        }
        logger.info("get blob [$digest] from repo [${context.artifactName}] ,length [${blob.length}]")
        val downloadContext = DownloadContext(context).sha256(digest.getDigestHex()).length(blob.length)
        val inputStream = repo.download(downloadContext)
        httpHeaders.apply {
            set(DOCKER_HEADER_API_VERSION, DOCKER_API_VERSION)
        }.apply {
            set(DOCKER_CONTENT_DIGEST, digest.toString())
        }
        val resource = InputStreamResource(inputStream)
        return ResponseEntity.ok().apply {
            headers(httpHeaders)
        }.apply {
            contentLength(blob.length)
        }.apply {
            contentType(MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE))
        }.body(resource)
    }

    // start upload a blob file
    override fun startBlobUpload(context: RequestContext, mount: String?): DockerResponse {
        RepoUtil.loadContext(repo, context)
        logger.info("start upload blob : [$context]")
        if (!repo.canWrite(context)) {
            logger.warn("start blob upload unauthorizedUpload [$context]")
            return DockerV2Errors.unauthorizedUpload()
        }
        mount?.let {
            val mountDigest = DockerDigest(mount)
            val mountableBlob = BlobUtil.getBlobByName(repo, context, mountDigest.fileName())
            mountableBlob?.let {
                val location = ResponseUtil.getDockerURI("${context.artifactName}$BLOB_PATTERN/$mount", httpHeaders)
                logger.info("found accessible blob at [$mountableBlob] to mount  [$context,$mount]")
                return ResponseEntity.status(HttpStatus.CREATED).apply {
                    header(DOCKER_HEADER_API_VERSION, DOCKER_API_VERSION)
                }.apply {
                    header(DOCKER_CONTENT_DIGEST, mount)
                }.apply {
                    header(CONTENT_LENGTH, DOCKER_LENGTH_EMPTY)
                }.apply {
                    header(HttpHeaders.LOCATION, location.toString())
                }.build()
            }
        }
        val uuid = repo.startAppend(context)
        var startUrl: String
        with(context) {
            startUrl = "$projectId/$repoName/$artifactName/blobs/uploads/$uuid"
        }

        val location = ResponseUtil.getDockerURI(startUrl, httpHeaders)
        return ResponseEntity.status(HttpStatus.ACCEPTED).apply {
            header(DOCKER_HEADER_API_VERSION, DOCKER_API_VERSION)
        }.apply {
            header(DOCKER_UPLOAD_UUID, uuid)
        }.apply {
            header(HttpHeaders.LOCATION, location.toString())
        }.build()
    }

    // upload a blob file
    override fun uploadBlob(context: RequestContext, digest: DockerDigest, uuid: String, file: ArtifactFile): DockerResponse {
        logger.info("upload blob [$context,$digest ,$uuid]")
        RepoUtil.loadContext(repo, context)
        return if (ResponseUtil.putHasStream(httpHeaders)) {
            uploadBlobFromPut(context, digest, file)
        } else {
            finishPatchUpload(context, digest, uuid)
        }
    }

    // patch upload file
    override fun patchUpload(context: RequestContext, uuid: String, file: ArtifactFile): DockerResponse {
        RepoUtil.loadContext(repo, context)
        with(context) {
            logger.info("patch upload blob [$context, $uuid]")
            val appendId = repo.writeAppend(context, uuid, file)
            val url = "$projectId/$repoName/$artifactName/blobs/uploads/$uuid"
            val location = ResponseUtil.getDockerURI(url, httpHeaders)
            return ResponseEntity.status(HttpStatus.ACCEPTED).apply {
                header(CONTENT_LENGTH, DOCKER_LENGTH_EMPTY)
            }.apply {
                header(DOCKER_HEADER_API_VERSION, DOCKER_API_VERSION)
            }.apply {
                header(DOCKER_UPLOAD_UUID, uuid)
            }.apply {
                header(HttpHeaders.LOCATION, location.toString())
            }.apply {
                header(DOCKER_UPLOAD_UUID, uuid)
            }.apply {
                header(HttpHeaders.RANGE, "0-" + (appendId - 1L))
            }.build()
        }
    }

    // delete a manifest file by tag first
    private fun deleteManifestByTag(context: RequestContext, tag: String): DockerResponse {
        with(context) {
            val tagPath = "$artifactName/$tag"
            val manifestPath = "$tagPath/$DOCKER_MANIFEST"
            if (!repo.exists(projectId, repoName, manifestPath)) {
                logger.warn("repo not exist [$context]")
                return DockerV2Errors.manifestUnknown(manifestPath)
            } else if (repo.delete(tagPath)) {
                return ResponseEntity.status(HttpStatus.ACCEPTED).apply {
                    header(DOCKER_HEADER_API_VERSION, DOCKER_API_VERSION)
                }.build()
            }
            return DockerV2Errors.manifestUnknown(manifestPath)
        }
    }

    // delete a manifest file by digest then
    private fun deleteManifestByDigest(context: RequestContext, digest: DockerDigest): DockerResponse {
        logger.info("delete docker manifest for digest [$context, $digest] ")
        val manifests = repo.getArtifactListByName(context.projectId, context.repoName, DOCKER_MANIFEST)
        val manifestIter = manifests.iterator()

        while (manifestIter.hasNext()) {
            val manifest = manifestIter.next()
            val fullPath = manifest[DOCKER_NODE_FULL_PATH] as String
            if (!repo.canWrite(context)) {
                return DockerV2Errors.manifestUnknown(digest.toString())
            }
            with(context) {
                val manifestDigest = repo.getAttribute(projectId, repoName, fullPath, digest.getDigestAlg())
                manifestDigest.let {
                    if (StringUtils.equals(manifestDigest, digest.getDigestHex())) {
                        repo.delete(fullPath)
                    }
                }
            }
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).header(DOCKER_HEADER_API_VERSION, DOCKER_API_VERSION).build()
    }

    // upload not with patch but direct from the put
    private fun uploadBlobFromPut(context: RequestContext, digest: DockerDigest, file: ArtifactFile): DockerResponse {
        val blobPath = context.artifactName + SLASH + DOCKER_TMP_UPLOAD_PATH + SLASH + digest.fileName()
        if (!repo.canWrite(context)) {
            logger.warn("upload manifest fail [$context , $digest]")
            return ResponseUtil.consumeStreamAndReturnError(file.getInputStream())
        }
        logger.info("deploy docker blob [$blobPath] into [$context]")
        val uploadContext = UploadContext(context.projectId, context.repoName, blobPath).sha256(digest.getDigestHex()).artifactFile(file)
        if (!repo.upload(uploadContext)) {
            logger.warn("error upload blob [$blobPath]")
            return DockerV2Errors.blobUploadInvalid(context.artifactName)
        }
        val location = ResponseUtil.getDockerURI("${context.artifactName}$BLOB_PATTERN/$digest", httpHeaders)
        return ResponseEntity.created(location).apply {
            header(DOCKER_HEADER_API_VERSION, DOCKER_API_VERSION)
        }.apply {
            header(DOCKER_CONTENT_DIGEST, digest.toString())
        }.build()
    }

    // the response after finish patch upload
    private fun finishPatchUpload(context: RequestContext, digest: DockerDigest, uuid: String): DockerResponse {
        logger.debug("finish upload blob [$context, $digest, $uuid]")
        val fileName = digest.fileName()
        var url: String
        val blobPath = "/${context.artifactName}/$DOCKER_TMP_UPLOAD_PATH/$fileName"
        val uploadContext = UploadContext(context.projectId, context.repoName, blobPath)
        repo.finishAppend(uploadContext, uuid)
        with(context) {
            url = "$projectId/$repoName/$artifactName$BLOB_PATTERN/$digest"
        }
        val location = ResponseUtil.getDockerURI(url, httpHeaders)
        return ResponseEntity.created(location).apply {
            header(DOCKER_HEADER_API_VERSION, DOCKER_API_VERSION)
        }.apply {
            header(CONTENT_LENGTH, DOCKER_LENGTH_EMPTY)
        }.apply {
            header(DOCKER_CONTENT_DIGEST, digest.toString())
        }.build()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DockerV2LocalRepoService::class.java)
    }
}
