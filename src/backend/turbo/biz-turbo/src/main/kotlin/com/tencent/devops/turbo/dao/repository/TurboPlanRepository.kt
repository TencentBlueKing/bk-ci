package com.tencent.devops.turbo.dao.repository

import com.tencent.devops.turbo.model.TTurboPlanEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Suppress("MaxLineLength")
@Repository
interface TurboPlanRepository : MongoRepository<TTurboPlanEntity, String> {

    /**
     * 通过项目id查找
     */
    fun findByProjectId(projectId: String): List<TTurboPlanEntity>?

    /**
     * 根据项目id和状态查找
     */
    fun findByProjectIdAndOpenStatusAndMigratedIn(projectId: String, openStatus: Boolean, migratedList: List<Boolean?>, pageable: Pageable): Page<TTurboPlanEntity>?

    /**
     * 根据项目id和编译加速方案名称判断是否存在
     */
    fun existsByProjectIdAndPlanName(projectId: String, planName: String): Boolean

    /**
     * 根据项目id和编译加速方案名称和id判断是否存在
     */
    fun existsByProjectIdAndPlanNameAndIdNot(projectId: String, planName: String, planId: String): Boolean
}
