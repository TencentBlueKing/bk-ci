package com.tencent.devops.turbo.dao.repository

import com.tencent.devops.turbo.model.TTurboEngineConfigEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface TurboEngineConfigRepository : MongoRepository<TTurboEngineConfigEntity, String> {

    /**
     * 根据模板代码查找
     */
    fun findByEngineCode(engineCode: String): TTurboEngineConfigEntity?

    /**
     * 通过状态查询
     */
    fun findByEnabled(enabled: Boolean, pageable: Pageable): List<TTurboEngineConfigEntity>

    /**
     * 通过模板代码删除
     */
    fun removeByEngineCode(engineCode: String)

    /**
     * 通过是否推荐查询
     */
    fun findByRecommendAndEnabled(recommend: Boolean, enabled: Boolean): List<TTurboEngineConfigEntity>?
}
