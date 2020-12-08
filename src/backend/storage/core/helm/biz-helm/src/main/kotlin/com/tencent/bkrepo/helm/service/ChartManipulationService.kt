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

package com.tencent.bkrepo.helm.service

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.artifact.api.ArtifactFileMap
import com.tencent.bkrepo.common.artifact.constant.OCTET_STREAM
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactRemoveContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.context.RepositoryHolder
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.resolve.file.multipart.MultipartArtifactFile
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.helm.artifact.HelmArtifactInfo
import com.tencent.bkrepo.helm.constants.CHART
import com.tencent.bkrepo.helm.constants.CHART_PACKAGE_FILE_EXTENSION
import com.tencent.bkrepo.helm.constants.FULL_PATH
import com.tencent.bkrepo.helm.constants.INDEX_CACHE_YAML
import com.tencent.bkrepo.helm.constants.NAME
import com.tencent.bkrepo.helm.constants.PROV
import com.tencent.bkrepo.helm.constants.PROVENANCE_FILE_EXTENSION
import com.tencent.bkrepo.helm.constants.VERSION
import com.tencent.bkrepo.helm.exception.HelmErrorInvalidProvenanceFileException
import com.tencent.bkrepo.helm.exception.HelmFileNotFoundException
import com.tencent.bkrepo.helm.pojo.HelmSuccessResponse
import com.tencent.bkrepo.helm.pojo.IndexEntity
import com.tencent.bkrepo.helm.utils.DecompressUtil.getArchivesContent
import com.tencent.bkrepo.helm.utils.YamlUtils
import com.tencent.bkrepo.repository.util.NodeUtils.FILE_SEPARATOR
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChartManipulationService {

    @Autowired
    private lateinit var chartRepositoryService: ChartRepositoryService

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    @Transactional(rollbackFor = [Throwable::class])
    fun uploadProv(artifactInfo: HelmArtifactInfo, artifactFileMap: ArtifactFileMap): HelmSuccessResponse {
        val context = ArtifactUploadContext(artifactFileMap)
        val repository = RepositoryHolder.getRepository(context.repositoryInfo.category)
        context.contextAttributes = getContextAttrMap(artifactFileMap = artifactFileMap)
        if (!artifactFileMap.keys.contains(PROV)) throw HelmFileNotFoundException("no package or provenance file found in form fields chart and prov")
        repository.upload(context)
        return HelmSuccessResponse.pushSuccess()
    }

    fun getContextAttrMap(
        artifactFileMap: ArtifactFileMap,
        chartFileInfo: Map<String, Any>? = null
    ): MutableMap<String, Any> {
        val attributesMap = mutableMapOf<String, Any>()
        artifactFileMap.entries.forEach { (name, _) ->
            if (CHART != name && PROV != name) {
                throw HelmFileNotFoundException("no package or provenance file found in form fields chart and prov")
            }
            if (CHART == name) {
                attributesMap[name + FULL_PATH] = getChartFileFullPath(chartFileInfo)
            }
            if (PROV == name) {
                attributesMap[name + FULL_PATH] = getProvFileFullPath(artifactFileMap)
            }
        }
        return attributesMap
    }

    fun getChartFileFullPath(chartFile: Map<String, Any>?): String {
        val chartName = chartFile?.get(NAME) as String
        val chartVersion = chartFile[VERSION] as String
        return String.format("$FILE_SEPARATOR%s-%s.%s", chartName, chartVersion, CHART_PACKAGE_FILE_EXTENSION)
    }

    private fun getProvFileFullPath(artifactFileMap: ArtifactFileMap): String {
        val inputStream = (artifactFileMap[PROV] as MultipartArtifactFile).getInputStream()
        val contentStr = String(inputStream.readBytes())
        val hasPGPBegin = contentStr.startsWith("-----BEGIN PGP SIGNED MESSAGE-----")
        val nameMatch = Regex("\nname:[ *](.+)").findAll(contentStr).toList().flatMap(MatchResult::groupValues)
        val versionMatch = Regex("\nversion:[ *](.+)").findAll(contentStr).toList().flatMap(MatchResult::groupValues)
        if (!hasPGPBegin || nameMatch.size != 2 || versionMatch.size != 2) {
            throw HelmErrorInvalidProvenanceFileException("invalid provenance file")
        }
        return provenanceFilenameFromNameVersion(nameMatch[1], versionMatch[1])
    }

    private fun provenanceFilenameFromNameVersion(name: String, version: String): String {
        return String.format("$FILE_SEPARATOR%s-%s.%s", name, version, PROVENANCE_FILE_EXTENSION)
    }

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    @Transactional(rollbackFor = [Throwable::class])
    fun upload(artifactInfo: HelmArtifactInfo, artifactFileMap: ArtifactFileMap): HelmSuccessResponse {
        val context = ArtifactUploadContext(artifactFileMap)
        val repository = RepositoryHolder.getRepository(context.repositoryInfo.category)
        val chartFileInfo = getChartFile(artifactFileMap)
        context.contextAttributes = getContextAttrMap(artifactFileMap, chartFileInfo)
        repository.upload(context)
        return HelmSuccessResponse.pushSuccess()
    }

    private fun getChartFile(artifactFileMap: ArtifactFileMap): MutableMap<String, Any> {
        if (!artifactFileMap.keys.contains(CHART)) throw HelmFileNotFoundException("no package or provenance file found in form fields chart and prov")
        val inputStream = (artifactFileMap[CHART] as MultipartArtifactFile).getInputStream()
        val result = inputStream.getArchivesContent("tgz")
        return YamlUtils.convertStringToEntity(result)
    }

    private fun uploadIndexYaml(indexEntity: IndexEntity) {
        val artifactFile = ArtifactFileFactory.build(YamlUtils.transEntityToStream(indexEntity))
        val uploadContext = ArtifactUploadContext(artifactFile)
        uploadContext.contextAttributes[OCTET_STREAM + FULL_PATH] = "$FILE_SEPARATOR$INDEX_CACHE_YAML"
        val uploadRepository = RepositoryHolder.getRepository(uploadContext.repositoryInfo.category)
        uploadRepository.upload(uploadContext)
    }

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    @Transactional(rollbackFor = [Throwable::class])
    fun delete(artifactInfo: HelmArtifactInfo): HelmSuccessResponse {
        chartRepositoryService.freshIndexFile(artifactInfo)
        val chartInfo = getChartInfo(artifactInfo)
        val context = ArtifactRemoveContext()
        val repository = RepositoryHolder.getRepository(context.repositoryInfo.category)
        val fullPath = String.format("/%s-%s.%s", chartInfo.first, chartInfo.second, CHART_PACKAGE_FILE_EXTENSION)
        context.contextAttributes[FULL_PATH] = fullPath
        repository.remove(context)
        logger.info("remove artifact [$fullPath] success!")
        freshIndexYamlForRemove(chartInfo)
        return HelmSuccessResponse.deleteSuccess()
    }

    fun getChartInfo(artifactInfo: HelmArtifactInfo): Pair<String, String> {
        val artifactUri = artifactInfo.artifactUri.trimStart('/')
        val name = artifactUri.substringBeforeLast('/')
        val version = artifactUri.substringAfterLast('/')
        return Pair(name, version)
    }

    private fun freshIndexYamlForRemove(chartInfo: Pair<String, String>) {
        try {
            val indexEntity = chartRepositoryService.getOriginalIndexYaml()
            indexEntity.entries.let {
                if (it[chartInfo.first]?.size == 1 && chartInfo.second == it[chartInfo.first]?.get(0)?.get(VERSION) as String) {
                    it.remove(chartInfo.first)
                } else {
                    run stop@{
                        it[chartInfo.first]?.forEachIndexed { index, chartMap ->
                            if (chartInfo.second == chartMap[VERSION] as String) {
                                it[chartInfo.first]?.removeAt(index)
                                return@stop
                            }
                        }
                    }
                }
            }
            uploadIndexYaml(indexEntity)
            logger.info("fresh index.yaml for delete [${chartInfo.first}-${chartInfo.second}.tgz] success!")
        } catch (exception: TypeCastException) {
            logger.error("fresh index.yaml for delete [${chartInfo.first}-${chartInfo.second}.tgz] failed, ${exception.message}")
            throw exception
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ChartManipulationService::class.java)
    }
}
