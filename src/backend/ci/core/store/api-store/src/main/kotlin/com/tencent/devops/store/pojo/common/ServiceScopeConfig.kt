package com.tencent.devops.store.pojo.common

import com.fasterxml.jackson.annotation.JsonIgnore
import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import com.tencent.devops.store.pojo.common.enums.ServiceScopeEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "服务范围配置")
data class ServiceScopeConfig(
    @get:Schema(title = "服务范围", required = true)
    val serviceScope: ServiceScopeEnum,

    @get:Schema(title = "所属插件分类代码", required = true)
    val classifyCode: String,

    @get:Schema(
        title = "Job类型配置列表（包含每个 jobType 及其操作系统信息）",
        required = true
    )
    val jobTypeConfigs: List<JobTypeConfig>,

    @get:Schema(title = "插件标签列表", required = false)
    val labelIdList: List<String>? = null
) {
    /**
     * 获取生效的 jobType 列表，从 jobTypeConfigs 提取。
     */
    @JsonIgnore
    fun getEffectiveJobTypes(): List<JobTypeEnum> {
        return jobTypeConfigs.map { it.jobType }
    }

    /**
     * 获取生效的 jobType → OS 列表映射。仅包含编译环境且有 OS 配置的 jobType。
     */
    @JsonIgnore
    fun getEffectiveOsMap(): Map<String, List<String>> {
        return jobTypeConfigs
            .filter { !it.osList.isNullOrEmpty() && it.jobType.isBuildEnv() }
            .associate { it.jobType.name to it.osList!! }
    }
}
