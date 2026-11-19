package com.tencent.devops.scm.pojo.tapd

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import io.swagger.v3.oas.annotations.media.Schema

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "TAPD 用户参与项目外层包装")
data class ParticipantWorkspaceResponse(
    @JsonProperty("Workspace")
    val workspace: TapdWorkspace? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "TAPD 应用已授权项目响应")
data class GrantedWorkspacesResponse(
    val list: List<GrantedWorkspaceItem>? = null,
    val pager: TapdPager? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GrantedWorkspaceItem(
    @JsonProperty("OpenOrganizationApp")
    val app: GrantedWorkspaceApp? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class GrantedWorkspaceApp(
    @JsonProperty("workspace_id")
    val workspaceId: String? = null,
    val type: String? = null,
    val created: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "TAPD 分页信息")
data class TapdPager(
    val count: Int = 0,
    val page: Int = 1,
    val limit: Int = 200
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "TAPD 已安装应用项目响应")
data class InstalledWorkspacesResponse(
    val total: Int = 0,
    @JsonProperty("worksapces")
    @JsonAlias("workspaces")
    val workspaces: List<InstalledWorkspace>? = null,
    val page: Int = 1,
    @JsonProperty("per_page")
    val perPage: Int = 200
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown = true)
data class InstalledWorkspace(
    val workspaceId: String? = null,
    val appVersion: String? = null,
    val installType: String? = null,
    val installedAt: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "TAPD 状态字段定义")
data class TapdStatusField(
    val name: String? = null,
    val label: String? = null,
    val options: Map<String, String>? = null
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "TAPD 需求字段配置")
data class TapdStoryFieldConfig(
    val status: TapdStatusField? = null,
    val priorityLabel: TapdStatusField? = null
)

@Schema(description = "TAPD 项目下拉选项；禁用提示由后端拼接到 name，前端无需额外字段")
data class TapdWorkspaceOption(
    val id: String,
    val name: String,
    val status: String? = null,
    val disabled: Boolean = false
)

@Schema(description = "TAPD 状态下拉选项")
data class TapdStatusOption(
    val id: String,
    val name: String
)

@Schema(description = "TAPD 应用已授权项目集合")
data class TapdAuthorizedWorkspaces(
    val known: Boolean,
    val workspaceIds: Set<String> = emptySet()
)
