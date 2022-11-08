package com.tencent.devops.turbo.dao.repository

import com.tencent.devops.turbo.model.TTurboRecordEntity
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface TurboRecordRepository : MongoRepository<TTurboRecordEntity, String> {

    /**
     * 通过tbs记录id查询
     */
    fun existsByTbsRecordId(tbsRecordId: String): Boolean

    /**
     * 根据tbs记录id查询
     */
    fun findByTbsRecordId(tbsRecordId: String): TTurboRecordEntity

    /**
     * 查找状态不为特定值的记录
     */
    fun findByEngineCodeAndStatusNotIn(engineCode: String, status: Set<String>): List<TTurboRecordEntity>

    /**
     * 通过项目id查找
     */
    fun findByProjectId(projectId: String): List<TTurboRecordEntity>

    /**
     * 通过构建id记录查询
     */
    fun existsByBuildId(buildId: String): Boolean

    /**
     * 通过构建id查找
     */
    fun findByBuildId(buildId: String): TTurboRecordEntity?
}
