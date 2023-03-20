package com.tencent.devops.turbo.dao.mongotemplate

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.util.constants.codeccAdmin
import com.tencent.devops.turbo.model.TTurboPlanEntity
import com.tencent.devops.turbo.pojo.TurboDaySummaryOverviewModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationResults
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Suppress("MaxLineLength")
@Repository
class TurboPlanDao @Autowired constructor(
    private val mongoTemplate: MongoTemplate
) {

    /**
     * 实例数加1
     */
    fun incrInstanceNum(turboPlanId: String) {
        val query = Query()
        query.addCriteria(Criteria.where("_id").`is`(turboPlanId))
        val update = Update()
        update.inc("instance_num", 1)
        mongoTemplate.updateFirst(query, update, TTurboPlanEntity::class.java)
    }

    /**
     * 更新编译加速方案统计信息
     */
    fun updateTurboPlan(
        turboPlanId: String,
        estimateTimeHour: Double?,
        executeTimeHour: Double?,
        createFlag: Boolean
    ) {
        val query = Query()
        query.addCriteria(Criteria.where("_id").`is`(turboPlanId))
        val update = Update()
        if (null != executeTimeHour) {
            update.inc("execute_time", executeTimeHour)
        }
        if (null != estimateTimeHour) {
            update.inc("estimate_time", estimateTimeHour)
        }
        if (createFlag) {
            update.inc("execute_count", 1)
        }
        update.set("updated_by", codeccAdmin)
            .set("updated_date", LocalDateTime.now())
        mongoTemplate.updateFirst(query, update, TTurboPlanEntity::class.java)
    }

    /**
     * 获取实例数量和执行次数
     */
    fun getInstanceNumAndExecuteCount(projectId: String): List<TurboDaySummaryOverviewModel> {

        val match = Aggregation.match(Criteria.where("project_id").`is`(projectId))

        val group = Aggregation.group("project_id")
            .count().`as`("plan_num")
            .sum("execute_count").`as`("execute_count")
            .sum("execute_time").`as`("execute_time")
            .sum("estimate_time").`as`("estimate_time")

        val aggregation = Aggregation.newAggregation(match, group)
        val queryResults: AggregationResults<TurboDaySummaryOverviewModel> = mongoTemplate.aggregate(aggregation, "t_turbo_plan_entity", TurboDaySummaryOverviewModel::class.java)
        return queryResults.mappedResults
    }

    /**
     * 获取 加速方案-列表页 方案清单数据
     */
    fun getTurboPlanStatRowData(projectId: String, pageable: Pageable?): List<TTurboPlanEntity> {
        val query = Query()
        query.addCriteria(Criteria.where("project_id").`is`(projectId))
        if (null != pageable) {
            query.with(pageable)
        }
        return mongoTemplate.find(query, TTurboPlanEntity::class.java, "t_turbo_plan_entity")
    }

    /**
     * 查询项目下编译加速方案总数
     */
    fun getTurboPlanCount(projectId: String): Long {
        val query = Query()
        query.addCriteria(Criteria.where("project_id").`is`(projectId))
        return mongoTemplate.count(query, TTurboPlanEntity::class.java)
    }

    /**
     * 编辑加速方案名称及开启状态
     */
    fun updateTurboPlanDetailNameAndOpenStatus(
        planId: String?,
        planName: String?,
        openStatus: Boolean?,
        desc: String?,
        user: String
    ): TTurboPlanEntity? {
        val query = Query()
        query.addCriteria(Criteria.where("_id").`is`(planId))
        val update = Update()
        if (null != planName) {
            update.set("plan_name", planName)
        }
        if (null != openStatus) {
            update.set("open_status", openStatus)
        }
        update.set("desc", desc)
            .set("updated_by", user)
            .set("updated_date", LocalDateTime.now())
        return mongoTemplate.findAndModify(query, update, TTurboPlanEntity::class.java)
    }

    /**
     * 编辑IP白名单
     */
    fun updateTurboPlanWhiteList(planId: String?, whiteList: String?, user: String) {
        val query = Query()
        query.addCriteria(Criteria.where("_id").`is`(planId))
        val update = Update()
        if (null != whiteList) {
            update.set("white_list", whiteList)
        }
        update.set("updated_by", user)
            .set("updated_date", LocalDateTime.now())
        mongoTemplate.updateFirst(query, update, TTurboPlanEntity::class.java)
    }

    /**
     *编辑配置参数
     */
    fun updateTurboPlanConfigParam(planId: String?, configParam: Map<String, Any>?, user: String): TTurboPlanEntity? {
        val query = Query()
        query.addCriteria(Criteria.where("_id").`is`(planId))
        val update = Update()
        if (null != configParam) {
            update.set("config_param", configParam)
        }
        update.set("updated_by", user)
            .set("updated_date", LocalDateTime.now())
        return mongoTemplate.findAndModify(query, update, TTurboPlanEntity::class.java)
    }

    /**
     * 编辑置顶状态
     */
    fun updateTurboPlanTopStatus(planId: String, topStatus: String, user: String) {
        val query = Query()
        query.addCriteria(Criteria.where("_id").`is`(planId))
        val update = Update()
        update.set("top_status", topStatus)
        update.set("updated_by", user)
            .set("updated_date", LocalDateTime.now())
        mongoTemplate.updateFirst(query, update, TTurboPlanEntity::class.java)
    }

    /**
     * 通过id增加实例和执行个数
     */
    fun incInstanceAndCountById(turboPlanId: String): TTurboPlanEntity? {
        val query = Query()
        query.addCriteria(Criteria.where("_id").`is`(turboPlanId))
        val update = Update()
        update.inc("instance_num", 1)
        update.inc("execute_count", 1)
        val findAndModifyOptions = FindAndModifyOptions()
        findAndModifyOptions.returnNew(true)
        return mongoTemplate.findAndModify(query, update, findAndModifyOptions, TTurboPlanEntity::class.java)
    }

    /**
     * 更新编译加速方案信息
     */
    fun updateTurboPlanConfig(
        turboPlanId: String,
        planName: String,
        desc: String?,
        configParam: Map<String, Any>?,
        openStatus: Boolean?,
        user: String
    ): TTurboPlanEntity? {
        val query = Query()
        query.addCriteria(Criteria.where("_id").`is`(turboPlanId))
        val update = Update()
        update.set("plan_name", planName)
        update.set("desc", desc)
        update.set("config_param", configParam)
        update.set("open_status", openStatus)
        update.set("updated_by", user)
        update.set("updated_date", LocalDateTime.now())
        val findAndModifyOptions = FindAndModifyOptions()
        findAndModifyOptions.returnNew(true)
        return mongoTemplate.findAndModify(query, update, findAndModifyOptions, TTurboPlanEntity::class.java)
    }

    /**
     * 根据条件查询所有编译加速方案列表清单
     */
    fun getAllTurboPlanList(
        turboPlanId: String?,
        planName: String?,
        projectId: String?,
        pageable: Pageable
    ): Page<TTurboPlanEntity> {
        val query = Query()
        if (!turboPlanId.isNullOrBlank()) {
            query.addCriteria(Criteria.where("_id").`is`(turboPlanId))
        }
        if (!planName.isNullOrBlank()) {
            query.addCriteria(Criteria.where("plan_name").regex(planName))
        }
        if (!projectId.isNullOrBlank()) {
            query.addCriteria(Criteria.where("project_id").`is`(projectId))
        }
        val totalCount = mongoTemplate.count(query, "t_turbo_plan_entity")
        query.with(pageable)

        // 计算总页数
        val pageSize = pageable.pageSize
        val pageNumber = pageable.pageNumber
        var totalPageNum = 0
        if (totalCount > 0) {
            totalPageNum = (totalCount.toInt() + pageSize - 1) / pageSize
        }
        val queryResults = mongoTemplate.find(query, TTurboPlanEntity::class.java)
        return Page(totalCount, pageNumber + 1, pageSize, totalPageNum, queryResults)
    }

    /**
     * 更新评估时间
     */
    fun updateEstimateTime(
        turboPlanId: String,
        estimateTime: Long
    ) {
        val query = Query()
        query.addCriteria(Criteria.where("_id").`is`(turboPlanId))
        val update = Update()
        update.set("estimate_time", estimateTime.toDouble().div(3600))
        update.set("updated_by", codeccAdmin)
        update.set("updated_date", LocalDateTime.now())
        mongoTemplate.updateFirst(query, update, TTurboPlanEntity::class.java)
    }

    /**
     * 根据项目id和创建时间获取加速方案列表
     */
    fun getTurboPlanByProjectIdAndCreatedDate(projectId: String, startTime: LocalDate?, endTime: LocalDate?, pageable: Pageable): Page<TTurboPlanEntity> {
        val query = turboPlanParameter(projectId, startTime, endTime)
        // 先算总数
        val totalCount = mongoTemplate.count(query, TTurboPlanEntity::class.java)
        query.with(pageable)
        // 分页排序
        val pageSize = pageable.pageSize
        val pageNumber = pageable.pageNumber
        val queryResults = mongoTemplate.find(query, TTurboPlanEntity::class.java, "t_turbo_plan_entity")

        // 计算总页数
        var totalPageNum = 0
        if (totalCount > 0) {
            totalPageNum = (totalCount.toInt() + pageSize - 1) / pageSize
        }

        return Page(totalCount, pageNumber + 1, pageSize, totalPageNum, queryResults)
    }

    /**
     * 公共方法,获取加速方案列表所需参数
     */
    private fun turboPlanParameter(projectId: String, startTime: LocalDate?, endTime: LocalDate?): Query {
        val query = Query()
        query.addCriteria(Criteria.where("project_id").`is`(projectId))
        if (null != startTime) {
            query.addCriteria(Criteria.where("created_date").gte(startTime))
        }
        if (null != endTime) {
            query.addCriteria(Criteria.where("created_date").lte(endTime))
        }
        return query
    }
}
