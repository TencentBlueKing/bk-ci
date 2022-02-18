package com.tencent.devops.turbo.dao.mongotemplate

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.util.constants.codeccAdmin
import com.tencent.devops.turbo.model.TTurboPlanInstanceEntity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class TurboPlanInstanceDao @Autowired constructor(
    private val mongoTemplate: MongoTemplate
) {

    /**
     * 更新编译加速实例表的统计信息
     */
    fun updateTurboPlanInstance(
        turboPlanInstanceId: String,
        totalEstimateTime: Long?,
        totalExecuteTime: Long?,
        startTime: LocalDateTime,
        status: String,
        createFlag: Boolean
    ) {
        val query = Query()
        query.addCriteria(Criteria.where("_id").`is`(turboPlanInstanceId))
        val update = Update()
        if (null != totalEstimateTime) {
            update.inc("total_estimate_time", totalEstimateTime)
        }
        if (null != totalExecuteTime) {
            update.inc("total_execute_time", totalExecuteTime)
        }
        if (createFlag) {
            update.inc("execute_count", 1)
        }
        update.set("latest_start_time", startTime)
            .set("latest_status", status)
            .set("updated_by", codeccAdmin)
            .set("updated_date", LocalDateTime.now())
        mongoTemplate.updateFirst(query, update, TTurboPlanInstanceEntity::class.java)
    }

    /**
     * 获取 加速方案-列表页 表格数据
     */
    fun getTurboPlanInstanceList(turboPlanId: String, pageable: Pageable): Page<TTurboPlanInstanceEntity> {

        val criteria = Criteria.where("turbo_plan_id").`is`(turboPlanId)

        val totalCount = mongoTemplate.count(Query(criteria), "t_turbo_plan_instance_entity")

        // 分页排序
        val query = Query()
        query.addCriteria(criteria)
        query.with(pageable)
        val queryResults = mongoTemplate.find(query, TTurboPlanInstanceEntity::class.java)

        val pageSize = pageable.pageSize
        val pageNumber = pageable.pageNumber

        // 计算总页数
        var totalPageNum = 0
        if (totalCount > 0) {
            totalPageNum = (totalCount.toInt() + pageSize - 1) / pageSize
        }

        return Page<TTurboPlanInstanceEntity>(totalCount, pageNumber + 1, pageSize, totalPageNum, queryResults)
    }

    /**
     * 只更新预计时间
     */
    fun updateEstimateTime(
        turboPlanInstanceId: String,
        estimateTime: Long
    ) {
        val query = Query()
        query.addCriteria(Criteria.where("_id").`is`(turboPlanInstanceId))
        val update = Update()
        update.set("total_estimate_time", estimateTime)
        update.set("updated_by", codeccAdmin)
        update.set("updated_date", LocalDateTime.now())
        mongoTemplate.updateFirst(query, update, TTurboPlanInstanceEntity::class.java)
    }
}
