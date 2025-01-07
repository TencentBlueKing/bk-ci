package com.tencent.devops.auth.pojo.vo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "成员退出项目检查返回体")
data class MemberExitsProjectCheckVo(
    @get:Schema(title = "组织加入的数量")
    val departmentJoinedCount: Int? = 0,
    @get:Schema(title = "组织")
    val departments: String? = "",
    @get:Schema(title = "唯一管理员组的数量")
    val uniqueManagerCount: Int? = 0,
    @get:Schema(title = "流水线授权数量")
    val pipelineAuthorizationCount: Int? = 0,
    @get:Schema(title = "代码库授权数量")
    val repositoryAuthorizationCount: Int? = 0,
    @get:Schema(title = "环境节点授权数量")
    val envNodeAuthorizationCount: Int? = 0
) {
    fun canExitsProjectDirectly(): Boolean {
        return departmentJoinedCount == 0 && uniqueManagerCount == 0 && pipelineAuthorizationCount == 0 &&
            repositoryAuthorizationCount == 0 && envNodeAuthorizationCount == 0
    }
}
