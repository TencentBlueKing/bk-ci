package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.constants.ManagerScopesEnum
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.entity.SearchUserAndDeptEntity
import com.tencent.devops.auth.pojo.BkUserDeptInfo
import com.tencent.devops.auth.pojo.BkUserInfo
import com.tencent.devops.auth.pojo.vo.BkDeptDetailsVo
import com.tencent.devops.auth.pojo.vo.BkUserInfoVo
import com.tencent.devops.auth.pojo.vo.DeptInfoVo
import com.tencent.devops.auth.pojo.vo.UserAndDeptInfoVo
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.service.tenant.TenantUtils
import jakarta.ws.rs.HttpMethod
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value

/**
 * 多租户部门服务
 */
class TenantAuthDeptServiceImpl : DeptService {
    @Value("\${bk.apigw.user.host:#{null}}")
    val bkApigwUserHost: String? = null

    override fun getUserParentDept(userId: String, tenantId: String?): Int {
        return listUserDepartment(userId, false, tenantId).data[0].id
    }

    override fun getDeptByName(
        deptName: String,
        userId: String,
        tenantId: String?
    ): DeptInfoVo? {
        logger.warn("getDeptByName isn`t support in tenant environment: $deptName, $userId, $tenantId")
        return null
    }

    override fun getUserDeptInfo(
        userId: String,
        tenantId: String?
    ): Set<String> {
        val userDepartment = listUserDepartment(userId, true, tenantId)
        val result = mutableSetOf<String>(userDepartment.data[0].id.toString())
        result.addAll(userDepartment.data[0].ancestors.map { it.id.toString() })
        return result
    }

    override fun getUserInfo(
        userId: String,
        name: String,
        tenantId: String?
    ): UserAndDeptInfoVo? {
        return retrieveUser(userId, tenantId).toVo()
    }

    override fun getUserInfo(userId: String, tenantId: String?): UserAndDeptInfoVo? {
        return retrieveUser(userId, tenantId).toVo()
    }

    override fun getUserInfoFromExternal(userId: String, tenantId: String?): UserAndDeptInfoVo? {
        return getUserInfo(userId, tenantId)
    }

    override fun getMemberInfo(
        memberId: String,
        memberType: ManagerScopesEnum,
        tenantId: String?
    ): UserAndDeptInfoVo {
        return listMemberInfos(listOf(memberId), memberType, tenantId).firstOrNull() ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.USER_NOT_EXIST,
            params = arrayOf(memberId),
            defaultMessage = "member $memberId not exist"
        )
    }

    override fun listMemberInfos(
        memberIds: List<String>,
        memberType: ManagerScopesEnum,
        tenantId: String?
    ): List<UserAndDeptInfoVo> {
        if (memberIds.isEmpty() || tenantId.isNullOrBlank()) {
            logger.warn("listMemberInfos, memberIds is $memberIds , tenantId is $tenantId")
            return emptyList()
        }
        return if (memberType == ManagerScopesEnum.USER) {
            batchQueryUserDisplayInfo(memberIds, tenantId).toVos()
        } else {
            batchLookupDepartment(memberIds, tenantId).toVos()
        }
    }

    override fun listDepartedMembers(
        memberIds: List<String>,
        tenantId: String?
    ): List<String> {
        val activeMembers = listMemberInfos(
            memberIds = memberIds,
            memberType = ManagerScopesEnum.USER,
            tenantId = tenantId
        ).map { it.name }
        return memberIds.subtract(activeMembers.toSet()).toList()
    }

    override fun isUserDeparted(userId: String, tenantId: String?): Boolean {
        return listMemberInfos(
            memberIds = listOf(userId),
            memberType = ManagerScopesEnum.USER,
            tenantId = tenantId
        ).isEmpty()
    }

    override fun listDeptInfos(searchUserEntity: SearchUserAndDeptEntity, tenantId: String?): DeptInfoVo {
        logger.warn("listDeptInfos isn`t support in tenant environment: $searchUserEntity, $tenantId")
        return DeptInfoVo(count = 0, results = emptyList())
    }

    override fun listUserInfos(searchUserEntity: SearchUserAndDeptEntity, tenantId: String?): BkUserInfoVo {
        try {
            return TenantUtils.callApigw(
                apigwHost = bkApigwUserHost!!,
                path = LIST_USER,
                params = mapOf(
                    "page" to (searchUserEntity.page ?: 0),
                    "page_size" to (searchUserEntity.pageSize ?: 100),
                ),
                tenantId = tenantId,
                method = HttpMethod.GET,
                respType = ListUserResp::class.java
            ).toVo()
        } catch (e: Exception) {
            logger.error("listUserInfos error: $e")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.USER_NOT_EXIST,
                defaultMessage = "listUserInfos error, page:${searchUserEntity.page}, pageSize:${searchUserEntity.pageSize}"
            )
        }
    }

    override fun getUserDeptDetails(userId: String, tenantId: String?): BkDeptDetailsVo? {
        val departmentInfos = listUserDepartment(userId, true, tenantId).data
        if (departmentInfos.isNotEmpty()) {
            val department = departmentInfos[0]
            return BkDeptDetailsVo(
                id = department.id,
                name = department.name,
                family = department.ancestors.map {
                    BkUserDeptInfo(
                        id = it.id.toString(),
                        name = it.name,
                        fullName = null
                    )
                }
            )
        } else {
            logger.warn("getUserDeptDetails isn`t support in tenant environment: $userId, $tenantId")
            return null
        }

    }

    /**
     * 查询用户所在的部门列表
     */
    private fun listUserDepartment(
        bkUsername: String,
        withAncestors: Boolean,
        tenantId: String?
    ): ListUserDepartmentResp {
        val params = mapOf(
            "with_ancestors" to withAncestors
        )
        try {
            return TenantUtils.callApigw(
                apigwHost = bkApigwUserHost!!,
                path = LIST_USER_DEPARTMENT.replace("{bk_username}", bkUsername),
                params = params,
                tenantId = tenantId,
                method = HttpMethod.GET,
                respType = ListUserDepartmentResp::class.java
            )
        } catch (e: Exception) {
            logger.error("listUserDepartment error: $e")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.USER_NOT_EXIST,
                params = arrayOf(bkUsername),
                defaultMessage = "user $bkUsername not exist"
            )
        }
    }

    /**
     * 查询用户信息
     */
    private fun retrieveUser(
        bkUsername: String,
        tenantId: String?
    ): RetrieveUserResp {
        try {
            return TenantUtils.callApigw(
                apigwHost = bkApigwUserHost!!,
                path = RETRIEVE_USER.replace("{bk_username}", bkUsername),
                params = emptyMap(),
                tenantId = tenantId,
                method = HttpMethod.GET,
                respType = RetrieveUserResp::class.java
            )
        } catch (e: Exception) {
            logger.error("retrieveUser error: $e")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.USER_NOT_EXIST,
                params = arrayOf(bkUsername),
                defaultMessage = "user $bkUsername not exist"
            )
        }
    }

    /**
     * 批量查询用户信息
     */
    private fun batchQueryUserDisplayInfo(
        bkUsernames: List<String>,
        tenantId: String?
    ): BatchQueryUserDisplayInfoResp {
        try {
            return TenantUtils.callApigw(
                apigwHost = bkApigwUserHost!!,
                path = BATCH_QUERY_USER_DISPLAY_INFO,
                params = mapOf(
                    "bk_usernames" to bkUsernames.joinToString(",")
                ),
                tenantId = tenantId,
                method = HttpMethod.GET,
                respType = BatchQueryUserDisplayInfoResp::class.java
            )
        } catch (e: Exception) {
            logger.error("batchQueryUserDisplayInfo error: $e")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.USER_NOT_EXIST,
                params = bkUsernames.toTypedArray(),
                defaultMessage = "user ${bkUsernames.joinToString(",")} not exist"
            )
        }
    }

    /**
     * 批量查询部门信息
     */
    private fun batchLookupDepartment(
        departmentIds: List<String>,
        tenantId: String?,
        withOrganizationPath: Boolean = false
    ): BatchLookupDepartmentResp {
        try {
            return TenantUtils.callApigw(
                apigwHost = bkApigwUserHost!!,
                path = BATCH_LOOKUP_DEPARTMENT,
                params = mapOf(
                    "department_ids" to departmentIds.joinToString(","),
                    "with_organization_path" to withOrganizationPath,
                ),
                tenantId = tenantId,
                method = HttpMethod.GET,
                respType = BatchLookupDepartmentResp::class.java
            )
        } catch (e: Exception) {
            logger.error("batchLookupDepartment error: $e")
            throw ErrorCodeException(
                errorCode = AuthMessageCode.USER_NOT_EXIST,
                params = departmentIds.toTypedArray(),
                defaultMessage = "department ${departmentIds.joinToString(",")} not exist"
            )
        }
    }

    companion object {
        const val LIST_USER_DEPARTMENT = "/api/v3/open/tenant/users/{bk_username}/departments/"
        const val RETRIEVE_USER = "/api/v3/open/tenant/users/{bk_username}/"
        const val BATCH_QUERY_USER_DISPLAY_INFO = "/api/v3/open/tenant/users/-/display_info/"
        const val BATCH_LOOKUP_DEPARTMENT = "/api/v3/open/tenant/departments/-/lookup/"
        const val LIST_USER = "/api/v3/open/tenant/users/"
        private val logger = LoggerFactory.getLogger(TenantAuthDeptServiceImpl::class.java)
    }

    data class ListUserDepartmentResp(
        val data: List<DepartmentInfo>
    )

    data class DepartmentInfo(
        val id: Int,
        val name: String,
        val ancestors: List<Ancestor>
    )

    data class Ancestor(
        val id: Int,
        val name: String
    )

    data class RetrieveUserResp(
        val data: UserInfo
    ) {
        fun toVo(): UserAndDeptInfoVo {
            return UserAndDeptInfoVo(
                id = 0,
                name = data.bk_username,
                displayName = data.display_name,
                type = ManagerScopesEnum.USER,
                deptInfo = emptyList()
            )
        }
    }

    data class UserInfo(
        val tenant_id: String,
        val bk_username: String,
        val display_name: String,
        val time_zone: String,
        val language: String
    )

    data class BatchQueryUserDisplayInfoResp(
        val data: List<UserDisplayInfo>
    ) {
        fun toVos(): List<UserAndDeptInfoVo> {
            if (data.isEmpty()) return emptyList()
            val result = mutableListOf<UserAndDeptInfoVo>()
            for (info in data) {
                result.add(
                    UserAndDeptInfoVo(
                        id = 0,
                        name = info.bk_username,
                        displayName = info.display_name,
                        type = ManagerScopesEnum.USER,
                        deptInfo = emptyList()
                    )
                )
            }
            return result
        }
    }

    data class UserDisplayInfo(
        val bk_username: String,
        val display_name: String
    )

    data class BatchLookupDepartmentResp(
        val data: List<LookupDepartment>
    ) {
        fun toVos(): List<UserAndDeptInfoVo> {
            if (data.isEmpty()) return emptyList()
            val result = mutableListOf<UserAndDeptInfoVo>()
            for (i in 0..data.size - 1) {
                val info = data[i]
                result.add(
                    UserAndDeptInfoVo(
                        id = 0,
                        name = info.name,
                        displayName = info.name,
                        type = ManagerScopesEnum.DEPARTMENT,
                        hasChild = i < data.size - 1,
                    )
                )
            }
            return result
        }
    }

    data class LookupDepartment(
        val id: Int,
        val name: String,
        val organization_path: String? = null
    )

    data class ListUserResp(
        val data: ListUser
    ) {
        fun toVo(): BkUserInfoVo {
            val userInfos = data.results.map {
                BkUserInfo(
                    id = 0,
                    userName = it.bk_username,
                    displayName = it.display_name,
                    enabled = it.status == "enabled",
                    extras = null,
                    departments = null
                )
            }
            return BkUserInfoVo(data.count, userInfos)
        }
    }

    data class ListUser(
        val count: Int,
        val results: List<User>
    ) {

    }

    data class User(
        val bk_username: String,
        val login_name: String,
        val full_name: String,
        val display_name: String,
        val status: String
    )
}
