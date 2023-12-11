package com.tencent.devops.auth.service.security

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.pojo.vo.SecOpsWaterMarkInfoVo
import com.tencent.devops.auth.pojo.vo.UserAndDeptInfoVo
import com.tencent.devops.auth.service.DeptService
import com.tencent.devops.auth.service.iam.PermissionProjectService
import com.tencent.devops.common.api.exception.ErrorCodeException

/**
 * 安全相关接口
 */
abstract class SecurityService(
    val deptService: DeptService,
    val permissionProjectService: PermissionProjectService
) {
    fun getUserSecurityInfo(
        userId: String,
        projectCode: String
    ): UserAndDeptInfoVo {
        val userInfo = deptService.getUserInfo(
            userId = userId,
            name = userId
        ) ?: throw ErrorCodeException(
            errorCode = AuthMessageCode.ERROR_USER_NOT_EXIST,
            defaultMessage = "user not exist!$userId"
        )
        val belongProjectMember = permissionProjectService.getProjectUsers(
            projectCode = projectCode,
            group = null
        ).contains(userId)
        val userWaterMark = getUserWaterMark(userId = userId)
        return UserAndDeptInfoVo(
            id = userInfo.id,
            name = userInfo.name,
            type = userInfo.type,
            hasChild = userInfo.hasChild,
            deptInfo = userInfo.deptInfo,
            extras = userInfo.extras,
            waterMark = userWaterMark.data,
            belongProjectMember = belongProjectMember
        )
    }

    /**
     * 获取用户水印信息
     */
    abstract fun getUserWaterMark(userId: String): SecOpsWaterMarkInfoVo
}
