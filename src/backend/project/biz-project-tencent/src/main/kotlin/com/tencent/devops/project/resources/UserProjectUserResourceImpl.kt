package com.tencent.devops.project.resources

import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.UserRole
import com.tencent.devops.project.pojo.user.ProjectUser
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.ProjectLocalService
import com.tencent.devops.project.service.tof.TOFService
import com.tencent.devops.project.user.api.UserProjectUserResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserProjectUserResourceImpl @Autowired constructor(
    private val tofService: TOFService,
    private val projectLocalService: ProjectLocalService,
    private val serviceCode: BSPipelineAuthServiceCode
) : UserProjectUserResource {

    override fun get(userId: String, bkToken: String?): Result<ProjectUser> {
        val staff = tofService.getStaffInfo(userId, bkToken!!)
        return Result(
                ProjectUser(
                        staff.ChineseName,
                        "http://dayu.oa.com/avatars/$userId/profile.jpg",
                        userId
                )
        )
    }

    override fun getDetail(userId: String, bk_ticket: String): Result<UserDeptDetail> {
        return Result(tofService.getUserDeptDetail(userId, bk_ticket))
    }

    override fun getProjectUsers(accessToken: String, userId: String, projectCode: String): Result<List<String>?> {
        return projectLocalService.getProjectUsers(accessToken, userId, projectCode)
    }

    override fun getProjectUserRoles(accessToken: String, userId: String, projectCode: String): Result<List<UserRole>> {
        return Result(projectLocalService.getProjectUserRoles(accessToken, userId, projectCode, serviceCode))
    }
}
