package com.tencent.devops.environment.pojo.thirdpartyagent

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "安装环境摘要")
data class InstallEnvItem(
    @get:Schema(title = "环境 HashId", required = true)
    val envHashId: String,
    @get:Schema(title = "环境名称", required = true)
    val name: String
)

@Schema(title = "安装环境预览")
data class InstallEnvPreview(
    @get:Schema(title = "标签已完整匹配的环境", required = true)
    val matchedEnvironments: List<InstallEnvItem> = emptyList(),
    @get:Schema(title = "仅等待未知内置标签确认的环境", required = true)
    val pendingEnvironments: List<InstallEnvItem> = emptyList(),
    @get:Schema(title = "重装前后均匹配的环境", required = true)
    val unchangedEnvironments: List<InstallEnvItem> = emptyList(),
    @get:Schema(title = "重装后新加入的环境", required = true)
    val joiningEnvironments: List<InstallEnvItem> = emptyList(),
    @get:Schema(title = "重装后离开的动态环境", required = true)
    val leavingEnvironments: List<InstallEnvItem> = emptyList()
)
