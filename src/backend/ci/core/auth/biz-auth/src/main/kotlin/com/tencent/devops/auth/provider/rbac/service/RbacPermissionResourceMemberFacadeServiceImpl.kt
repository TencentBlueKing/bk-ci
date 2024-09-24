package com.tencent.devops.auth.provider.rbac.service

import com.tencent.devops.auth.dao.AuthResourceGroupDao
import com.tencent.devops.auth.dao.AuthResourceGroupMemberDao
import com.tencent.devops.auth.pojo.ResourceMemberInfo
import com.tencent.devops.auth.pojo.dto.IamGroupIdsQueryConditionDTO
import com.tencent.devops.auth.pojo.dto.ProjectMembersQueryConditionDTO
import com.tencent.devops.auth.pojo.request.ProjectMembersQueryConditionReq
import com.tencent.devops.auth.service.iam.PermissionResourceGroupFacadeService
import com.tencent.devops.auth.service.iam.PermissionResourceMemberFacadeService
import com.tencent.devops.auth.service.iam.PermissionResourceMemberService
import com.tencent.devops.common.api.model.SQLPage
import org.jooq.DSLContext
import org.slf4j.LoggerFactory

class RbacPermissionResourceMemberFacadeServiceImpl(
    private val permissionResourceGroupFacadeService: PermissionResourceGroupFacadeService,
    private val permissionResourceMemberService: PermissionResourceMemberService,
    private val authResourceGroupDao: AuthResourceGroupDao,
    private val authResourceGroupMemberDao: AuthResourceGroupMemberDao,
    private val dslContext: DSLContext
) : PermissionResourceMemberFacadeService {
    override fun listProjectMembersByComplexConditions(
        conditionReq: ProjectMembersQueryConditionReq
    ): SQLPage<ResourceMemberInfo> {
        logger.info("list project members by complex conditions: $conditionReq")
        // 不允许同时查询部门名称和用户名称
        if (conditionReq.userName != null && conditionReq.deptName != null) {
            return SQLPage(count = 0, records = emptyList())
        }

        // 简单查询直接返回结果
        if (!conditionReq.isComplexQuery()) {
            return permissionResourceMemberService.listProjectMembers(
                projectCode = conditionReq.projectCode,
                memberType = conditionReq.memberType,
                userName = conditionReq.userName,
                deptName = conditionReq.deptName,
                departedFlag = conditionReq.departedFlag,
                page = conditionReq.page,
                pageSize = conditionReq.pageSize
            )
        }

        // 处理复杂查询条件
        val iamGroupIdsByCondition = if (conditionReq.isNeedToQueryIamGroups()) {
            permissionResourceGroupFacadeService.listIamGroupIdsByConditions(
                condition = IamGroupIdsQueryConditionDTO(
                    projectCode = conditionReq.projectCode,
                    groupName = conditionReq.groupName,
                    relatedResourceType = conditionReq.relatedResourceType,
                    relatedResourceCode = conditionReq.relatedResourceCode,
                    action = conditionReq.action
                )
            )
        } else {
            emptyList()
        }.toMutableList()

        // 查询不到用户组，直接返回空
        if (conditionReq.isNeedToQueryIamGroups() && iamGroupIdsByCondition.isEmpty()) {
            return SQLPage(0, emptyList())
        }

        val conditionDTO = ProjectMembersQueryConditionDTO.build(conditionReq, iamGroupIdsByCondition)

        if (iamGroupIdsByCondition.isNotEmpty()) {
            logger.debug("iamGroupIdsByCondition :$iamGroupIdsByCondition")
            // 根据用户组Id查询出对应用户组中的人员模板成员
            val iamTemplateIds = authResourceGroupMemberDao.listProjectMembersByComplexConditions(
                dslContext = dslContext,
                conditionDTO = ProjectMembersQueryConditionDTO(
                    projectCode = conditionDTO.projectCode,
                    queryTemplate = true,
                    iamGroupIds = conditionDTO.iamGroupIds
                )
            )
            if (iamTemplateIds.isNotEmpty()) {
                // 根据查询出的人员模板ID，查询出对应的组ID
                val iamGroupIdsFromTemplate = authResourceGroupDao.listIamGroupIdsByConditions(
                    dslContext = dslContext,
                    projectCode = conditionDTO.projectCode,
                    iamTemplateIds = iamTemplateIds.map { it.id.toInt() }
                )
                iamGroupIdsByCondition.addAll(iamGroupIdsFromTemplate)
                logger.debug("iamGroupIdsByCondition and template :$iamGroupIdsByCondition")
            }
        }

        val records = authResourceGroupMemberDao.listProjectMembersByComplexConditions(
            dslContext = dslContext,
            conditionDTO = conditionDTO
        )
        logger.debug("listProjectMembersByComplexConditions :$records")

        val count = authResourceGroupMemberDao.countProjectMembersByComplexConditions(
            dslContext = dslContext,
            conditionDTO = conditionDTO
        )
        logger.debug("listProjectMembersByComplexConditions :$count")
        // 添加离职标志
        return if (conditionDTO.departedFlag == false) {
            SQLPage(count, records)
        } else {
            SQLPage(count, permissionResourceMemberService.addDepartedFlagToMembers(records))
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionResourceMemberService::class.java)

        // 永久过期时间
        private const val PERMANENT_EXPIRED_TIME = 4102444800000L
    }
}
