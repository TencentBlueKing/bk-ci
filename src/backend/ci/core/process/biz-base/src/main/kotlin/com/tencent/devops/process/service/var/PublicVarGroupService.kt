/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.service.`var`

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import com.tencent.devops.common.api.constant.CommonMessageCode.ERROR_INVALID_PARAM_
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.PublicVarGroupReferenceTypeEnum
import com.tencent.devops.common.pipeline.pojo.PublicVarGroupRef
import com.tencent.devops.common.pipeline.pojo.PublicVarGroupVariable
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessConstants
import com.tencent.devops.process.constant.ProcessConstants.DYNAMIC_VERSION
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PIPELINE_COMMON_VAR_GROUP_CONFLICT
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_YAML_DESERIALIZE_ERROR
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_YAML_FORMAT_ERROR
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_YAML_MISSING_FIELD
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_YAML_NAME_FORMAT
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_YAML_PARSE_FAILED
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_YAML_UNKNOWN_FIELD
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_YAML_VARIABLE_NAME_FORMAT
import com.tencent.devops.process.dao.`var`.PublicVarDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupReferInfoDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupReleaseRecordDao
import com.tencent.devops.process.dao.`var`.PublicVarGroupVersionSummaryDao
import com.tencent.devops.process.dao.`var`.PublicVarReferInfoDao
import com.tencent.devops.process.dao.`var`.PublicVarVersionSummaryDao
import com.tencent.devops.process.permission.`var`.PublicVarGroupPermissionService
import com.tencent.devops.process.pojo.`var`.PublicVarGroupPermissions
import com.tencent.devops.process.pojo.`var`.`do`.PipelineRefPublicVarGroupDO
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarGroupDO
import com.tencent.devops.process.pojo.`var`.`do`.PublicVarReleaseDO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarDTO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupDTO
import com.tencent.devops.process.pojo.`var`.dto.PublicVarGroupInfoQueryReqDTO
import com.tencent.devops.process.pojo.`var`.enums.PublicVarTypeEnum
import com.tencent.devops.process.pojo.`var`.po.PublicVarGroupPO
import com.tencent.devops.process.pojo.`var`.po.PublicVarPO
import com.tencent.devops.process.pojo.`var`.vo.PublicVarGroupVO
import com.tencent.devops.process.pojo.`var`.vo.PublicVarGroupYamlStringVO
import com.tencent.devops.process.pojo.`var`.vo.PublicVarVO
import com.tencent.devops.process.yaml.transfer.TransferMapper
import com.tencent.devops.process.yaml.transfer.VariableTransfer
import com.tencent.devops.process.yaml.transfer.pojo.PublicVarGroupYamlParser
import com.tencent.devops.process.yaml.v2.utils.YamlCommonUtils
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import jakarta.ws.rs.core.Response
import java.time.LocalDateTime
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PublicVarGroupService @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val publicVarGroupDao: PublicVarGroupDao,
    private val client: Client,
    private val publicVarService: PublicVarService,
    private val variableTransfer: VariableTransfer,
    private val publicVarDao: PublicVarDao,
    private val pipelinePublicVarGroupReleaseRecordDao: PublicVarGroupReleaseRecordDao,
    private val publicVarGroupReferInfoDao: PublicVarGroupReferInfoDao,
    private val publicVarReferInfoDao: PublicVarReferInfoDao,
    private val publicVarGroupReleaseRecordService: PublicVarGroupReleaseRecordService,
    private val publicVarGroupPermissionService: PublicVarGroupPermissionService,
    private val publicVarVersionSummaryDao: PublicVarVersionSummaryDao,
    private val publicVarGroupVersionSummaryDao: PublicVarGroupVersionSummaryDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PublicVarGroupService::class.java)
        // 校验规则与对外文案同源，避免二者各改一处后对不上（见 ProcessConstants 注释）
        private val GROUP_NAME_REGEX = Regex(ProcessConstants.PUBLIC_VAR_GROUP_NAME_PATTERN)
        private val VAR_NAME_REGEX = Regex(ProcessConstants.PUBLIC_VAR_NAME_PATTERN)
        // 权限中心删除重试配置
        private const val IAM_DELETE_MAX_RETRY = 3
        private const val IAM_DELETE_RETRY_INTERVAL_MS = 500L
    }

    fun saveGroup(
        publicVarGroupDTO: PublicVarGroupDTO,
        allowUpgrade: Boolean = true,
        requireExisting: Boolean = false
    ): String {
        val projectId = publicVarGroupDTO.projectId
        val userId = publicVarGroupDTO.userId
        val groupName = publicVarGroupDTO.publicVarGroup.groupName
        if (groupName.isBlank() || !GROUP_NAME_REGEX.matches(groupName)) {
            throw ErrorCodeException(errorCode = ERROR_PUBLIC_VAR_GROUP_YAML_NAME_FORMAT)
        }
        // ID 远程分配放在锁外，缩短锁持有时间
        val id = client.get(ServiceAllocIdResource::class)
            .generateSegmentId("T_RESOURCE_PUBLIC_VAR_GROUP").data
            ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_ADD_FAILED,
                params = arrayOf("ID allocation service unavailable")
            )
        val varSegmentIds = publicVarService.batchGenerateVarSegmentIds(
            publicVarGroupDTO.publicVarGroup.publicVars.size
        )
        val redisLock = RedisLock(
            redisOperation = redisOperation,
            lockKey = "${ProcessConstants.PUBLIC_VAR_GROUP_ADD_LOCK_KEY}_${projectId}_$groupName",
            expiredTimeInSeconds = ProcessConstants.PUBLIC_VAR_GROUP_LOCK_EXPIRED_TIME_IN_SECONDS
        )
        redisLock.lock()
        try {
            // 同名校验（忽略大小写）
            val conflictNames = publicVarGroupDao.listNamesConflictByNameIgnoreCase(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName
            )
            val hasConflict = if (allowUpgrade) {
                conflictNames.any { it != groupName }
            } else {
                conflictNames.isNotEmpty()
            }
            if (hasConflict) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_NAME_DUPLICATE_CASE_INSENSITIVE,
                    params = arrayOf(groupName)
                )
            }
            publicVarService.checkGroupPublicVar(publicVarGroupDTO.publicVarGroup.publicVars)
            // 判断是新建还是升级：升级需校验编辑权限
            val existingVersion = publicVarGroupDao.getLatestVersionByGroupName(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName
            ) ?: 0
            // 更新要求变量组已存在
            if (requireExisting && existingVersion == 0) {
                throw ErrorCodeException(
                    errorCode = ERROR_INVALID_PARAM_,
                    params = arrayOf(groupName)
                )
            }
            if (existingVersion > 0) {
                // 权限校验需用锁内版本查询结果，保留在锁内
                val editPermissionMap = publicVarGroupPermissionService.filterPublicVarGroups(
                    userId = userId,
                    projectId = projectId,
                    authPermissions = setOf(AuthPermission.EDIT)
                )
                val canEdit = editPermissionMap[AuthPermission.EDIT]?.contains(groupName) ?: false
                if (!canEdit) {
                    throw ErrorCodeException(
                        errorCode = ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_NO_PERMISSION,
                        params = arrayOf(groupName, AuthPermission.EDIT.getI18n(I18nUtil.getLanguage()))
                    )
                }
            }
            val isCreate = createOrUpgradeGroupRecord(
                id = id,
                projectId = projectId,
                userId = userId,
                groupName = groupName,
                publicVarGroupDTO = publicVarGroupDTO,
                existingVersion = existingVersion,
                varSegmentIds = varSegmentIds
            )
            // 数据库事务成功后，如果是新建变量组（首次创建），注册到权限中心
            if (isCreate) {
                registerToIamOrRollback(
                    userId = userId,
                    projectId = projectId,
                    groupName = groupName
                )
            }
        } catch (e: ErrorCodeException) {
            throw e
        } catch (t: Throwable) {
            logger.warn("Failed to add variable group $groupName", t)
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_ADD_FAILED,
                params = arrayOf(groupName)
            )
        } finally {
            redisLock.unlock()
        }
        return publicVarGroupDTO.publicVarGroup.groupName
    }

    /**
     * 更新已存在的公共变量组：以新版本覆盖当前最新版本。
     * 变量组必须已存在（不存在则报错，避免误走创建路径）；编辑权限由接口层校验。
     */
    fun updateGroup(publicVarGroupDTO: PublicVarGroupDTO): String {
        return saveGroup(
            publicVarGroupDTO = publicVarGroupDTO,
            allowUpgrade = true,
            requireExisting = true
        )
    }

    private fun createOrUpgradeGroupRecord(
        id: Long,
        projectId: String,
        userId: String,
        groupName: String,
        publicVarGroupDTO: PublicVarGroupDTO,
        existingVersion: Int,
        varSegmentIds: List<Long>
    ): Boolean {
        val isCreate = (existingVersion == 0)
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            val newVersion = existingVersion + 1
            val publicVarGroupPO = PublicVarGroupPO(
                id = id,
                projectId = projectId,
                groupName = groupName,
                version = newVersion,
                versionName = "v$newVersion",
                latestFlag = true,
                varCount = publicVarGroupDTO.publicVarGroup.publicVars.size,
                desc = publicVarGroupDTO.publicVarGroup.desc,
                creator = userId,
                modifier = userId,
                createTime = LocalDateTime.now(),
                updateTime = LocalDateTime.now()
            )
            if (existingVersion != 0) {
                publicVarGroupDao.updateLatestFlag(
                    dslContext = context,
                    projectId = projectId,
                    groupName = groupName,
                    latestFlag = false
                )
            }
            publicVarGroupDao.save(context, publicVarGroupPO)
            publicVarService.addGroupPublicVar(
                context = context,
                publicVarDTO = PublicVarDTO(
                    projectId = projectId,
                    userId = userId,
                    groupName = groupName,
                    version = publicVarGroupPO.version,
                    versionDesc = publicVarGroupDTO.publicVarGroup.versionDesc ?: "",
                    publicVars = publicVarGroupDTO.publicVarGroup.publicVars
                ),
                preGeneratedIds = varSegmentIds
            )
        }
        return isCreate
    }

    /**
     * 注册变量组到权限中心，失败时补偿回滚 DB 记录。
     */
    private fun registerToIamOrRollback(
        userId: String,
        projectId: String,
        groupName: String
    ) {
        try {
            publicVarGroupPermissionService.createResource(
                userId = userId,
                projectId = projectId,
                groupCode = groupName,
                name = groupName
            )
        } catch (e: Exception) {
            logger.warn(
                "Failed to register auth resource for [$projectId|$groupName], rolling back DB records", e
            )
            try {
                dslContext.transaction { configuration ->
                    val ctx = DSL.using(configuration)
                    publicVarGroupDao.deleteByGroupName(ctx, projectId, groupName)
                    publicVarDao.deleteByGroupName(ctx, projectId, groupName)
                    publicVarVersionSummaryDao.deleteByGroupName(ctx, projectId, groupName)
                }
            } catch (compensationEx: Throwable) {
                logger.warn(
                    "Compensation rollback failed for [$projectId|$groupName], manual cleanup required.",
                    compensationEx
                )
            }
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_ADD_FAILED,
                params = arrayOf(groupName)
            )
        }
    }

    fun getPipelineGroupsVar(projectId: String, groupName: String, version: Int? = null): PublicVarGroupVO {
        // 版本号空值默认取最新版本
        val groupRecord = publicVarGroupDao.getRecordByGroupName(
            dslContext = dslContext,
            projectId = projectId,
            groupName = groupName,
            version = version
        ) ?: throw ErrorCodeException(
            errorCode = ERROR_INVALID_PARAM_,
            params = arrayOf(groupName)
        )

        val varPOs = publicVarService.getGroupPublicVar(
            projectId = projectId,
            groupName = groupName,
            version = groupRecord.version
        )
        val publicVars = publicVarService.convertVarPOsToPublicVarVOs(varPOs)

        return PublicVarGroupVO(
            groupName = groupRecord.groupName,
            desc = groupRecord.desc,
            publicVars = publicVars
        )
    }

    fun getGroups(
        userId: String,
        queryReq: PublicVarGroupInfoQueryReqDTO
    ): Page<PublicVarGroupDO> {
        val projectId = queryReq.projectId
        val page = queryReq.page
        val pageSize = queryReq.pageSize

        val varFilterGroupNames = publicVarService.listGroupNamesByVarFilter(
            projectId = projectId,
            filterByVarName = queryReq.filterByVarName,
            filterByVarAlias = queryReq.filterByVarAlias
        )

        // 如果用户指定了变量名或别名筛选条件，但没有匹配结果，直接返回空页面
        val hasVarFilter = !queryReq.filterByVarName.isNullOrBlank() || !queryReq.filterByVarAlias.isNullOrBlank()
        if (hasVarFilter && varFilterGroupNames.isEmpty()) {
            return Page(
                count = 0,
                page = page,
                pageSize = pageSize,
                totalPages = 0,
                records = emptyList()
            )
        }

        // count 和分页查询不按资源粒度过滤，只按变量筛选条件过滤
        val effectiveGroupNames = varFilterGroupNames

        val totalCount = publicVarGroupDao.countGroupsByProjectId(
            dslContext = dslContext,
            projectId = projectId,
            filterByGroupName = queryReq.filterByGroupName,
            filterByGroupDesc = queryReq.filterByGroupDesc,
            filterByUpdater = queryReq.filterByUpdater,
            groupNames = effectiveGroupNames.takeIf { it.isNotEmpty() }
        )

        val groupPOs = publicVarGroupDao.listGroupsByProjectIdPage(
            dslContext = dslContext,
            projectId = projectId,
            page = page,
            pageSize = pageSize,
            filterByGroupName = queryReq.filterByGroupName,
            filterByGroupDesc = queryReq.filterByGroupDesc,
            filterByUpdater = queryReq.filterByUpdater,
            groupNames = effectiveGroupNames.takeIf { it.isNotEmpty() }
        )

        val groupNameList = groupPOs.map { it.groupName }

        // 批量查询当前有效引用数量（读预聚合 summary，点查/小范围，避免明细表实时聚合的开销）
        val referCountMap = if (groupNameList.isNotEmpty()) {
            publicVarGroupVersionSummaryDao.batchGetTotalReferCount(
                dslContext = dslContext,
                projectId = projectId,
                groupNames = groupNameList
            )
        } else {
            emptyMap()
        }

        // 批量查询当前有效动态版本的引用数量
        val dynamicVersionReferCountMap = if (groupNameList.isNotEmpty()) {
            publicVarGroupVersionSummaryDao.batchGetDynamicVersionReferCount(
                dslContext = dslContext,
                projectId = projectId,
                groupNames = groupNameList
            )
        } else {
            emptyMap()
        }

        // 批量查询当前有效固定版本的引用数量
        val fixedVersionReferCountMap = if (groupNameList.isNotEmpty()) {
            publicVarGroupVersionSummaryDao.batchGetFixedVersionReferCount(
                dslContext = dslContext,
                projectId = projectId,
                groupNames = groupNameList
            )
        } else {
            emptyMap()
        }

        // 批量查询当前页结果集的 EDIT/VIEW/DELETE/USE 权限
        val permissionsMap = if (groupNameList.isNotEmpty()) {
            publicVarGroupPermissionService.filterPublicVarGroups(
                userId = userId,
                projectId = projectId,
                authPermissions = setOf(
                    AuthPermission.EDIT,
                    AuthPermission.VIEW,
                    AuthPermission.DELETE,
                    AuthPermission.USE,
                    AuthPermission.LIST
                )
            )
        } else {
            emptyMap()
        }

        val records = groupPOs.map { po ->
            val actualReferCount = referCountMap[po.groupName] ?: 0
            val dynamicVersionReferCount = dynamicVersionReferCountMap[po.groupName] ?: 0
            val fixedVersionReferCount = fixedVersionReferCountMap[po.groupName] ?: 0
            PublicVarGroupDO(
                groupName = po.groupName,
                referCount = actualReferCount,
                dynamicVersionReferCount = dynamicVersionReferCount,
                fixedVersionReferCount = fixedVersionReferCount,
                varCount = po.varCount,
                desc = po.desc,
                modifier = po.modifier,
                updateTime = po.updateTime,
                permission = PublicVarGroupPermissions(
                    canEdit = permissionsMap[AuthPermission.EDIT]?.contains(po.groupName) ?: false,
                    canView = permissionsMap[AuthPermission.VIEW]?.contains(po.groupName) ?: false,
                    canDelete = permissionsMap[AuthPermission.DELETE]?.contains(po.groupName) ?: false,
                    canUse = permissionsMap[AuthPermission.USE]?.contains(po.groupName) ?: false
                )
            )
        }

        return Page(
            count = totalCount,
            page = page,
            pageSize = pageSize,
            totalPages = PageUtil.calTotalPage(pageSize, totalCount),
            records = records
        )
    }

    fun listGroupNames(projectId: String): List<String> {
        return publicVarGroupDao.listGroupsNameByProjectId(dslContext, projectId)
    }

    fun importGroup(
        userId: String,
        projectId: String,
        yaml: PublicVarGroupYamlStringVO
    ): String {
        val publicVarGroupVO = parseYamlToPublicVarGroupVO(yaml)

        return saveGroup(
            publicVarGroupDTO = PublicVarGroupDTO(
                projectId = projectId,
                userId = userId,
                publicVarGroup = publicVarGroupVO
            ),
            allowUpgrade = false
        )
    }

    fun getGroupYaml(
        groupName: String,
        version: Int?,
        projectId: String
    ): String {
        val groupInfo = publicVarGroupDao.getRecordByGroupName(
            dslContext = dslContext,
            projectId = projectId,
            groupName = groupName,
            version = version
        )

        if (groupInfo == null) {
            throw ErrorCodeException(
                errorCode = ERROR_INVALID_PARAM_,
                params = arrayOf(groupName)
            )
        }

        val varPOs = publicVarService.getGroupPublicVar(
            projectId = projectId,
            groupName = groupName,
            version = groupInfo.version
        )
        val params = publicVarService.convertVarPOsToBuildFormProperties(varPOs)
        val variables = variableTransfer.makeVariableFromBuildParams(params, false)
        val parserVO = PublicVarGroupYamlParser(
            version = "v3.0",
            name = groupInfo.groupName,
            desc = groupInfo.desc,
            variables = variables ?: emptyMap()
        )
        return TransferMapper.getObjectMapper().writeValueAsString(parserVO)
    }

    fun exportGroup(
        groupName: String,
        version: Int?,
        projectId: String
    ): Response {
        val yaml = getGroupYaml(groupName, version, projectId)
        return YamlCommonUtils.exportToFile(yaml, groupName)
    }

    fun deleteGroup(userId: String, projectId: String, groupName: String): Boolean {
        val redisLock = RedisLock(
            redisOperation = redisOperation,
            lockKey = "${ProcessConstants.PUBLIC_VAR_GROUP_DELETE_LOCK_KEY}_${projectId}_$groupName",
            expiredTimeInSeconds = ProcessConstants.PUBLIC_VAR_GROUP_LOCK_EXPIRED_TIME_IN_SECONDS
        )
        // 与引用变更链路的 summary 重算串行化：防止"读到 referCount=0 → 并发保存刚加了引用 → 误删被引用的组"。
        // 锁 key 必须与 PublicVarGroupReferManageService 的按组 summary 锁一致。
        val groupSummaryLock = RedisLock(
            redisOperation = redisOperation,
            lockKey = "${ProcessConstants.PUBLIC_VAR_GROUP_REFER_LOCK_KEY_PREFIX}:summary:$projectId:$groupName",
            expiredTimeInSeconds = ProcessConstants.PUBLIC_VAR_GROUP_LOCK_EXPIRED_TIME_IN_SECONDS
        )
        redisLock.lock()
        groupSummaryLock.lock()
        try {
            publicVarGroupDao.getRecordByGroupName(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName
            ) ?: throw ErrorCodeException(
                errorCode = ERROR_INVALID_PARAM_,
                params = arrayOf(groupName)
            )

            // 检查变量组是否被引用（读预聚合 summary，与列表口径一致；summary 由引用变更事务内重算保证准确）
            val referCount = publicVarGroupVersionSummaryDao.getTotalReferCount(
                dslContext = dslContext,
                projectId = projectId,
                groupName = groupName
            )

            if (referCount > 0) {
                logger.warn(
                    "Delete group blocked by active refer summary: " +
                        "projectId=$projectId, groupName=$groupName, referCount=$referCount"
                )
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_REFERENCED,
                    params = arrayOf(groupName)
                )
            }

            // summary 仅统计生效版本（LATEST_FLAG=true），草稿版本的引用不计入。
            // 若仅依赖 summary，草稿仍在引用该组却被删除，会导致草稿后续无法发布。
            // 故再补充明细存在性判断：只要仍有任意版本（生效/历史/草稿）引用该组即阻止删除。
            if (publicVarGroupReferInfoDao.existsAnyReferByGroupName(
                    dslContext = dslContext,
                    projectId = projectId,
                    groupName = groupName
                )
            ) {
                logger.warn(
                    "Delete group blocked by draft/historical refer detail: " +
                        "projectId=$projectId, groupName=$groupName"
                )
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_REFERENCED,
                    params = arrayOf(groupName)
                )
            }

            // 先执行 DB 事务删除，再删权限中心。
            // 权限中心是外部系统无法参与 DB 事务，调整顺序避免"DB 失败但权限已删"的僵尸资源。
            // 权限中心删除失败不阻断主流程——DB 已删，权限残留不影响功能（最坏情况是重新创建同名组时报已存在）。
            dslContext.transaction { configuration ->
                val context = DSL.using(configuration)
                publicVarGroupDao.deleteByGroupName(dslContext = context, projectId = projectId, groupName = groupName)
                pipelinePublicVarGroupReleaseRecordDao.deleteByGroupName(
                    dslContext = context,
                    projectId = projectId,
                    groupName = groupName
                )
                publicVarDao.deleteByGroupName(dslContext = context, projectId = projectId, groupName = groupName)
                // 删除变量版本概要信息
                publicVarVersionSummaryDao.deleteByGroupName(
                    dslContext = context,
                    projectId = projectId,
                    groupName = groupName
                )
                // 删除变量组版本引用数概要（summary）
                publicVarGroupVersionSummaryDao.deleteByGroupName(
                    dslContext = context,
                    projectId = projectId,
                    groupName = groupName
                )
                // 防御性清理引用记录（正常情况 referCount=0 时无数据，防止历史脏数据残留）
                publicVarGroupReferInfoDao.deleteByGroupName(
                    dslContext = context, projectId = projectId, groupName = groupName
                )
                publicVarReferInfoDao.deleteByGroupName(
                    dslContext = context, projectId = projectId, groupName = groupName
                )
            }

            // DB 删除成功后，删除权限中心资源。
            // 同步重试覆盖瞬时网络抖动；重试耗尽仍失败则日志告警，不阻断主流程。
            deleteIamResourceWithRetry(projectId, groupName)

            logger.info("Deleted public var group: projectId=$projectId, groupName=$groupName, userId=$userId")
            return true
        } catch (e: ErrorCodeException) {
            // 引用拦截已在上方打了具体原因；此处仅对非引用类业务异常补日志，避免重复刷屏
            if (e.errorCode != ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_REFERENCED) {
                logger.warn("Failed to delete variable group $groupName", e)
            }
            throw e
        } catch (t: Throwable) {
            logger.warn("Failed to delete variable group $groupName", t)
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_DELETE_FAILED,
                params = arrayOf(groupName)
            )
        } finally {
            runCatching { groupSummaryLock.unlock() }
            redisLock.unlock()
        }
    }

    /**
     * 删除权限中心资源（带同步重试）
     * 瞬时网络抖动自动恢复；重试耗尽仍失败则日志告警，不抛异常不阻断主流程。
     */
    private fun deleteIamResourceWithRetry(projectId: String, groupName: String) {
        repeat(IAM_DELETE_MAX_RETRY) { attempt ->
            try {
                publicVarGroupPermissionService.deleteResource(
                    projectId = projectId,
                    groupName = groupName
                )
                return
            } catch (e: Throwable) {
                if (attempt < IAM_DELETE_MAX_RETRY - 1) {
                    logger.warn(
                        "IAM delete failed (attempt ${attempt + 1}/$IAM_DELETE_MAX_RETRY), retrying. " +
                            "projectId=$projectId, groupName=$groupName",
                        e
                    )
                    Thread.sleep(IAM_DELETE_RETRY_INTERVAL_MS)
                } else {
                    logger.error(
                        "IAM delete failed after $IAM_DELETE_MAX_RETRY attempts, manual cleanup needed. " +
                            "projectId=$projectId, groupName=$groupName",
                        e
                    )
                }
            }
        }
    }

    fun getChangePreview(
        userId: String,
        projectId: String,
        publicVarGroup: PublicVarGroupVO
    ): List<PublicVarReleaseDO> {
        val groupName = publicVarGroup.groupName

        val (latestGroupRecord, latestVarPOs) = dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)

            // 获取数据库中最新版本的变量组信息
            val groupRecord = publicVarGroupDao.getRecordByGroupName(
                dslContext = context,
                projectId = projectId,
                groupName = groupName
            ) ?: throw ErrorCodeException(
                errorCode = ERROR_INVALID_PARAM_,
                params = arrayOf(groupName)
            )

            // 获取最新版本的变量列表
            val varPOs = publicVarService.getGroupPublicVar(
                projectId = projectId,
                groupName = groupName,
                version = groupRecord.version,
                context = context
            )

            Pair(groupRecord, varPOs)
        }

        val latestVarDOs = publicVarGroupReleaseRecordService.convertPOToDO(
            varPOs = latestVarPOs,
            projectId = projectId,
            groupName = groupName,
            version = latestGroupRecord.version
        )

        // 批量查询新变量的引用计数：动态版本 + 变量组当前最新版本（与变量组详情页语义一致）
        val newVarNames = publicVarGroup.publicVars.map { it.varName }
        val newVarReferCountMap = publicVarService.batchGetActiveReferCount(
            projectId = projectId,
            groupName = groupName,
            varNames = newVarNames
        )
        val newVarDOs = publicVarService.convertPublicVarVOsToDOsWithReferCount(
            publicVars = publicVarGroup.publicVars,
            referCountMap = newVarReferCountMap
        )

        val version = latestGroupRecord.version + 1
        val pubTime = LocalDateTime.now()

        return publicVarGroupReleaseRecordService.generateVarChangeRecords(
            PublicVarGroupReleaseRecordService.VarChangeRecordRequest(
                oldVars = latestVarDOs,
                newVars = newVarDOs,
                groupName = groupName,
                version = version,
                userId = userId,
                pubTime = pubTime,
                versionDesc = publicVarGroup.versionDesc
            )
        )
    }

    fun getProjectPublicParamByRef(
        userId: String,
        projectId: String,
        varGroupRefs: List<PublicVarGroupRef>
    ): List<PublicVarGroupVariable> {
        if (varGroupRefs.isEmpty()) {
            return emptyList()
        }

        // 第一阶段：批量查询所有变量组记录
        val groupNameVersionPairs = varGroupRefs.map { ref ->
            // version 优先；version 为 null 时 DAO 按 LATEST_FLAG=true 查最新版本
            ref.groupName to ref.version
        }
        val groupRecords = publicVarGroupDao.batchGetRecordsByGroupNameAndVersion(
            dslContext = dslContext,
            projectId = projectId,
            groupNameVersionPairs = groupNameVersionPairs
        )
        val groupRecordMap = groupRecords.associateBy { it.groupName }

        // 第二阶段：批量查询所有变量组的变量
        val groupNameVersionList = groupRecords.map { it.groupName to it.version }
        val allVarPOs = publicVarDao.batchListVarsByGroupNameAndVersion(
            dslContext = dslContext,
            projectId = projectId,
            groupNameVersionList = groupNameVersionList
        )
        val varPOsMap = allVarPOs.groupBy { it.groupName to it.version }

        // 第三阶段：按原始顺序组装结果
        val publicVarGroupVariables = mutableListOf<PublicVarGroupVariable>()
        val processedVarNames = mutableSetOf<String>()
        var currentIndex = 0

        varGroupRefs.forEach { varGroupRef ->
            val groupRecord = groupRecordMap[varGroupRef.groupName]
            if (groupRecord == null) {
                logger.warn("Variable group ${varGroupRef.groupName} not found in project $projectId")
                return@forEach
            }
            val varPOs = varPOsMap[groupRecord.groupName to groupRecord.version] ?: emptyList()
            // 动态版本引用（varGroupRef.version == null）时 groupVersion 传 null，
            // 使返回的 BuildFormProperty.varGroupVersion = null，不会被 processDynamicVarGroups
            // 误判为固定版本引用
            currentIndex = processVarPOs(
                varPOs = varPOs,
                groupName = groupRecord.groupName,
                groupVersion = varGroupRef.version,
                publicVarGroupVariables = publicVarGroupVariables,
                processedVarNames = processedVarNames,
                currentIndex = currentIndex
            )
        }
        return publicVarGroupVariables
    }

    /**
     * 处理变量PO列表，转换为PublicVarGroupVariable
     */
    private fun processVarPOs(
        varPOs: List<PublicVarPO>,
        groupName: String,
        groupVersion: Int?,
        publicVarGroupVariables: MutableList<PublicVarGroupVariable>,
        processedVarNames: MutableSet<String>,
        currentIndex: Int
    ): Int {
        val buildFormProperties = publicVarService.convertVarPOsToBuildFormProperties(varPOs)
        var index = currentIndex
        varPOs.forEachIndexed { i, po ->
            val varName = po.varName
            if (processedVarNames.contains(varName)) {
                throw ErrorCodeException(
                    errorCode = ERROR_PIPELINE_COMMON_VAR_GROUP_CONFLICT,
                    params = arrayOf(groupName, varName)
                )
            }
            val buildFormProperty = buildFormProperties[i]
            buildFormProperty.varGroupName = groupName
            buildFormProperty.varGroupVersion = groupVersion
            publicVarGroupVariables.add(
                PublicVarGroupVariable(
                    groupName = groupName,
                    groupVersion = groupVersion,
                    buildFormProperty = buildFormProperty,
                    originalIndex = index++
                )
            )
            processedVarNames.add(varName)
        }
        return index
    }

    fun convertGroupYaml(userId: String, projectId: String, publicVarGroup: PublicVarGroupVO): String {
        val params = publicVarGroup.publicVars.map { it.buildFormProperty }
        val variables = variableTransfer.makeVariableFromBuildParams(params, false)
        val parserVO = PublicVarGroupYamlParser(
            version = "v3.0",
            name = publicVarGroup.groupName,
            desc = publicVarGroup.desc ?: "",
            variables = variables ?: emptyMap()
        )
        return TransferMapper.getObjectMapper().writeValueAsString(parserVO)
    }

    fun convertYamlToGroup(userId: String, projectId: String, yaml: PublicVarGroupYamlStringVO): PublicVarGroupVO {
        return parseYamlToPublicVarGroupVO(yaml)
    }

    /**
     * 解析YAML字符串并转换为PublicVarGroupVO对象
     */
    private fun parseYamlToPublicVarGroupVO(yaml: PublicVarGroupYamlStringVO): PublicVarGroupVO {
        val parserVO = parseYamlToParserVO(yaml)
        return convertParserVOToPublicVarGroupVO(parserVO)
    }

    /**
     * 将YAML解析为PublicVarGroupYamlParser，并处理异常包装
     */
    private fun parseYamlToParserVO(yaml: PublicVarGroupYamlStringVO): PublicVarGroupYamlParser {
        return try {
            TransferMapper.getObjectMapper().readValue(
                yaml.yaml,
                object : TypeReference<PublicVarGroupYamlParser>() {}
            )
        } catch (e: Throwable) {
            logger.warn("Failed to parse YAML for public variable group", e)
            val errorMsg = buildErrorMsg(e)
            throw ErrorCodeException(
                errorCode = ERROR_PUBLIC_VAR_GROUP_YAML_PARSE_FAILED,
                params = arrayOf(errorMsg)
            )
        }
    }

    /**
     * 根据 Jackson 反序列化异常构建用户友好的错误消息
     * 通过异常类型判断替代字符串匹配，避免 Jackson 版本差异导致误判。
     */
    private fun buildErrorMsg(e: Throwable): String {
        return when (e) {
            // 未知字段：YAML 中存在目标类未定义的字段
            is UnrecognizedPropertyException -> {
                val fieldName = e.propertyName
                I18nUtil.getCodeLanMessage(
                    messageCode = ERROR_PUBLIC_VAR_GROUP_YAML_UNKNOWN_FIELD,
                    params = arrayOf(fieldName)
                )
            }
            // 反序列化失败：类型不匹配、格式错误等
            is MismatchedInputException -> {
                I18nUtil.getCodeLanMessage(ERROR_PUBLIC_VAR_GROUP_YAML_DESERIALIZE_ERROR)
            }
            // JSON 映射错误基类：包含缺失必填字段（MissingKotlinParameterException 是其子类）
            is JsonMappingException -> {
                val missingField = e.path.lastOrNull()?.fieldName
                if (missingField != null) {
                    I18nUtil.getCodeLanMessage(
                        messageCode = ERROR_PUBLIC_VAR_GROUP_YAML_MISSING_FIELD,
                        params = arrayOf(missingField)
                    )
                } else {
                    I18nUtil.getCodeLanMessage(ERROR_PUBLIC_VAR_GROUP_YAML_FORMAT_ERROR)
                }
            }
            else -> e.message ?: I18nUtil.getCodeLanMessage(ERROR_PUBLIC_VAR_GROUP_YAML_FORMAT_ERROR)
        }
    }

    /**
     * 将PublicVarGroupYamlParser转换为PublicVarGroupVO，并完成变量格式处理
     */
    private fun convertParserVOToPublicVarGroupVO(parserVO: PublicVarGroupYamlParser): PublicVarGroupVO {
        validateYamlFormat(parserVO)

        parserVO.variables.forEach { variable ->
            if (variable.value.const == true) {
                variable.value.readonly = true
                variable.value.allowModifyAtStartup = null
            }
        }
        val buildFormProperties = variableTransfer.makeVariableFromYaml(parserVO.variables)
        val publicVars = buildFormProperties.map { property ->
            PublicVarVO(
                varName = property.id,
                alias = property.name ?: "",
                type = if (property.constant == true) PublicVarTypeEnum.CONSTANT else PublicVarTypeEnum.VARIABLE,
                valueType = property.type,
                defaultValue = property.defaultValue,
                desc = property.desc,
                buildFormProperty = property
            )
        }

        return PublicVarGroupVO(
            groupName = parserVO.name,
            desc = parserVO.desc,
            publicVars = publicVars
        )
    }

    fun listPipelineVariables(
        userId: String,
        projectId: String,
        referId: String,
        referType: PublicVarGroupReferenceTypeEnum,
        referVersion: Int
    ): Result<List<PipelineRefPublicVarGroupDO>> {
        try {
            // 查询流水线关联的变量组信息
            val referInfos = publicVarGroupReferInfoDao.listVarGroupReferInfoByReferId(
                dslContext = dslContext,
                projectId = projectId,
                referId = referId,
                referType = referType,
                referVersion = referVersion
            )

            if (referInfos.isEmpty()) {
                return Result(emptyList())
            }

            // 批量查询变量组记录，避免 N+1 查询
            // referInfo.version 为 -1(DYNAMIC_VERSION) 时表示动态最新版本，需转为 null 让 DAO 走 LATEST_FLAG=true 查询
            val groupNameVersionPairs = referInfos.map {
                it.groupName to (if (it.version == DYNAMIC_VERSION) null else it.version)
            }
            val groupRecordMap = publicVarGroupDao.batchGetRecordsByGroupNameAndVersion(
                dslContext = dslContext,
                projectId = projectId,
                groupNameVersionPairs = groupNameVersionPairs
            ).associateBy { record ->
                val keyVersion = if (record.latestFlag == true) DYNAMIC_VERSION else record.version
                record.groupName to keyVersion
            }

            // 转换为PipelinePublicVarGroupDO列表
            val pipelineVarGroups = referInfos.mapNotNull { referInfo ->
                val groupRecord = groupRecordMap[referInfo.groupName to referInfo.version] ?: return@mapNotNull null
                PipelineRefPublicVarGroupDO(
                    groupName = groupRecord.groupName,
                    varCount = groupRecord.varCount,
                    desc = groupRecord.desc,
                    modifier = groupRecord.modifier,
                    updateTime = groupRecord.updateTime
                )
            }

            return Result(pipelineVarGroups)
        } catch (t: Throwable) {
            logger.warn("[$projectId|$referId] Failed to get pipeline variables", t)
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_LIST_PIPELINE_VARIABLES_FAILED,
                params = arrayOf(projectId, referId)
            )
        }
    }

    fun listProjectVarGroupInfo(userId: String, projectId: String): Result<List<PipelineRefPublicVarGroupDO>> {
        try {
            // 获取项目中所有的公共变量组
            val varGroups = publicVarGroupDao.listGroupsByProjectId(
                dslContext = dslContext,
                projectId = projectId
            )

            if (varGroups.isEmpty()) {
                return Result(emptyList())
            }

            // 转换为PipelinePublicVarGroupDO列表
            val pipelineVarGroups = varGroups.map { groupRecord ->
                PipelineRefPublicVarGroupDO(
                    groupName = groupRecord.groupName,
                    varCount = groupRecord.varCount,
                    desc = groupRecord.desc,
                    modifier = groupRecord.modifier,
                    updateTime = groupRecord.updateTime
                )
            }

            return Result(pipelineVarGroups)
        } catch (t: Throwable) {
            logger.warn("[$projectId] Failed to get project variable groups info", t)
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PUBLIC_VAR_GROUP_LIST_PROJECT_VAR_GROUP_FAILED,
                params = arrayOf(projectId)
            )
        }
    }

    /**
     * 验证YAML格式
     * 验证规则：
     * 1. 变量组名称：以英文字母开头，由字母、数字、下划线组成，长度3-32字符
     * 2. 变量名：以字母或下划线开头，由字母、数字、下划线组成
     */
    private fun validateYamlFormat(parserVO: PublicVarGroupYamlParser) {
        // 验证变量组名称格式
        if (parserVO.name.isBlank() || !parserVO.name.matches(GROUP_NAME_REGEX)) {
            throw ErrorCodeException(
                errorCode = ERROR_PUBLIC_VAR_GROUP_YAML_NAME_FORMAT
            )
        }

        // 验证变量名格式
        parserVO.variables.keys.forEach { varName ->
            if (varName.isBlank() || !varName.matches(VAR_NAME_REGEX)) {
                throw ErrorCodeException(
                    errorCode = ERROR_PUBLIC_VAR_GROUP_YAML_VARIABLE_NAME_FORMAT,
                    params = arrayOf(varName)
                )
            }
        }
    }
}