package com.tencent.devops.process.util

import com.tencent.devops.common.pipeline.Model

object PipelineTemplateUtil {

    private const val MAX_VERSION_NAME_LENGTH = 64 // 数据库 VARCHAR(64) 限制

    /**
     * 构建带后缀的版本名称，确保不超过最大长度限制
     *
     * @param originalName 原始版本名称
     * @param suffix 后缀（如 "-200"）
     * @param maxLength 最大长度限制，默认为 64
     * @return 截断后的版本名称（如果需要）
     */
    fun buildVersionNameWithSuffix(
        originalName: String,
        suffix: String,
        maxLength: Int = MAX_VERSION_NAME_LENGTH
    ): String {
        val newName = "$originalName$suffix"
        return if (newName.length > maxLength) {
            // 需要截断原始名称以容纳后缀
            val maxOriginalLength = maxLength - suffix.length
            if (maxOriginalLength <= 0) {
                // 后缀本身就超长了，只能返回后缀
                suffix.take(maxLength)
            } else {
                // 截断原始名称，保留完整后缀
                "${originalName.take(maxOriginalLength)}$suffix"
            }
        } else {
            newName
        }
    }

    /**
     * 将 v2 版本的合并参数拆分为 v1 版本的 templateParams 和 params
     *
     * v2 版本：params 包含所有参数，其中 constant = true 的来自原 templateParams
     * v1 版本：templateParams 和 params 分开存储
     */
    fun splitParamsForV1Compatibility(model: Model) {
        val triggerContainer = model.getTriggerContainer()
        val allParams = triggerContainer.params
        // 将参数按 constant 标记分组
        val (templateParams, params) = allParams.partition { it.constant == true }
        triggerContainer.params = params.toMutableList().map {
            // 模版入参+实例化不入参,那么旧变量应该是不入参
            if (it.required && it.asInstanceInput == false) {
                it.copy(required = false, asInstanceInput = null)
            } else {
                it.copy(asInstanceInput = null)
            }
        }
        triggerContainer.templateParams = takeIf { templateParams.isNotEmpty() }?.let {
            templateParams.map { it.copy(constant = false, asInstanceInput = null) }
        }
    }
}
