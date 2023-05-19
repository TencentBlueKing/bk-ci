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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.service

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryResource
import com.tencent.devops.artifactory.pojo.CustomFileSearchCondition
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildFormValue
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_SUB_PIPELINE_PARAM_FILTER_FAILED
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.SubPipeline
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.RepositoryInfo
import com.tencent.devops.repository.pojo.enums.Permission
import com.tencent.devops.store.api.container.ServiceContainerResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.lang.RuntimeException

@Suppress("ALL")
@Service
class ParamFacadeService @Autowired constructor(
    private val client: Client,
    private val codeService: CodeService,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineRuntimeService: PipelineRuntimeService
) {

    fun filterParams(
        userId: String?,
        projectId: String,
        pipelineId: String?,
        params: List<BuildFormProperty>
    ): List<BuildFormProperty> {
        val filterParams = mutableListOf<BuildFormProperty>()
        params.forEach {
            if (it.type == BuildFormPropertyType.SVN_TAG && (!it.repoHashId.isNullOrBlank())) {
                val svnTagBuildFormProperty = addSvnTagDirectories(projectId, it)
                filterParams.add(svnTagBuildFormProperty)
            } else if (it.type == BuildFormPropertyType.GIT_REF && (!it.repoHashId.isNullOrBlank())) {
                val gitRefBuildFormProperty = addGitRefs(projectId, it)
                filterParams.add(gitRefBuildFormProperty)
            } else if (it.type == BuildFormPropertyType.CODE_LIB && it.scmType != null) {
                filterParams.add(addCodelibProperties(userId, projectId, it))
            } else if (it.type == BuildFormPropertyType.CONTAINER_TYPE && it.containerType != null) {
                filterParams.add(addContainerTypeProperties(userId, projectId, it))
            } else if (it.type == BuildFormPropertyType.ARTIFACTORY) {
                filterParams.add(addArtifactoryProperties(userId, projectId, it))
            } else if (it.type == BuildFormPropertyType.SUB_PIPELINE) {
                filterParams.add(addSubPipelineProperties(userId, projectId, pipelineId, it))
            } else {
                filterParams.add(it)
            }
        }

        return filterParams
    }

    fun filterOptions(
        userId: String?,
        projectId: String,
        pipelineId: String?,
        search: String? = null,
        property: BuildFormProperty
    ): List<BuildFormValue> {
        val filterParams =
            if (property.type == BuildFormPropertyType.SVN_TAG && (!property.repoHashId.isNullOrBlank())) {
                addSvnTagDirectories(projectId, property)
            } else if (property.type == BuildFormPropertyType.GIT_REF && (!property.repoHashId.isNullOrBlank())) {
                addGitRefs(projectId, property, search)
            } else if (property.type == BuildFormPropertyType.CODE_LIB && property.scmType != null) {
                addCodelibProperties(userId, projectId, property, search)
            } else if (property.type == BuildFormPropertyType.CONTAINER_TYPE && property.containerType != null) {
                addContainerTypeProperties(userId, projectId, property)
            } else if (property.type == BuildFormPropertyType.ARTIFACTORY) {
                addArtifactoryProperties(userId, projectId, property)
            } else if (property.type == BuildFormPropertyType.SUB_PIPELINE) {
                addSubPipelineProperties(userId, projectId, pipelineId, property)
            } else {
                property
            }

        return filterParams.options ?: emptyList()
    }

    private fun addGitRefs(
        projectId: String,
        formProperty: BuildFormProperty,
        search: String? = null
    ): BuildFormProperty {
        val refs = try {
            codeService.getGitRefs(projectId, formProperty.repoHashId, search)
        } catch (e: Exception) {
            logger.warn("projectId:$projectId,repoHashId:${formProperty.repoHashId} add git refs error", e)
            listOf<String>()
        }
        val options = refs.map {
            BuildFormValue(it, it)
        }
        val searchUrl = "/process/api/user/scm/$projectId/${formProperty.repoHashId}/refs?search={words}"
        val replaceKey = "{words}"
        return copyFormProperty(
            property = formProperty,
            options = options,
            searchUrl = searchUrl,
            replaceKey = replaceKey
        )
    }

    /**
     * SVN_TAG类型参数添加SVN目录作为复选参数
     */
    private fun addSvnTagDirectories(projectId: String, svnTagBuildFormProperty: BuildFormProperty): BuildFormProperty {
        val directories = try {
            codeService.getSvnDirectories(
                projectId = projectId,
                repoHashId = svnTagBuildFormProperty.repoHashId,
                relativePath = svnTagBuildFormProperty.relativePath
            )
        } catch (e: Exception) {
            logger.warn("projectId:$projectId,repoHashId:${svnTagBuildFormProperty.repoHashId} add svn tag error", e)
            listOf<String>()
        }
        val options = directories.map {
            BuildFormValue(it, it)
        }
        return copyFormProperty(svnTagBuildFormProperty, options)
    }

    /**
     * 自定义仓代码库过滤参数
     */
    private fun addCodelibProperties(
        userId: String?,
        projectId: String,
        codelibFormProperty: BuildFormProperty,
        aliasName: String? = null
    ): BuildFormProperty {

        val aliasNames = if ((!userId.isNullOrBlank())) {
            // 检查代码库的权限， 只返回用户有权限代码库
            val hasPermissionCodelibs =
                getPermissionCodelibList(userId, projectId, codelibFormProperty.scmType!!, aliasName)
            logger.info("[$userId|$projectId] Get the permission code lib list ($hasPermissionCodelibs)")
            hasPermissionCodelibs.map { BuildFormValue(it.aliasName, it.aliasName) }
        } else {
            // 该接口没有搜索字段
            val codeAliasName = codeService.listRepository(projectId, codelibFormProperty.scmType!!)
            codeAliasName.map { BuildFormValue(it.aliasName, it.aliasName) }
        }
        val searchUrl = "/process/api/user/buildParam/repository/$projectId/aliasName?" +
                "repositoryType=${codelibFormProperty.scmType!!}&permission=${Permission.LIST.name}" +
                "&aliasName={words}&page=1&pageSize=100"
        val replaceKey = "{words}"
        return copyFormProperty(
            property = codelibFormProperty,
            options = aliasNames,
            searchUrl = searchUrl,
            replaceKey = replaceKey
        )
    }

    private fun addContainerTypeProperties(
        userId: String?,
        projectId: String,
        property: BuildFormProperty
    ): BuildFormProperty {
        try {
            if (userId.isNullOrBlank()) {
                logger.warn("The user id if empty for the container type properties")
                return property
            }
            val containerType = property.containerType!!
            val containers = client.get(ServiceContainerResource::class)
                .getContainers(userId, projectId, containerType.buildType, containerType.os)
            if (containers.data == null || containers.data!!.resources == null) {
                logger.warn("[$userId|$projectId|$property] Fail to get the container properties")
                return property
            }
            val containerValue = containers.data!!.resources!!.map {
                BuildFormValue(it, it)
            }.toList()
            return copyFormProperty(property, containerValue)
        } catch (ignored: Throwable) {
            logger.warn("[$userId|$projectId|$property] Fail to get the pcg container images", ignored)
        }
        return property
    }

    /**
     * 自定义仓库文件过滤参数
     */
    private fun addArtifactoryProperties(
        userId: String?,
        projectId: String,
        property: BuildFormProperty
    ): BuildFormProperty {
        try {
            val glob = property.glob
            if (glob.isNullOrBlank() && (property.properties == null || property.properties!!.isEmpty())) {
                logger.warn("glob and properties are both empty")
                return property
            }

            val listResult = client.get(ServiceArtifactoryResource::class).searchCustomFiles(
                userId ?: "",
                projectId,
                CustomFileSearchCondition(
                    property.glob,
                    property.properties ?: mapOf()
                )
            )
            if (listResult.data == null) {
                logger.warn("list file result is empty")
                return property
            }

            return copyFormProperty(
                property,
                listResult.data!!.map { BuildFormValue(it, File(it).name) }
            )
        } catch (t: Throwable) {
            logger.warn("[$userId|$projectId|$property] Fail to list artifactory files", t)
        }
        return property
    }

    /**
     * 自定义子流水线参数过滤
     */
    private fun addSubPipelineProperties(
        userId: String?,
        projectId: String,
        pipelineId: String?,
        subPipelineFormProperty: BuildFormProperty
    ): BuildFormProperty {
        try {
            val hasPermissionPipelines = getHasPermissionPipelineList(userId, projectId)
            val aliasName = hasPermissionPipelines
                .filter { pipelineId == null || !it.pipelineId.contains(pipelineId) }
                .map { BuildFormValue(it.pipelineName, it.pipelineName) }
            return copyFormProperty(subPipelineFormProperty, aliasName)
        } catch (t: Throwable) {
            logger.warn("[$userId|$projectId] Fail to filter the properties of subpipelines", t)
            throw OperationException(
                MessageUtil.getMessageByLocale(ERROR_SUB_PIPELINE_PARAM_FILTER_FAILED, I18nUtil.getLanguage(userId))
            )
        }
    }

    private fun copyFormProperty(
        property: BuildFormProperty,
        options: List<BuildFormValue>,
        searchUrl: String? = null,
        replaceKey: String? = null
    ): BuildFormProperty {
        return BuildFormProperty(
            id = property.id,
            required = property.required,
            type = property.type,
            defaultValue = property.defaultValue,
            options = options,
            desc = property.desc,
            repoHashId = property.repoHashId,
            relativePath = property.relativePath,
            scmType = property.scmType,
            containerType = property.containerType,
            glob = property.glob,
            properties = property.properties,
            searchUrl = searchUrl,
            replaceKey = replaceKey
        )
    }

    private fun getPermissionCodelibList(
        userId: String,
        projectId: String,
        scmType: ScmType?,
        aliasName: String? = null
    ): List<RepositoryInfo> {
        val watcher = Watcher("getPermissionCodelibList_${userId}_${projectId}_${scmType?.name}")
        return try {
            client.get(ServiceRepositoryResource::class).hasPermissionList(
                userId = userId,
                projectId = projectId,
                permission = Permission.LIST,
                repositoryType = scmType?.name,
                page = 1,
                pageSize = 100,
                aliasName = aliasName
            ).data?.records ?: emptyList()
        } catch (e: RuntimeException) {
            logger.warn("[$userId|$projectId] Fail to get the permission code lib list", e)
            emptyList()
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher, errorThreshold = 4000)
        }
    }

    private fun getHasPermissionPipelineList(userId: String?, projectId: String): List<SubPipeline> {
        val watcher = Watcher("getHasPermissionPipelineList_$userId")
        try {
            // 从权限中拉取有权限的流水线，若无userId则返回空值
            watcher.start("perm_r_perm")
            val hasPermissionList =
                if (userId.isNullOrBlank()) {
                    null
                } else {
                    pipelinePermissionService.getResourceByPermission(
                        userId = userId,
                        projectId = projectId,
                        permission = AuthPermission.EXECUTE
                    )
                }
            watcher.stop()

            // 获取项目下所有流水线，并过滤出有权限部分，有权限列表为空时返回项目所有流水线
            watcher.start("s_r_summary")
            val buildPipelineRecords =
                pipelineRuntimeService.getBuildPipelineRecords(projectId, ChannelCode.BS, hasPermissionList)
            watcher.stop()

            return buildPipelineRecords.map {
                val pipelineId = it.pipelineId
                val pipelineName = it.pipelineName
                SubPipeline(pipelineName, pipelineId)
            }
        } catch (t: Throwable) {
            logger.warn("[$userId|$projectId] Fail to get the permission pipeline list", t)
            return emptyList()
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher, errorThreshold = 3000)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ParamFacadeService::class.java)
    }
}
