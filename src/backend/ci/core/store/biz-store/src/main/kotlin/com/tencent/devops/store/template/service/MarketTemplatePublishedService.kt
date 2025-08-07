package com.tencent.devops.store.template.service

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.store.pojo.template.TemplatePublishedVersionInfo

interface MarketTemplatePublishedService {
    /**
     *  创建模板发布历史
     * */
    fun create(templatePublishedVersionInfo: TemplatePublishedVersionInfo)

    /**
     *  删除模板发布历史版本
     * */
    fun deleteVersions(
        templateCode: String,
        versions: List<Long>
    )

    /**
     *  删除模板发布历史
     * */
    fun delete(templateCode: String)

    /**
     * 获取模板最新上架版本
     * */
    fun getLatest(templateCode: String): TemplatePublishedVersionInfo?

    /**
     * 获取模板发布历史
     * */
    fun list(
        userId: String,
        templateCode: String,
        page: Int,
        pageSize: Int
    ): Page<TemplatePublishedVersionInfo>

    /**
     * 批量获取模板最新上架版本
     * */
    fun listLatestRecords(templateCodes: List<String>): List<TemplatePublishedVersionInfo>

    /**
     * 下架模板
     * */
    fun offlineTemplate(
        templateCode: String,
        templateVersion: Long?
    )
}
