package com.tencent.devops.store.pojo.common

import com.tencent.devops.store.pojo.atom.enums.JobTypeEnum
import com.tencent.devops.store.pojo.common.enums.ServiceScopeEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "服务范围配置")
data class ServiceScopeConfig(
    @get:Schema(title = "服务范围", required = true)
    val serviceScope: ServiceScopeEnum,

    @get:Schema(title = "所属插件分类代码", required = true)
    val classifyCode: String,

    @get:Schema(title = "适用Job类型（单个，向后兼容）", required = false)
    @Deprecated("使用 jobTypes 替代", replaceWith = ReplaceWith("jobTypes"))
    val jobType: JobTypeEnum? = null,

    @get:Schema(title = "适用Job类型列表（一个 scope 可支持多种 jobType）", required = false)
    val jobTypes: List<JobTypeEnum>? = null,

    @get:Schema(title = "插件标签列表", required = false)
    val labelIdList: List<String>? = null
) {
    /**
     * 获取有效的 jobType 列表：优先 jobTypes，回退到单个 jobType，去重后返回。
     */
    fun getEffectiveJobTypes(): List<JobTypeEnum> {
        if (!jobTypes.isNullOrEmpty()) return jobTypes.distinct()
        return listOfNotNull(jobType)
    }
}
