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

package com.tencent.bkrepo.npm.service

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.artifact.api.ArtifactFileMap
import com.tencent.bkrepo.common.artifact.constant.OCTET_STREAM
import com.tencent.bkrepo.common.artifact.exception.ArtifactNotFoundException
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactListContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactSearchContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.context.RepositoryHolder
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.async.NpmDependentHandler
import com.tencent.bkrepo.npm.constants.APPLICATION_OCTET_STEAM
import com.tencent.bkrepo.npm.constants.ATTACHMENTS
import com.tencent.bkrepo.npm.constants.ATTRIBUTE_OCTET_STREAM_SHA1
import com.tencent.bkrepo.npm.constants.CONTENT_TYPE
import com.tencent.bkrepo.npm.constants.CREATED
import com.tencent.bkrepo.npm.constants.DATA
import com.tencent.bkrepo.npm.constants.DIST
import com.tencent.bkrepo.npm.constants.DISTTAGS
import com.tencent.bkrepo.npm.constants.ERROR_MAP
import com.tencent.bkrepo.npm.constants.FILE_DASH
import com.tencent.bkrepo.npm.constants.FILE_SUFFIX
import com.tencent.bkrepo.npm.constants.LATEST
import com.tencent.bkrepo.npm.constants.MODIFIED
import com.tencent.bkrepo.npm.constants.NAME
import com.tencent.bkrepo.npm.constants.NPM_FILE_FULL_PATH
import com.tencent.bkrepo.npm.constants.NPM_METADATA
import com.tencent.bkrepo.npm.constants.NPM_PACKAGE_JSON_FILE
import com.tencent.bkrepo.npm.constants.NPM_PACKAGE_TGZ_FILE
import com.tencent.bkrepo.npm.constants.NPM_PACKAGE_VERSION_JSON_FILE
import com.tencent.bkrepo.npm.constants.NPM_PKG_FULL_PATH
import com.tencent.bkrepo.npm.constants.NPM_PKG_JSON_FILE_FULL_PATH
import com.tencent.bkrepo.npm.constants.NPM_PKG_TGZ_FILE_FULL_PATH
import com.tencent.bkrepo.npm.constants.NPM_PKG_TGZ_FULL_PATH
import com.tencent.bkrepo.npm.constants.NPM_PKG_VERSION_FULL_PATH
import com.tencent.bkrepo.npm.constants.NPM_PKG_VERSION_JSON_FILE_FULL_PATH
import com.tencent.bkrepo.npm.constants.REV
import com.tencent.bkrepo.npm.constants.REV_VALUE
import com.tencent.bkrepo.npm.constants.SEARCH_REQUEST
import com.tencent.bkrepo.npm.constants.SHASUM
import com.tencent.bkrepo.npm.constants.TIME
import com.tencent.bkrepo.npm.constants.VERSION
import com.tencent.bkrepo.npm.constants.VERSIONS
import com.tencent.bkrepo.npm.exception.NpmArtifactExistException
import com.tencent.bkrepo.npm.exception.NpmArtifactNotFoundException
import com.tencent.bkrepo.npm.pojo.NpmDeleteResponse
import com.tencent.bkrepo.npm.pojo.NpmMetaData
import com.tencent.bkrepo.npm.pojo.NpmSearchResponse
import com.tencent.bkrepo.npm.pojo.NpmSuccessResponse
import com.tencent.bkrepo.npm.pojo.enums.NpmOperationAction
import com.tencent.bkrepo.npm.pojo.metadata.MetadataSearchRequest
import com.tencent.bkrepo.npm.utils.BeanUtils
import com.tencent.bkrepo.npm.utils.GsonUtils
import com.tencent.bkrepo.npm.utils.TimeUtil
import com.tencent.bkrepo.repository.api.MetadataClient
import com.tencent.bkrepo.repository.pojo.metadata.MetadataSaveRequest
import org.apache.commons.codec.binary.Base64
import org.apache.commons.lang.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NpmService @Autowired constructor(
    private val npmDependentHandler: NpmDependentHandler,
    private val metadataClient: MetadataClient
) {

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    @Transactional(rollbackFor = [Throwable::class])
    fun publish(userId: String, artifactInfo: NpmArtifactInfo, body: String): NpmSuccessResponse {
        body.takeIf { StringUtils.isNotBlank(it) } ?: throw ArtifactNotFoundException("request body not found!")
        val jsonObj = JsonParser.parseString(body).asJsonObject
        val artifactFileMap = ArtifactFileMap()
        return if (jsonObj.has(ATTACHMENTS)) {
            val attributesMap = mutableMapOf<String, Any>()
            buildTgzFile(artifactFileMap, jsonObj, attributesMap)
            buildPkgVersionFile(artifactFileMap, jsonObj, attributesMap)
            buildPkgFile(artifactInfo, artifactFileMap, jsonObj)
            val context = ArtifactUploadContext(artifactFileMap)
            context.contextAttributes = attributesMap
            val repository = RepositoryHolder.getRepository(context.repositoryInfo.category)
            repository.upload(context)
            npmDependentHandler.updatePkgDepts(userId, artifactInfo, jsonObj, NpmOperationAction.PUBLISH)
            NpmSuccessResponse.createEntitySuccess()
        } else {
            unPublishOperation(artifactInfo, jsonObj)
            NpmSuccessResponse.updatePkgSuccess()
        }
    }

    private fun unPublishOperation(artifactInfo: NpmArtifactInfo, jsonObj: JsonObject) {
        // 非publish操作 deprecate等操作
        val versions = jsonObj.getAsJsonObject(VERSIONS)
        versions.keySet().forEach {
            val name = jsonObj.get(NAME).asString
            val version = versions.getAsJsonObject(it)[VERSION].asString
            val metaData = buildMetaData(versions[it].asJsonObject)
            val tgzFullPath = String.format(NPM_PKG_TGZ_FULL_PATH, name, name, version)
            metadataClient.save(
                MetadataSaveRequest(
                    artifactInfo.projectId,
                    artifactInfo.repoName,
                    tgzFullPath,
                    metaData
                )
            )
        }
    }

    /**
     * 构造package.json文件
     */
    private fun buildPkgFile(artifactInfo: NpmArtifactInfo, artifactFileMap: ArtifactFileMap, jsonObj: JsonObject) {
        // 读取package.json文件去追加内容
        var pkgInfo = searchPackageInfo(artifactInfo) ?: JsonObject()
        val leastJsonObject = jsonObj.getAsJsonObject(VERSIONS)

        val distTags = getDistTags(jsonObj)!!
        if (pkgInfo.size() > 0 && pkgInfo.getAsJsonObject(VERSIONS).has(distTags.second)) {
            throw NpmArtifactExistException("cannot modify pre-existing version: ${distTags.second}.")
        }

        // first upload
        val gmtTime = TimeUtil.getGMTTime()
        val timeMap = if (pkgInfo.size() == 0) pkgInfo else pkgInfo.getAsJsonObject(TIME)!!
        if (pkgInfo.size() == 0) {
            jsonObj.addProperty(REV, REV_VALUE)
            pkgInfo = jsonObj
            timeMap.add(CREATED, GsonUtils.gson.toJsonTree(gmtTime))
        }

        pkgInfo.getAsJsonObject(VERSIONS).add(distTags.second, leastJsonObject.getAsJsonObject(distTags.second))
        pkgInfo.getAsJsonObject(DISTTAGS).addProperty(distTags.first, distTags.second)
        timeMap.add(distTags.second, GsonUtils.gson.toJsonTree(gmtTime))
        timeMap.add(MODIFIED, GsonUtils.gson.toJsonTree(gmtTime))
        pkgInfo.add(TIME, timeMap)
        val packageJsonFile = ArtifactFileFactory.build(GsonUtils.gsonToInputStream(pkgInfo))
        artifactFileMap[NPM_PACKAGE_JSON_FILE] = packageJsonFile
    }

    private fun getDistTags(jsonObj: JsonObject): Pair<String, String>? {
        val distTags = jsonObj.getAsJsonObject(DISTTAGS)
        distTags.entrySet().forEach {
            return Pair(it.key, it.value.asString)
        }
        return null
    }

    /**
     * 构造pkgName-version.json文件
     */
    private fun buildPkgVersionFile(
        artifactFileMap: ArtifactFileMap,
        jsonObj: JsonObject,
        attributesMap: MutableMap<String, Any>
    ) {
        val distTags = getDistTags(jsonObj)!!
        val name = jsonObj.get(NAME).asString
        val versionJsonObj = jsonObj.getAsJsonObject(VERSIONS).getAsJsonObject(distTags.second)
        val packageJsonWithVersionFile = ArtifactFileFactory.build(
            GsonUtils.gson.toJson(versionJsonObj).byteInputStream()
        )
        artifactFileMap[NPM_PACKAGE_VERSION_JSON_FILE] = packageJsonWithVersionFile
        // 添加相关属性
        attributesMap[ATTRIBUTE_OCTET_STREAM_SHA1] = versionJsonObj.getAsJsonObject(DIST).get(SHASUM).asString
        attributesMap[NPM_METADATA] = buildMetaData(versionJsonObj)
        attributesMap[NPM_PKG_VERSION_JSON_FILE_FULL_PATH] =
            String.format(NPM_PKG_VERSION_FULL_PATH, name, name, distTags.second)
        attributesMap[NPM_PKG_JSON_FILE_FULL_PATH] = String.format(NPM_PKG_FULL_PATH, name)
    }

    private fun buildMetaData(versionJsonObj: JsonObject): Map<String, String> {
        val metaData = GsonUtils.gson.fromJson(versionJsonObj, NpmMetaData::class.java)
        return BeanUtils.beanToMap(metaData)
    }

    /**
     * 构造pkgName-version.tgz文件
     */
    private fun buildTgzFile(
        artifactFileMap: ArtifactFileMap,
        jsonObj: JsonObject,
        attributesMap: MutableMap<String, Any>
    ) {
        val attachments = getAttachmentsInfo(jsonObj, attributesMap)
        val tgzFile = ArtifactFileFactory.build(Base64.decodeBase64(attachments.get(DATA)?.asString).inputStream())
        artifactFileMap[NPM_PACKAGE_TGZ_FILE] = tgzFile
    }

    /**
     * 获取文件模块相关信息，最后将文件信息移除（data量容易过大）
     */
    private fun getAttachmentsInfo(jsonObj: JsonObject, attributesMap: MutableMap<String, Any>): JsonObject {
        val distTags = getDistTags(jsonObj)!!
        val name = jsonObj.get(NAME).asString
        logger.info("current pkgName : $name ,current version : ${distTags.second}")
        val attachKey = "$name$FILE_DASH${distTags.second}$FILE_SUFFIX"
        val mutableMap = jsonObj.getAsJsonObject(ATTACHMENTS).getAsJsonObject(attachKey)
        attributesMap[NPM_PKG_TGZ_FILE_FULL_PATH] = String.format(NPM_PKG_TGZ_FULL_PATH, name, name, distTags.second)
        attributesMap[APPLICATION_OCTET_STEAM] = mutableMap.get(CONTENT_TYPE).asString
        jsonObj.remove(ATTACHMENTS)
        return mutableMap
    }

    @Permission(ResourceType.REPO, PermissionAction.READ)
    @Transactional(rollbackFor = [Throwable::class])
    fun searchPackageInfo(artifactInfo: NpmArtifactInfo): JsonObject? {
        if (StringUtils.equals(artifactInfo.version, LATEST)) {
            return searchLatestVersionMetadata(artifactInfo)
        }
        return searchVersionMetadata(artifactInfo)
    }

    private fun searchVersionMetadata(artifactInfo: NpmArtifactInfo): JsonObject? {
        val context = ArtifactSearchContext()
        context.contextAttributes[NPM_FILE_FULL_PATH] = getFileFullPath(artifactInfo)
        val repository = RepositoryHolder.getRepository(context.repositoryInfo.category)
        return repository.search(context) as? JsonObject
    }

    private fun searchLatestVersionMetadata(artifactInfo: NpmArtifactInfo): JsonObject? {
        with(artifactInfo) {
            val scopePkg = if (StringUtils.isEmpty(scope)) pkgName else "$scope/$pkgName"
            val fullPath = String.format(NPM_PKG_FULL_PATH, scopePkg)
            val context = ArtifactSearchContext()
            context.contextAttributes[NPM_FILE_FULL_PATH] = fullPath
            val repository = RepositoryHolder.getRepository(context.repositoryInfo.category)
            val npmMetaData = repository.search(context) as? JsonObject
                ?: throw NpmArtifactNotFoundException("document not found!")
            val latestPackageVersion = npmMetaData.getAsJsonObject(DISTTAGS)[LATEST].asString
            val npmArtifactInfo = NpmArtifactInfo(
                projectId, repoName, artifactUri, scope, pkgName, latestPackageVersion
            )
            return searchVersionMetadata(npmArtifactInfo)
        }
    }

    private fun getFileFullPath(artifactInfo: NpmArtifactInfo): String {
        val scope = artifactInfo.scope
        val pkgName = artifactInfo.pkgName
        val version = artifactInfo.version
        val scopePkg = if (StringUtils.isEmpty(scope)) pkgName else "$scope/$pkgName"
        return if (StringUtils.isEmpty(version)) String.format(NPM_PKG_FULL_PATH, scopePkg) else String.format(
            NPM_PKG_VERSION_FULL_PATH,
            scopePkg,
            scopePkg,
            version
        )
    }

    @Permission(ResourceType.REPO, PermissionAction.READ)
    @Transactional(rollbackFor = [Throwable::class])
    fun download(artifactInfo: NpmArtifactInfo) {
        val context = ArtifactDownloadContext()
        val requestURI = HttpContextHolder.getRequest().requestURI
        context.contextAttributes[NPM_FILE_FULL_PATH] = requestURI.substringAfterLast(artifactInfo.repoName)
        val repository = RepositoryHolder.getRepository(context.repositoryInfo.category)
        repository.download(context)
    }

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    fun unpublish(userId: String, artifactInfo: NpmArtifactInfo): NpmDeleteResponse {
        val fullPathList = mutableListOf<String>()
        val pkgInfo = searchPackageInfo(artifactInfo)
            ?: throw NpmArtifactNotFoundException("document not found")
        val name = pkgInfo[NAME].asString
        pkgInfo.getAsJsonObject(VERSIONS).keySet().forEach { version ->
            fullPathList.add(String.format(NPM_PKG_VERSION_FULL_PATH, name, name, version))
            fullPathList.add(String.format(NPM_PKG_TGZ_FULL_PATH, name, name, version))
        }
        fullPathList.add(String.format(NPM_PKG_FULL_PATH, name))
        val context = ArtifactRemoveContext()
        context.contextAttributes[NPM_FILE_FULL_PATH] = fullPathList
        val repository = RepositoryHolder.getRepository(context.repositoryInfo.category)
        repository.remove(context)
        npmDependentHandler.updatePkgDepts(userId, artifactInfo, pkgInfo, NpmOperationAction.UNPUBLISH)
        return NpmDeleteResponse(true, name, REV_VALUE)
    }

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    fun updatePkg(artifactInfo: NpmArtifactInfo, body: String): NpmSuccessResponse {
        val attributesMap = mutableMapOf<String, Any>()
        body.takeIf { StringUtils.isNotBlank(it) } ?: throw ArtifactNotFoundException("request body not found!")
        val jsonObj = JsonParser.parseString(body).asJsonObject
        val name = jsonObj.get(NAME).asString
        attributesMap[NPM_PKG_JSON_FILE_FULL_PATH] = String.format(NPM_PKG_FULL_PATH, name)

        val artifactFileMap = ArtifactFileMap()
        val pkgFile = ArtifactFileFactory.build(GsonUtils.gson.toJson(jsonObj).byteInputStream())
        artifactFileMap[NPM_PACKAGE_JSON_FILE] = pkgFile
        val context = ArtifactUploadContext(artifactFileMap)
        context.contextAttributes = attributesMap
        val repository = RepositoryHolder.getRepository(context.repositoryInfo.category)
        repository.upload(context)
        logger.info("update package $name success!")
        return NpmSuccessResponse.updatePkgSuccess()
    }

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    fun unPublishPkgWithVersion(artifactInfo: NpmArtifactInfo): NpmDeleteResponse {
        val fullPathList = mutableListOf<String>()
        val artifactUri = artifactInfo.artifactUri.substringAfterLast("/-/").substringBeforeLast("/-rev")
        val pkgName = artifactUri.substringBeforeLast('-')
        fullPathList.add("/$pkgName/-/$artifactUri")
        fullPathList.add("/.npm/$pkgName/${artifactUri.replace(".tgz", ".json")}")
        val context = ArtifactRemoveContext()
        context.contextAttributes[NPM_FILE_FULL_PATH] = fullPathList
        val repository = RepositoryHolder.getRepository(context.repositoryInfo.category)
        repository.remove(context)
        logger.info("delete package $artifactUri success")
        return NpmDeleteResponse(true, artifactUri, REV_VALUE)
    }

    @Permission(ResourceType.REPO, PermissionAction.READ)
    fun search(artifactInfo: NpmArtifactInfo, searchRequest: MetadataSearchRequest): NpmSearchResponse {
        val context = ArtifactListContext()
        context.contextAttributes[SEARCH_REQUEST] = searchRequest
        val repository = RepositoryHolder.getRepository(context.repositoryInfo.category)
        return repository.list(context) as NpmSearchResponse
    }

    @Permission(ResourceType.REPO, PermissionAction.READ)
    fun getDistTagsInfo(artifactInfo: NpmArtifactInfo): Map<String, String> {
        val context = ArtifactSearchContext()
        context.contextAttributes[NPM_FILE_FULL_PATH] =
            String.format(NPM_PKG_FULL_PATH, artifactInfo.artifactUri.trimStart('/').removeSuffix("/dist-tags"))
        val repository = RepositoryHolder.getRepository(context.repositoryInfo.category)
        val pkgInfo = repository.search(context) as? JsonObject
        return pkgInfo?.let {
            GsonUtils.gsonToMaps<String>(it.get(DISTTAGS))
        } ?: ERROR_MAP
    }

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    fun addDistTags(artifactInfo: NpmArtifactInfo, body: String): NpmSuccessResponse {
        val context = ArtifactSearchContext()
        val uriInfo = artifactInfo.artifactUri.split(DISTTAGS)
        val name = uriInfo[0].trimStart('/').trimEnd('/')
        val tag = uriInfo[1].trimStart('/')
        context.contextAttributes[NPM_FILE_FULL_PATH] = String.format(NPM_PKG_FULL_PATH, name)
        val repository = RepositoryHolder.getRepository(context.repositoryInfo.category)
        val pkgInfo = repository.search(context) as JsonObject
        pkgInfo.getAsJsonObject(DISTTAGS).addProperty(tag, body.replace("\"", ""))
        val artifactFile = ArtifactFileFactory.build(GsonUtils.gson.toJson(pkgInfo).byteInputStream())
        val uploadContext = ArtifactUploadContext(artifactFile)
        uploadContext.contextAttributes[OCTET_STREAM + "_full_path"] = String.format(NPM_PKG_FULL_PATH, name)
        repository.upload(uploadContext)
        return NpmSuccessResponse.createTagSuccess()
    }

    fun deleteDistTags(artifactInfo: NpmArtifactInfo) {
        val context = ArtifactSearchContext()
        val uriInfo = artifactInfo.artifactUri.split(DISTTAGS)
        val name = uriInfo[0].trimStart('/').trimEnd('/')
        val tag = uriInfo[1].trimStart('/')
        context.contextAttributes[NPM_FILE_FULL_PATH] = String.format(NPM_PKG_FULL_PATH, name)
        val repository = RepositoryHolder.getRepository(context.repositoryInfo.category)
        val pkgInfo = repository.search(context) as JsonObject
        pkgInfo.getAsJsonObject(DISTTAGS).remove(tag)
        val artifactFile = ArtifactFileFactory.build(GsonUtils.gson.toJson(pkgInfo).byteInputStream())
        val uploadContext = ArtifactUploadContext(artifactFile)
        uploadContext.contextAttributes[OCTET_STREAM + "_full_path"] = String.format(NPM_PKG_FULL_PATH, name)
        repository.upload(uploadContext)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(NpmService::class.java)
    }
}
