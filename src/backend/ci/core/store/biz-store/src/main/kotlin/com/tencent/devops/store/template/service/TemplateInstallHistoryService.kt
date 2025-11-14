package com.tencent.devops.store.template.service

import com.tencent.devops.store.pojo.template.TemplateVersionInstallHistoryInfo

interface TemplateInstallHistoryService {
    /**
     * 创建模板安装历史
     * */
    fun create(installHistoryInfo: TemplateVersionInstallHistoryInfo)

    /**
     * 删除模板安装历史
     * */
    fun delete(templateCode: String)

    /**
     * 删除模板安装历史版本
     * */
    fun deleteVersions(
        srcTemplateCode: String,
        templateCode: String,
        versions: List<Long>
    )

    /**
     * 获取模板最近安装版本
     * */
    fun getRecently(templateCode: String): TemplateVersionInstallHistoryInfo?

    /**
     * 获取模板最新安装版本
     * */
    fun getLatest(templateCode: String): TemplateVersionInstallHistoryInfo?

    /**
     * 批量获取模板最新安装版本
     * */
    fun listLatestRecords(templateCodes: List<String>): List<TemplateVersionInstallHistoryInfo>
}
