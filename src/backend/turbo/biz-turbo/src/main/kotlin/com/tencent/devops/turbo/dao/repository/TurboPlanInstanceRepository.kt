package com.tencent.devops.turbo.dao.repository

import com.tencent.devops.turbo.model.TTurboPlanInstanceEntity
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Suppress("MaxLineLength")
@Repository
interface TurboPlanInstanceRepository : MongoRepository<TTurboPlanInstanceEntity, String> {

    /**
     * 根据编译加速方案id和客户端ip查询
     */
    fun findFirstByTurboPlanIdAndClientIp(turboPlanId: String, clientIp: String): TTurboPlanInstanceEntity?

    /**
     * 根据编译加速方案id，流水线id和流水线元素id查询
     */
    fun findByTurboPlanIdAndPipelineIdAndPipelineElementId(turboPlanId: String, pipelineId: String, pipelineElementId: String): TTurboPlanInstanceEntity?

    /**
     * 通过项目id查找
     */
    fun findByProjectId(projectId: String): List<TTurboPlanInstanceEntity>

    /**
     * 通过编译加速方案id查找
     */
    fun findByTurboPlanId(turboPlanId: String): List<TTurboPlanInstanceEntity>

    /**
     * 通过项目id和流水线信息查询
     */
    fun findByProjectIdAndPipelineIdAndPipelineElementId(projectId: String, pipelineId: String, pipelineElementId: String): TTurboPlanInstanceEntity?
}
