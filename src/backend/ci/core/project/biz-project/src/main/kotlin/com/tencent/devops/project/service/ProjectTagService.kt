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

package com.tencent.devops.project.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.cache.CacheBuilder
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.consul.ConsulConstants.PROJECT_TAG_CODECC_REDIS_KEY
import com.tencent.devops.common.client.consul.ConsulConstants.PROJECT_TAG_REDIS_KEY
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.service.utils.KubernetesUtils
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.model.project.tables.records.TProjectRecord
import com.tencent.devops.project.ProjectInfoResponse
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.dao.ProjectTagDao
import com.tencent.devops.project.pojo.ProjectExtSystemTagDTO
import com.tencent.devops.project.pojo.ProjectTagUpdateDTO
import com.tencent.devops.project.pojo.enums.SystemEnums
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Suppress("UNUSED")
@Service
class ProjectTagService @Autowired constructor(
    val dslContext: DSLContext,
    val projectTagDao: ProjectTagDao,
    val redisOperation: RedisOperation,
    val projectDao: ProjectDao,
    val objectMapper: ObjectMapper,
    val bkTag: BkTag
) {

    private val executePool = Executors.newFixedThreadPool(1)

    @Value("\${system.router:#{null}}")
    val routerTagList: String? = ""

    @Value("\${system.enabled:false}")
    val routerCheckEnabled: Boolean = true

    @Value("\${tag.auto:#{null}}")
    private val autoTag: String? = null

    @Value("\${tag.prod:#{null}}")
    private val prodTag: String? = null

    @Value("\${tag.gray:#{null}}")
    private val grayTag: String? = null

    @Value("\${system.inContainer:#{null}}")
    private val inContainerTags: String? = null

    private val projectRouterCache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(2, TimeUnit.MINUTES)
        .build<String/*projectId*/, String>/*routerTag*/()

    fun setGrayExt(projectCodeList: List<String>, operateFlag: Int, system: SystemEnums): Boolean {
        val routerTag = when (operateFlag) {
            grayLabel -> grayTag
            prodLabel -> prodTag
            else -> null
        }

        if (routerTag.isNullOrBlank()) {
            return false
        }

        when (system) {
            SystemEnums.CI -> {
                val projectTagUpdateDTO = ProjectTagUpdateDTO(
                    routerTag = routerTag,
                    bgId = null,
                    deptId = null,
                    centerId = null,
                    projectCodeList = projectCodeList,
                    channel = null
                )
                updateTagByProject(projectTagUpdateDTO)
            }

            SystemEnums.CODECC, SystemEnums.REPO -> {
                val projectTagUpdateDTO = ProjectExtSystemTagDTO(routerTag, projectCodeList, system = system.name)
                updateExtSystemRouterTag(projectTagUpdateDTO)
            }
        }
        return true
    }

    fun updateTagByProject(projectTagUpdateDTO: ProjectTagUpdateDTO): Result<Boolean> {
        logger.info("updateTagByProject: $projectTagUpdateDTO")
        checkRouteTag(projectTagUpdateDTO.routerTag)
        // checkProject(projectTagUpdateDTO.projectCodeList)
        projectTagDao.updateProjectTags(
            dslContext = dslContext,
            englishNames = projectTagUpdateDTO.projectCodeList!!,
            routerTag = projectTagUpdateDTO.routerTag
        )
        refreshRouterByProject(
            routerTag = projectTagUpdateDTO.routerTag,
            redisOperation = redisOperation,
            projectIds = projectTagUpdateDTO.projectCodeList!!
        )
        return Result(true)
    }

    fun updateTagByProject(projectCode: String, tag: String? = null): Boolean {
        val routerTag = if (tag.isNullOrEmpty()) {
            autoTag
        } else {
            tag
        }
        if (autoTag.isNullOrEmpty()) {
            return true
        }
        logger.info("updateTagByProject: $projectCode| $routerTag")
        val projectTagUpdate = ProjectTagUpdateDTO(
            routerTag = routerTag!!,
            projectCodeList = arrayListOf(projectCode),
            bgId = null,
            centerId = null,
            deptId = null,
            channel = null
        )
        updateTagByProject(projectTagUpdate)
        return true
    }

    fun updateTagByOrg(projectTagUpdateDTO: ProjectTagUpdateDTO): Result<Boolean> {
        logger.info("updateTagByOrg: $projectTagUpdateDTO")
        checkRouteTag(projectTagUpdateDTO.routerTag)
        checkOrg(projectTagUpdateDTO)
        projectTagDao.updateOrgTags(
            dslContext = dslContext,
            routerTag = projectTagUpdateDTO.routerTag,
            bgId = projectTagUpdateDTO.bgId,
            centerId = projectTagUpdateDTO.centerId,
            deptId = projectTagUpdateDTO.deptId
        )

        projectDao.listByOrganization(
            dslContext = dslContext,
            bgId = projectTagUpdateDTO.bgId,
            centerId = projectTagUpdateDTO.centerId,
            deptId = projectTagUpdateDTO.deptId,
            enabled = null
        )?.map { it.englishName }?.let {
            executePool.submit {
                refreshRouterByProject(
                    routerTag = projectTagUpdateDTO.routerTag,
                    redisOperation = redisOperation,
                    projectIds = it
                )
            }
        }
        return Result(true)
    }

    fun updateTagByChannel(projectTagUpdateDTO: ProjectTagUpdateDTO): Result<Boolean> {
        logger.info("updateTagByChannel: $projectTagUpdateDTO")
        checkRouteTag(projectTagUpdateDTO.routerTag)
        checkChannel(projectTagUpdateDTO.channel)
        projectTagDao.updateChannelTags(
            dslContext = dslContext,
            routerTag = projectTagUpdateDTO.routerTag,
            channel = projectTagUpdateDTO.channel!!
        )

        executePool.submit {
            refreshRouterByChannel(
                routerTag = projectTagUpdateDTO.routerTag,
                redisOperation = redisOperation,
                channel = projectTagUpdateDTO.channel!!,
                dslContext = dslContext
            )
        }
        return Result(true)
    }

    fun updateExtSystemRouterTag(extSystemTag: ProjectExtSystemTagDTO): Result<Boolean> {
        checkRouteTag(extSystemTag.routerTag)
        checkProject(extSystemTag.projectCodeList)
        val projectInfos = projectTagDao.getExtSystemRouterTag(dslContext, extSystemTag.projectCodeList)
            ?: return Result(false)
        projectInfos.forEach {
            val extSystemRouter = it.otherRouterTags
            val newRouteMap = mutableMapOf<String, String>()
            // 如果有对应系统的router则替换，否则直接加
            if (extSystemRouter?.toString().isNullOrEmpty()) {
                newRouteMap[extSystemTag.system] = extSystemTag.routerTag
            } else {
                val routerMap = JsonUtil.to<Map<String, String>>(extSystemRouter.toString())
                newRouteMap.putAll(routerMap)
                newRouteMap[extSystemTag.system] = extSystemTag.routerTag
            }
            logger.info("setExtSystemRoute ${it.englishName} ${JsonUtil.toJson(newRouteMap)}")
            projectTagDao.updateExtSystemProjectTags(
                dslContext = dslContext,
                englishName = it.englishName,
                otherRouterTag = JsonUtil.toJson(newRouteMap)
            )
            if (extSystemTag.system == SystemEnums.CODECC.name) { // 网关会用到来做codecc路由
                redisOperation.hset(PROJECT_TAG_CODECC_REDIS_KEY, it.englishName, extSystemTag.routerTag)
            }
        }
        return Result(true)
    }

    private fun checkProject(projectIds: List<String>?) {
        if (projectIds == null || projectIds.isEmpty()) {
            throw ParamBlankException("Invalid projectIds")
        }

        val projectInfos = projectDao.listByEnglishName(
            dslContext = dslContext,
            englishNameList = projectIds
        ).map { it.englishName }
        if (projectIds.size > projectInfos.size) {
            val notExistProjectList = mutableListOf<String>()
            projectIds.forEach {
                if (!projectInfos.contains(it)) {
                    notExistProjectList.add(it)
                }
            }
            throw ParamBlankException("project $notExistProjectList not exist")
        }
    }

    private fun checkChannel(channel: String?) {
        if (channel == null || channel.isEmpty()) {
            throw ParamBlankException("Invalid projectIds")
        }
    }

    private fun checkOrg(projectTagUpdateDTO: ProjectTagUpdateDTO) {
        if (projectTagUpdateDTO.bgId == null &&
            projectTagUpdateDTO.deptId == null &&
            projectTagUpdateDTO.centerId == null
        ) {
            throw ParamBlankException("Invalid project org")
        }
    }

    fun refreshRouterByProject(routerTag: String, projectIds: List<String>, redisOperation: RedisOperation) {
        val watcher = Watcher("ProjectTagRefresh $routerTag")
        logger.info("ProjectTagRefresh start $routerTag $projectIds")
        projectIds.forEach { projectId ->
            redisOperation.hset(PROJECT_TAG_REDIS_KEY, projectId, routerTag)
        }
        logger.info("ProjectTagRefresh success. $routerTag ${projectIds.size}")
        LogUtils.printCostTimeWE(watcher)
    }

    fun refreshRouterByChannel(
        routerTag: String,
        channel: String,
        redisOperation: RedisOperation,
        dslContext: DSLContext
    ) {
        try {
            var offset = 0
            val limit = 500
            do {
                val projectInfos = projectTagDao.listByChannel(dslContext, channel, limit, offset)
                projectInfos.forEach {
                    redisOperation.hset(PROJECT_TAG_REDIS_KEY, it.englishName, routerTag)
                }
                offset += limit
            } while (projectInfos.size == limit)
        } finally {
            logger.info("refreshRouterByChannel success")
        }
    }

    // 判断当前项目流量与当前集群匹配
    fun checkProjectTag(projectId: String): Boolean {
        // 因定时任务请求量太大,为减小redis压力,优先match内存缓存。 内存数据可能与实际数据存在差异。失败继续做redis校验
        if (projectRouterCache.getIfPresent(projectId) != null) {
            val cacheCheck = projectClusterCheck(projectRouterCache.getIfPresent(projectId))
            // 如果缓存内的为"",说明项目没有配置路由信息。 缓存校验生效
            if (cacheCheck || projectRouterCache.getIfPresent(projectId).isNullOrBlank()) {
                return cacheCheck
            }
        }

        // 内存缓存校验失败, 走redis。 redis数据与db基本保持一致,仅redis击穿后再查db。 redis校验结果具备判断权,校验失败直接返回
        if (redisOperation.hget(PROJECT_TAG_REDIS_KEY, projectId) != null) {
            val redisCheck = projectClusterCheck(redisOperation.hget(PROJECT_TAG_REDIS_KEY, projectId))
            projectRouterCache.put(projectId, redisOperation.hget(PROJECT_TAG_REDIS_KEY, projectId)!!)
            return redisCheck
        }
        // 直接从db获取
        val projectInfo = projectDao.getByEnglishName(dslContext, projectId) ?: return false
        logger.info("refresh router cache $projectId|${projectInfo.routerTag}| by checkProjectTag")
        // 刷新内存缓存。 网关根据redis的值做判断依据。 此处不额外更新redis. 减少redis自动操作。
        projectRouterCache.put(projectId, projectInfo.routerTag ?: "")

        return projectClusterCheck(projectInfo.routerTag)
    }

    @SuppressWarnings("ReturnCount")
    private fun projectClusterCheck(projectTag: String?): Boolean {
        val isContainerProject = inContainerTags?.split(",")?.contains(projectTag) == true
        if (isContainerProject && KubernetesUtils.notInContainer()) { // 容器化项目需要在容器化环境下执行
            return false
        }
        // 容器化项目需要将本地tag中的kubernetes-去掉来比较
        val localTag = bkTag.getLocalTag()
        val clusterTag = if (isContainerProject) localTag.replace("kubernetes-", "") else localTag
        // 默认集群是不会有routerTag的信息
        if (projectTag.isNullOrBlank()) {
            // 只有默认集群在routerTag为空的时候才返回true
            return clusterTag == prodTag
        }
        return clusterTag == projectTag
    }

    private fun checkRouteTag(routerTag: String) {
        if (!routerCheckEnabled) {
            logger.info("router check disabled")
            return
        }

        if (routerTag.isBlank()) {
            throw ParamBlankException("routerTag error:empty routerTag")
        }

        if (routerTagList.isNullOrBlank()) {
            throw ParamBlankException("routerTag error:empty routerTagList")
        }

        if (!routerTagList!!.contains(routerTag)) {
            throw ParamBlankException("routerTag error:system unknown routerTag")
        }
    }

    fun getProjectListByFlag(
        projectName: String?,
        englishName: String?,
        projectType: Int?,
        isSecrecy: Boolean?,
        creator: String?,
        approver: String?,
        approvalStatus: Int?,
        offset: Int,
        limit: Int,
        grayFlag: Boolean,
        codeCCGrayFlag: Boolean,
        repoGrayFlag: Boolean
    ): com.tencent.devops.project.pojo.Result<Map<String, Any?>?> {
        val dataObj = mutableMapOf<String, Any?>()

        val routerTag = if (grayFlag) grayTag else null

        val otherRouterTagMaps = mutableMapOf<String, String>()
        if (codeCCGrayFlag && grayTag != null) {
            otherRouterTagMaps[SystemEnums.CODECC.name] = grayTag
        }
        if (repoGrayFlag && grayTag != null) {
            otherRouterTagMaps[SystemEnums.REPO.name] = grayTag
        }

        val projectInfos = projectDao.getProjectList(
            dslContext = dslContext,
            projectName = projectName,
            englishName = englishName,
            projectType = projectType,
            isSecrecy = isSecrecy,
            creator = creator,
            approver = approver,
            approvalStatus = approvalStatus,
            offset = offset,
            limit = limit,
            routerTag = routerTag,
            otherRouterTagMaps = otherRouterTagMaps
        )
        val totalCount = projectDao.getProjectCount(
            dslContext = dslContext,
            projectName = projectName,
            englishName = englishName,
            projectType = projectType,
            isSecrecy = isSecrecy,
            creator = creator,
            approver = approver,
            approvalStatus = approvalStatus,
            routerTag = routerTag,
            otherRouterTagMaps = otherRouterTagMaps
        )
        val dataList = mutableListOf<ProjectInfoResponse>()

        for (i in projectInfos.indices) {
            val projectData = projectInfos[i]
            val projectInfo = getProjectInfoResponse(projectData)
            dataList.add(projectInfo)
        }
        dataObj["projectList"] = dataList
        dataObj["count"] = totalCount
        return com.tencent.devops.project.pojo.Result(dataObj)
    }

    private fun getProjectInfoResponse(projectData: TProjectRecord): ProjectInfoResponse {
        val otherRouterTagMap = projectData.otherRouterTags?.let {
            JsonUtil.to<Map<String, String>>(projectData.otherRouterTags.toString())
        } ?: emptyMap()
        return ProjectInfoResponse(
            projectId = projectData.projectId,
            projectName = projectData.projectName,
            projectEnglishName = projectData.englishName,
            creatorBgName = projectData.creatorBgName,
            creatorDeptName = projectData.creatorDeptName,
            creatorCenterName = projectData.creatorCenterName,
            bgId = projectData.bgId,
            bgName = projectData.bgName,
            deptId = projectData.deptId,
            deptName = projectData.deptName,
            centerId = projectData.centerId,
            centerName = projectData.centerName,
            projectType = projectData.projectType,
            approver = projectData.approver,
            approvalTime = projectData.approvalTime?.timestampmilli(),
            approvalStatus = projectData.approvalStatus,
            secrecyFlag = projectData.isSecrecy,
            creator = projectData.creator,
            createdAtTime = projectData.createdAt.timestampmilli(),
            ccAppId = projectData.ccAppId,
            useBk = projectData.useBk,
            offlinedFlag = projectData.isOfflined,
            kind = projectData.kind,
            enabled = projectData.enabled ?: true,
            grayFlag = projectData.routerTag == grayTag,
            codeCCGrayFlag = otherRouterTagMap[SystemEnums.CODECC.name] == grayTag,
            repoGrayFlag = otherRouterTagMap[SystemEnums.REPO.name] == grayTag,
            hybridCCAppId = projectData.hybridCcAppId,
            enableExternal = projectData.enableExternal,
            enableIdc = projectData.enableIdc,
            pipelineLimit = projectData.pipelineLimit
        )
    }

    companion object {
        private const val grayLabel = 1
        private const val prodLabel = 2
        private val logger = LoggerFactory.getLogger(ProjectTagService::class.java)
    }
}
