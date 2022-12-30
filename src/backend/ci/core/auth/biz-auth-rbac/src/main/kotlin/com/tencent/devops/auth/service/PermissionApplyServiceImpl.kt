package com.tencent.devops.auth.service

import com.google.common.cache.CacheBuilder
import com.tencent.bk.sdk.iam.dto.V2PageInfoDTO
import com.tencent.bk.sdk.iam.dto.application.ApplicationDTO
import com.tencent.bk.sdk.iam.dto.manager.dto.SearchGroupDTO
import com.tencent.bk.sdk.iam.dto.manager.vo.V2ManagerRoleGroupVO
import com.tencent.bk.sdk.iam.dto.response.GroupPermissionDetailResponseDTO
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthActionDao
import com.tencent.devops.auth.dao.AuthResourceTypeDao
import com.tencent.devops.auth.pojo.ApplicationInfo
import com.tencent.devops.auth.pojo.RelatedResourceInfo
import com.tencent.devops.auth.pojo.SearchGroupInfo
import com.tencent.devops.auth.pojo.vo.ActionInfoVo
import com.tencent.devops.auth.pojo.vo.GroupPermissionDetailVo
import com.tencent.devops.auth.pojo.vo.ResourceTypeInfoVo
import com.tencent.devops.auth.service.iam.PermissionApplyService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceProjectResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
@Suppress("ALL")
class PermissionApplyServiceImpl @Autowired constructor(
    val dslContext: DSLContext,
    val authResourceTypeDao: AuthResourceTypeDao,
    val authActionDao: AuthActionDao,
    val v2ManagerService: V2ManagerService,
    val client: Client
) : PermissionApplyService {
    @Value("\${auth.iamSystem:}")
    val systemId = ""
    private val actionCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build<String, List<ActionInfoVo>>()
    private val resourceTypesCache = CacheBuilder.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build<String, List<ResourceTypeInfoVo>>()

    override fun listResourceTypes(userId: String): List<ResourceTypeInfoVo> {
        if (resourceTypesCache.getIfPresent(ALL_RESOURCE) == null) {
            val resourceTypeList = authResourceTypeDao.list(dslContext).map {
                ResourceTypeInfoVo(
                    resourceType = it.resourcetype,
                    name = it.name,
                    parent = it.parent,
                    system = it.system
                )
            }
            resourceTypesCache.put(ALL_RESOURCE, resourceTypeList)
        }
        return resourceTypesCache.getIfPresent(ALL_RESOURCE)!!
    }

    override fun listActions(userId: String, resourceType: String): List<ActionInfoVo> {
        if (actionCache.getIfPresent(resourceType) == null) {
            val actionList = authActionDao.list(dslContext, resourceType).map {
                ActionInfoVo(
                    actionId = it.actionid,
                    actionName = it.actionname,
                    resourceType = it.resourcetype
                )
            }
            actionCache.put(resourceType, actionList)
        }
        return actionCache.getIfPresent(resourceType)!!

    }

    override fun listGroups(
        userId: String,
        projectId: String,
        searchGroupInfo: SearchGroupInfo
    ): V2ManagerRoleGroupVO {
        val projectInfo = client.get(ServiceProjectResource::class).get(projectId).data ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.RESOURCE_NOT_FOUND,
            params = arrayOf(projectId),
            defaultMessage = "权限系统：项目[$projectId]不存在"
        )
        val searchGroupDTO = SearchGroupDTO
            .builder()
            .inherit(searchGroupInfo.inherit)
            .actionId(searchGroupInfo.actionId)
            .resourceTypeSystemId(systemId)
            .resourceTypeId(searchGroupInfo.resourceType)
            .resourceId(searchGroupInfo.resourceCode)
            .bkIamPath(searchGroupInfo.bkIamPath)
            .name(searchGroupInfo.name)
            .description(searchGroupInfo.description)
            .build()
        val v2PageInfoDTO = V2PageInfoDTO()
        v2PageInfoDTO.pageSize = searchGroupInfo.pageSize
        v2PageInfoDTO.page = searchGroupInfo.page
        try {
            return v2ManagerService.getGradeManagerRoleGroupV2(projectInfo.relationId, searchGroupDTO, v2PageInfoDTO)
        } catch (e: Exception) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.GET_IAM_GROUP_FAIL,
                defaultMessage = "权限系统：获取用户组失败！"
            )
        }
    }

    override fun applyToJoinGroup(userId: String, applicationInfo: ApplicationInfo): Boolean {
        try {
            val iamApplicationDTO = ApplicationDTO
                .builder()
                .groupId(applicationInfo.groupIds)
                .applicant(userId)
                .expiredAt(applicationInfo.expiredAt.toLong())
                .reason(applicationInfo.reason).build()
            v2ManagerService.createRoleGroupApplicationV2(iamApplicationDTO)
        } catch (e: Exception) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.APPLY_TO_JOIN_GROUP_FAIL,
                params = arrayOf(applicationInfo.groupIds.toString()),
                defaultMessage = "权限系统：申请加入用户组[${applicationInfo.groupIds}]失败！"
            )
        }
        return true
    }

    override fun getGroupPermissionDetail(userId: String, groupId: Int): List<GroupPermissionDetailVo> {
        val iamGroupPermissionDetailList: List<GroupPermissionDetailResponseDTO>
        try {
            iamGroupPermissionDetailList = v2ManagerService.getGroupPermissionDetail(groupId)
        } catch (e: Exception) {
            throw ErrorCodeException(
                errorCode = AuthMessageCode.GET_GROUP_PERMISSION_DETAIL_FAIL,
                params = arrayOf(groupId.toString()),
                defaultMessage = "权限系统：获取用户组[$groupId]权限信息失败！"
            )
        }
        val groupPermissionDetailVoList: MutableList<GroupPermissionDetailVo> = ArrayList()
        iamGroupPermissionDetailList.forEach {
            val relatedResourceTypesDTO = it.resourceGroups[0].relatedResourceTypesDTO[0]
            val relatedResourceInfo = RelatedResourceInfo(
                type = relatedResourceTypesDTO.type,
                name = relatedResourceTypesDTO.name,
                instances = relatedResourceTypesDTO.condition[0].instances[0]
            )
            groupPermissionDetailVoList.add(
                GroupPermissionDetailVo(
                    actionId = it.id,
                    name = it.name,
                    relatedResourceInfo = relatedResourceInfo
                )
            )
        }
        return groupPermissionDetailVoList
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GroupUserService::class.java)
        private const val ALL_RESOURCE = "all_resource"
    }
}
