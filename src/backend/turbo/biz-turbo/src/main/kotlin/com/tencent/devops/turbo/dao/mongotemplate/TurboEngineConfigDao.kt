package com.tencent.devops.turbo.dao.mongotemplate

import com.mongodb.bulk.BulkWriteResult
import com.tencent.devops.turbo.model.TTurboEngineConfigEntity
import com.tencent.devops.turbo.model.pojo.DisplayFieldEntity
import com.tencent.devops.turbo.model.pojo.ParamConfigEntity
import com.tencent.devops.turbo.pojo.TurboEngineConfigPriorityModel
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.BulkOperations
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class TurboEngineConfigDao @Autowired constructor(
    private val mongoTemplate: MongoTemplate
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TurboEngineConfigDao::class.java)
    }

    /**
     * 更新加速模式状态
     */
    fun updateConfigStatus(engineCode: String, enabled: Boolean, user: String) {
        val query = Query()
        query.addCriteria(Criteria.where("engine_code").`is`(engineCode))
        val update = Update()
        update.set("enabled", enabled)
            .set("updated_by", user)
            .set("updated_date", LocalDateTime.now())
        mongoTemplate.updateFirst(query, update, TTurboEngineConfigEntity::class.java)
    }

    /**
     * 更新加速模式信息
     */
    fun updateConfigInfo(
        engineCode: String,
        engineName: String,
        desc: String?,
        spelExpression: String,
        spelParamMap: Map<String, Any?>?,
        paramConfig: List<ParamConfigEntity>?,
        userManual: String?,
        docUrl: String?,
        displayFields: List<DisplayFieldEntity>,
        recommend: Boolean?,
        recommendReason: String?,
        pluginTips: String?,
        user: String
    ) {
        val query = Query()
        query.addCriteria(Criteria.where("engine_code").`is`(engineCode))
        val update = Update()
        update.set("engine_name", engineName)
            .set("desc", desc)
            .set("spel_expression", spelExpression)
            .set("spel_param_list", spelParamMap)
            .set("param_config", paramConfig)
            .set("user_manual", userManual)
            .set("doc_url", docUrl)
            .set("display_fields", displayFields)
            .set("recommend", recommend)
            .set("recommend_reason", recommendReason)
            .set("plugin_tips", pluginTips)
            .set("updated_date", LocalDateTime.now())
            .set("updated_by", user)
        mongoTemplate.updateFirst(query, update, TTurboEngineConfigEntity::class.java)
    }

    /**
     * 更新排名优先级
     */
    fun batchUpdatePriorityNum(turboPriorityList: List<TurboEngineConfigPriorityModel>, user: String): BulkWriteResult {
        val bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, TTurboEngineConfigEntity::class.java)
        turboPriorityList.forEach {
            logger.info("engine code: ${it.engineCode}, priority: ${it.priorityNum}")
            val query = Query()
            query.addCriteria(Criteria.where("engine_code").`is`(it.engineCode))
            val update = Update()
            update.set("priority_num", it.priorityNum)
                .set("updated_date", LocalDateTime.now())
                .set("updated_by", user)
            bulkOps.updateOne(query, update)
        }
        return bulkOps.execute()
    }

    /**
     * 查询优先级序号最大值
     */
    fun findEngineConfigWithMaxPriorityNum(): TTurboEngineConfigEntity? {
        val query = Query()
        query.with(Sort.by(Sort.Direction.DESC, "priority_num"))
        query.limit(1)
        return mongoTemplate.findOne(query, TTurboEngineConfigEntity::class.java)
    }
}
