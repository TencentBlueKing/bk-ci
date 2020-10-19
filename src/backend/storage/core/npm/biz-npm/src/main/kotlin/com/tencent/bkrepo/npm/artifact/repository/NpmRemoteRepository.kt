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

package com.tencent.bkrepo.npm.artifact.repository

import com.google.gson.JsonObject
import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.pojo.configuration.remote.RemoteConfiguration
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactListContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactMigrateContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactSearchContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactTransferContext
import com.tencent.bkrepo.common.artifact.repository.remote.RemoteRepository
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.npm.constants.DIST
import com.tencent.bkrepo.npm.constants.ID
import com.tencent.bkrepo.npm.constants.NAME
import com.tencent.bkrepo.npm.constants.NPM_FILE_FULL_PATH
import com.tencent.bkrepo.npm.constants.NPM_PKG_FULL_PATH
import com.tencent.bkrepo.npm.constants.NPM_PKG_VERSION_FULL_PATH
import com.tencent.bkrepo.npm.constants.OBJECTS
import com.tencent.bkrepo.npm.constants.TARBALL
import com.tencent.bkrepo.npm.constants.VERSIONS
import com.tencent.bkrepo.npm.pojo.NpmSearchResponse
import com.tencent.bkrepo.npm.utils.GsonUtils
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import com.tencent.bkrepo.repository.util.NodeUtils
import io.undertow.util.BadRequestException
import okhttp3.Request
import okhttp3.Response
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.IOException
import java.io.InputStream
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class NpmRemoteRepository : RemoteRepository() {

    @Value("\${npm.tarball.prefix}")
    private val tarballPrefix: String = StringPool.SLASH

    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        getCacheArtifactResource(context)?.let { return it }
        val tgzFile = super.onDownload(context)
        installPkgVersionFile(context)
        return tgzFile
    }

    override fun generateRemoteDownloadUrl(context: ArtifactTransferContext): String {
        val remoteConfiguration = context.repositoryConfiguration as RemoteConfiguration
        val tarballPrefix = getTarballPrefix(context)
        val queryString = context.request.queryString
        val requestURL =
            context.request.requestURL.toString() + if (StringUtils.isNotEmpty(queryString)) "?$queryString" else ""
        context.contextAttributes[NPM_FILE_FULL_PATH] = requestURL.removePrefix(tarballPrefix)
        return requestURL.replace(tarballPrefix, remoteConfiguration.url.trimEnd('/'))
    }

    override fun getCacheNodeCreateRequest(context: ArtifactDownloadContext, artifactFile: ArtifactFile): NodeCreateRequest {
        val nodeCreateRequest = super.getCacheNodeCreateRequest(context, artifactFile)
        return nodeCreateRequest.copy(
            fullPath = context.contextAttributes[NPM_FILE_FULL_PATH] as String
        )
    }

    private fun getCacheArtifactResource(context: ArtifactTransferContext): ArtifactResource? {
        val remoteConfiguration = context.repositoryConfiguration as RemoteConfiguration
        val cacheConfiguration = remoteConfiguration.cacheConfiguration
        if (!cacheConfiguration.cacheEnabled) return null
        val repositoryInfo = context.repositoryInfo
        val fullPath = context.contextAttributes[NPM_FILE_FULL_PATH] as String
        val node = nodeClient.detail(repositoryInfo.projectId, repositoryInfo.name, fullPath).data
        if (node == null || node.folder) return null
        val createdDate = LocalDateTime.parse(node.createdDate, DateTimeFormatter.ISO_DATE_TIME)
        val age = Duration.between(createdDate, LocalDateTime.now()).toMinutes()
        return if (age <= cacheConfiguration.cachePeriod) {
            storageService.load(
                node.sha256!!, Range.full(node.size),
                context.storageCredentials
            )?.run {
                logger.debug("Cached remote artifact[${context.artifactInfo}] is hit")
                ArtifactResource(this, determineArtifactName(context), node)
            }
        } else null
    }

    override fun determineArtifactName(context: ArtifactTransferContext): String {
        val fullPath = context.contextAttributes[NPM_FILE_FULL_PATH] as String
        return NodeUtils.getName(fullPath)
    }

    /**
     * install pkg-version json file when download tgzFile
     */
    fun installPkgVersionFile(context: ArtifactDownloadContext) {
        val tgzFullPath = context.contextAttributes[NPM_FILE_FULL_PATH] as String
        val pkgInfo = parseArtifactInfo(tgzFullPath)
        context.contextAttributes[NPM_FILE_FULL_PATH] =
            String.format(NPM_PKG_FULL_PATH, pkgInfo.first)
        try {
            val artifactResource = getCacheArtifactResource(context) ?: return
            val jsonFile = transFileToJson(artifactResource.inputStream)
            val versionFile = jsonFile.getAsJsonObject(VERSIONS).getAsJsonObject(pkgInfo.second)
            val artifact = ArtifactFileFactory.build(GsonUtils.gsonToInputStream(versionFile))
            val name = jsonFile[NAME].asString
            context.contextAttributes[NPM_FILE_FULL_PATH] =
                String.format(NPM_PKG_VERSION_FULL_PATH, name, name, pkgInfo.second)
            putArtifactCache(context, artifact)
        } catch (ex: TypeCastException) {
            logger.warn("cache artifact [${pkgInfo.first}-${pkgInfo.second}.json] failed, {}", ex.message)
        }
    }

    private fun parseArtifactInfo(tgzFullPath: String): Pair<String, String> {
        val pkgList = tgzFullPath.split('/').filter { it.isNotBlank() }.map { it.trim() }.toList()
        var pkgName = pkgList[0]
        if (pkgList[1].contains('@')) {
            pkgName = pkgList[0] + pkgList[1]
        }
        val version = pkgList.last().substringAfterLast('-').substringBeforeLast(".tgz")
        return Pair(pkgName, version)
    }

    override fun search(context: ArtifactSearchContext): JsonObject? {
        getCacheArtifactResource(context)?.let {
            return transFileToJson(it.inputStream)
        }
        val remoteConfiguration = context.repositoryConfiguration as RemoteConfiguration
        val httpClient = createHttpClient(remoteConfiguration)
        val searchUri = generateRemoteSearchUrl(context)
        val request = Request.Builder().url(searchUri).build()
        var response: Response? = null
        return try {
            response = httpClient.newCall(request).execute()
            if (checkResponse(response)) {
                val file = createTempFile(response.body()!!)
                val downloadContext = ArtifactDownloadContext()
                downloadContext.contextAttributes = context.contextAttributes
                val resultJson = transFileToJson(file.getInputStream())
                putArtifactCache(downloadContext, file)
                resultJson
            } else null
        } catch (exception: IOException) {
            logger.error("http send [$searchUri] failed, {}", exception.message)
            throw exception
        } finally {
            if (response != null) {
                response.body()?.close()
            }
        }
    }

    private fun transFileToJson(inputStream: InputStream): JsonObject {
        val pkgJson = GsonUtils.transferInputStreamToJson(inputStream)
        val name = pkgJson.get(NAME).asString
        val id = pkgJson[ID].asString
        if (id.substring(1).contains('@')) {
            val oldTarball = pkgJson.getAsJsonObject(DIST)[TARBALL].asString
            val prefix = oldTarball.split(name)[0].trimEnd('/')
            val newTarball = oldTarball.replace(prefix, tarballPrefix.trimEnd('/'))
            pkgJson.getAsJsonObject(DIST).addProperty(TARBALL, newTarball)
        } else {
            val versions = pkgJson.getAsJsonObject(VERSIONS)
            versions.keySet().forEach {
                val versionObject = versions.getAsJsonObject(it)
                val oldTarball = versionObject.getAsJsonObject(DIST)[TARBALL].asString
                val prefix = oldTarball.split(name)[0].trimEnd('/')
                val newTarball = oldTarball.replace(prefix, tarballPrefix.trimEnd('/'))
                versionObject.getAsJsonObject(DIST).addProperty(TARBALL, newTarball)
            }
        }
        return pkgJson
    }

    private fun getTarballPrefix(context: ArtifactTransferContext): String {
        val requestURL = context.request.requestURL.toString()
        val requestURI = context.request.requestURI
        val projectId = context.artifactInfo.projectId
        val repoName = context.artifactInfo.repoName
        val replace = requestURL.replace(requestURI, "")
        return "$replace/$projectId/$repoName"
    }

    private fun generateRemoteSearchUrl(context: ArtifactSearchContext): String {
        val remoteConfiguration = context.repositoryConfiguration as RemoteConfiguration
        val tarballPrefix = getTarballPrefix(context)
        val requestURL = context.request.requestURL.toString()
        return requestURL.replace(tarballPrefix, remoteConfiguration.url.trimEnd('/'))
    }

    override fun list(context: ArtifactListContext): NpmSearchResponse {
        val remoteConfiguration = context.repositoryConfiguration as RemoteConfiguration
        val httpClient = createHttpClient(remoteConfiguration)
        val downloadUri = generateRemoteDownloadUrl(context)
        val request = Request.Builder().url(downloadUri).build()
        val response = httpClient.newCall(request).execute()
        return if (checkResponse(response)) {
            NpmSearchResponse(
                GsonUtils.gsonToMaps<MutableList<Map<String, Any>>>(response.body()!!.string())?.get(OBJECTS)!!
            )
        } else NpmSearchResponse()
    }

    override fun migrate(context: ArtifactMigrateContext) {
        logger.warn("Unable to migrate npm package into a remote repository")
        throw BadRequestException("Unable to migrate npm package into a remote repository")
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(NpmRemoteRepository::class.java)
    }
}
