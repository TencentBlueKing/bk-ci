package com.tencent.devops.auth.pojo.dto

import com.tencent.devops.auth.pojo.request.ProjectMembersQueryConditionReq
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.PageUtil
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "项目成员查询业务处理实体")
data class ProjectMembersQueryConditionDTO(
    @get:Schema(title = "项目ID")
    val projectCode: String,
    @get:Schema(title = "成员类型")
    val memberType: String? = null,
    @get:Schema(title = "用户名称")
    val userName: String? = null,
    @get:Schema(title = "部门名称")
    val deptName: String? = null,
    @get:Schema(title = "用户组名称")
    val groupName: String? = null,
    @get:Schema(title = "用户组Id")
    val iamGroupIds: List<Int>? = null,
    @get:Schema(title = "最小过期时间")
    val minExpiredTime: LocalDateTime? = null,
    @get:Schema(title = "最大过期时间")
    val maxExpiredTime: LocalDateTime? = null,
    @get:Schema(title = "离职标识")
    val departedFlag: Boolean? = false,
    @get:Schema(title = "是否查询模板")
    val queryTemplate: Boolean? = false,
    @get:Schema(title = "限制")
    val limit: Int? = null,
    @get:Schema(title = "起始值")
    val offset: Int? = null
) {
    companion object {
        fun build(
            projectMembersQueryConditionReq: ProjectMembersQueryConditionReq,
            iamGroupIds: List<Int>?
        ): ProjectMembersQueryConditionDTO {
            return with(projectMembersQueryConditionReq) {
                val minExpiredTime = minExpiredAt?.let { DateTimeUtil.convertTimestampToLocalDateTime(it / 1000) }
                val maxExpiredTime = maxExpiredAt?.let { DateTimeUtil.convertTimestampToLocalDateTime(it / 1000) }
                val limit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
                ProjectMembersQueryConditionDTO(
                    projectCode = projectCode,
                    memberType = memberType,
                    userName = userName,
                    deptName = deptName,
                    groupName = groupName,
                    iamGroupIds = iamGroupIds,
                    minExpiredTime = minExpiredTime,
                    maxExpiredTime = maxExpiredTime,
                    departedFlag = departedFlag,
                    limit = limit.limit,
                    offset = limit.offset
                )
            }
        }
    }
}
