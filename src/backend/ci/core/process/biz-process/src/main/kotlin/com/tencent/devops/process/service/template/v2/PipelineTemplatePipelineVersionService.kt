package com.tencent.devops.process.service.template.v2

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.PipelineInstanceTypeEnum
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.pojo.BuildNo
import com.tencent.devops.process.dao.template.PipelineTemplatePipelineVersionDao
import com.tencent.devops.process.engine.dao.template.TemplatePipelineDao
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.pojo.template.v2.PTemplatePipelineVersion
import com.tencent.devops.process.pojo.template.v2.PTemplatePipelineVersionCommonCondition
import com.tencent.devops.process.pojo.template.v2.PTemplatePipelineVersionUpdateInfo
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 模版流水线版本关联服务类
 */
@Service
class PipelineTemplatePipelineVersionService @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineTemplatePipelineVersionDao: PipelineTemplatePipelineVersionDao,
    private val templatePipelineDao: TemplatePipelineDao,
    private val pipelineRepositoryService: PipelineRepositoryService
) {

    /**
     * 创建一条新的版本记录
     * @param transactionContext 事务上下文，可选
     * @param record Pojo对象
     */
    fun createOrUpdate(
        transactionContext: DSLContext? = null,
        record: PTemplatePipelineVersion
    ) {
        pipelineTemplatePipelineVersionDao.createOrUpdate(
            dslContext = transactionContext ?: dslContext,
            record = record
        )
    }

    /**
     * 根据动态条件获取单条记录
     * @param condition 查询条件
     * @return PTemplatePipelineVersion? 记录或null
     */
    fun get(condition: PTemplatePipelineVersionCommonCondition): PTemplatePipelineVersion? {
        return pipelineTemplatePipelineVersionDao.get(
            dslContext = dslContext,
            condition = condition
        )
    }

    /**
     * 根据项目ID、流水线ID和版本号获取记录
     * @param projectId 项目ID
     * @param pipelineId 流水线ID
     * @param pipelineVersion 流水线版本
     * @return PTemplatePipelineVersion? 记录或null
     */
    fun get(projectId: String, pipelineId: String, pipelineVersion: Int): PTemplatePipelineVersion? {
        return get(
            PTemplatePipelineVersionCommonCondition(
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineVersion = pipelineVersion
            )
        ) ?: templatePipelineDao.get(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )?.let { templatePipeline ->
            val pipelineResource = pipelineRepositoryService.getPipelineResourceVersion(
                projectId = projectId,
                pipelineId = pipelineId,
                version = pipelineVersion
            ) ?: return null
            PTemplatePipelineVersion(
                projectId = templatePipeline.projectId,
                pipelineId = templatePipeline.pipelineId,
                pipelineVersion = pipelineVersion,
                pipelineVersionName = pipelineResource.versionName ?: "",
                instanceType = PipelineInstanceTypeEnum.CONSTRAINT,
                buildNo = templatePipeline.buildNo?.takeIf { it.isNotBlank() }?.let {
                    JsonUtil.to(it, object : TypeReference<BuildNo>() {})
                },
                params = templatePipeline.param?.takeIf { it.isNotBlank() }?.let {
                    JsonUtil.to(it, object : TypeReference<List<BuildFormProperty>>() {})
                },
                inputTemplateId = templatePipeline.templateId,
                inputTemplateVersionName = templatePipeline.versionName,
                templateId = templatePipeline.templateId,
                templateVersion = templatePipeline.version,
                templateVersionName = templatePipeline.versionName
            )
        }
    }

    /**
     * 获取指定流水线的最新版本记录
     * @param projectId 项目ID
     * @param pipelineId 流水线ID
     * @return PTemplatePipelineVersion? 最新版本记录或null
     */
    fun getLatestVersion(projectId: String, pipelineId: String): PTemplatePipelineVersion? {
        return pipelineTemplatePipelineVersionDao.getLatestVersion(
            dslContext = dslContext,
            projectId = projectId,
            pipelineId = pipelineId
        )
    }

    /**
     * 根据动态条件统计数量
     * @param condition 查询条件
     * @return 数量
     */
    fun count(condition: PTemplatePipelineVersionCommonCondition): Int {
        return pipelineTemplatePipelineVersionDao.count(
            dslContext = dslContext,
            condition = condition
        )
    }

    /**
     * 根据动态条件删除记录
     * @param transactionContext 事务上下文，可选
     * @param condition 删除条件
     */
    fun delete(
        transactionContext: DSLContext? = null,
        condition: PTemplatePipelineVersionCommonCondition
    ) {
        pipelineTemplatePipelineVersionDao.delete(
            dslContext = transactionContext ?: dslContext,
            condition = condition
        )
    }

    /**
     * 根据动态条件查询记录列表
     * @param condition 查询条件
     * @return 记录列表
     */
    fun list(condition: PTemplatePipelineVersionCommonCondition): List<PTemplatePipelineVersion> {
        return pipelineTemplatePipelineVersionDao.list(
            dslContext = dslContext,
            condition = condition
        )
    }

    /**
     * 根据动态条件更新记录
     * @param transactionContext 事务上下文，可选
     * @param updateInfo 需要更新的字段信息
     * @param condition 更新条件
     */
    fun update(
        transactionContext: DSLContext? = null,
        updateInfo: PTemplatePipelineVersionUpdateInfo,
        condition: PTemplatePipelineVersionCommonCondition
    ) {
        pipelineTemplatePipelineVersionDao.update(
            dslContext = transactionContext ?: dslContext,
            updateInfo = updateInfo,
            condition = condition
        )
    }

    /**
     * 便捷方法：更新指定版本记录的状态
     * @param transactionContext 事务上下文，可选
     * @param projectId 项目ID
     * @param pipelineId 流水线ID
     * @param pipelineVersion 流水线版本
     * @param updater 更新人
     */
    fun updateStatus(
        transactionContext: DSLContext? = null,
        projectId: String,
        pipelineId: String,
        pipelineVersion: Int,
        updater: String
    ) {
        update(
            transactionContext = transactionContext,
            updateInfo = PTemplatePipelineVersionUpdateInfo(
                updater = updater,
                buildNo = null,
                params = null
            ),
            condition = PTemplatePipelineVersionCommonCondition(
                projectId = projectId,
                pipelineId = pipelineId,
                pipelineVersion = pipelineVersion
            )
        )
    }
}
