package com.tencent.bk.codecc.apiquery.defect.dao

import com.mongodb.BasicDBObject
import com.tencent.bk.codecc.apiquery.defect.model.CCNStatisticModel
import com.tencent.bk.codecc.apiquery.defect.model.CLOCStatisticModel
import com.tencent.bk.codecc.apiquery.defect.model.CommonStatisticModel
import com.tencent.bk.codecc.apiquery.defect.model.DUPCStatisticModel
import com.tencent.bk.codecc.apiquery.defect.model.LintStatisticModel
import com.tencent.bk.codecc.apiquery.utils.PageUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.BasicQuery
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository

@Repository
class StatisticDao @Autowired constructor(
    private val defectMongoTemplate: MongoTemplate
) {

    /**
     * 通过任务id清单查询缺陷类告警统计信息
     */
    fun findCommonByTaskIdInAndToolName(
        taskIds: List<Long>,
        toolName: String?,
        startTime: Long?,
        endTime: Long?,
        filterFields: List<String>?,
        pageable: Pageable?
    ): List<CommonStatisticModel> {
        val fieldsObj = BasicDBObject()
        PageUtils.getFilterFields(filterFields, fieldsObj)
        val query = BasicQuery(BasicDBObject().toJson(), fieldsObj.toJson())
        val criteria = if (toolName.isNullOrBlank()) {
            Criteria.where("task_id").`in`(taskIds)
        } else {
            Criteria.where("task_id").`in`(taskIds).and("tool_name").`is`(toolName)
        }
        query.addCriteria(criteria)
        if (null != startTime && null != endTime) {
            query.addCriteria(Criteria.where("time").gte(startTime).lt(endTime))
        } else if (null != startTime) {
            query.addCriteria(Criteria.where("time").gte(startTime))
        } else if (null != endTime) {
            query.addCriteria(Criteria.where("time").lt(endTime))
        }
        if (null != pageable) {
            query.with(pageable)
        }
        return defectMongoTemplate.find(query, CommonStatisticModel::class.java, "t_statistic")
    }

    /**
     * 通过任务id清单查询lint类告警统计信息
     */
    fun findLintByTaskIdInAndToolName(
        taskIds: List<Long>,
        toolName: String?,
        startTime: Long?,
        endTime: Long?,
        filterFields: List<String>?,
        buildId: String?,
        pageable: Pageable?
    ): List<LintStatisticModel> {
        val fieldsObj = BasicDBObject()
        PageUtils.getFilterFields(filterFields, fieldsObj)
        val query = BasicQuery(BasicDBObject().toJson(), fieldsObj.toJson())
        val criteria = if (toolName.isNullOrBlank()) {
            Criteria.where("task_id").`in`(taskIds)
        } else {
            Criteria.where("task_id").`in`(taskIds).and("tool_name").`is`(toolName)
        }
        query.addCriteria(criteria)
        if (null != startTime && null != endTime) {
            query.addCriteria(Criteria.where("time").gte(startTime).lt(endTime))
        } else if (null != startTime) {
            query.addCriteria(Criteria.where("time").gte(startTime))
        } else if (null != endTime) {
            query.addCriteria(Criteria.where("time").lt(endTime))
        }
        if (null != buildId) {
            query.addCriteria(Criteria.where("build_id").`is`(buildId))
        }
        if (null != pageable) {
            query.with(pageable)
        }
        return defectMongoTemplate.find(query, LintStatisticModel::class.java, "t_lint_statistic")
    }

    /**
     * 通过任务id清单查询圈复杂度告警统计信息
     */
    fun findCCNByTaskIdInAndToolName(
        taskIds: List<Long>,
        startTime: Long?,
        endTime: Long?,
        filterFields: List<String>?,
        buildId: String?,
        pageable: Pageable?
    ): List<CCNStatisticModel> {
        val fieldsObj = BasicDBObject()
        PageUtils.getFilterFields(filterFields, fieldsObj)
        val query = BasicQuery(BasicDBObject().toJson(), fieldsObj.toJson())
        query.addCriteria(Criteria.where("task_id").`in`(taskIds))
        if (null != startTime && null != endTime) {
            query.addCriteria(Criteria.where("time").gte(startTime).lt(endTime))
        } else if (null != startTime) {
            query.addCriteria(Criteria.where("time").gte(startTime))
        } else if (null != endTime) {
            query.addCriteria(Criteria.where("time").lt(endTime))
        }
        if (null != buildId) {
            query.addCriteria(Criteria.where("build_id").`is`(buildId))
        }
        if (null != pageable) {
            query.with(pageable)
        }
        return defectMongoTemplate.find(query, CCNStatisticModel::class.java, "t_ccn_statistic")
    }

    /**
     * 通过任务id清单查询重复率告警统计信息
     */
    fun findDUPCByTaskIdInAndToolName(
        taskIds: List<Long>,
        startTime: Long?,
        endTime: Long?,
        filterFields: List<String>?,
        pageable: Pageable?
    ): List<DUPCStatisticModel> {
        val fieldsObj = BasicDBObject()
        PageUtils.getFilterFields(filterFields, fieldsObj)
        val query = BasicQuery(BasicDBObject().toJson(), fieldsObj.toJson())
        query.addCriteria(Criteria.where("task_id").`in`(taskIds))
        if (null != startTime && null != endTime) {
            query.addCriteria(Criteria.where("time").gte(startTime).lt(endTime))
        } else if (null != startTime) {
            query.addCriteria(Criteria.where("time").gte(startTime))
        } else if (null != endTime) {
            query.addCriteria(Criteria.where("time").lt(endTime))
        }
        if (null != pageable) {
            query.with(pageable)
        }
        return defectMongoTemplate.find(query, DUPCStatisticModel::class.java, "t_dupc_statistic")
    }

    /**
     * 通过任务id清单查询CLOC告警统计信息
     */
    fun findCLOCByTaskIdInAndToolName(
        taskIds: List<Long>,
        toolName: String,
        startTime: Long?,
        endTime: Long?,
        filterFields: List<String>?,
        buildId: String?,
        pageable: Pageable?
    ): List<CLOCStatisticModel> {
        val fieldsObj = BasicDBObject()
        PageUtils.getFilterFields(filterFields, fieldsObj)
        val query = BasicQuery(BasicDBObject().toJson(), fieldsObj.toJson())
        query.addCriteria(Criteria.where("task_id").`in`(taskIds)
                .and("tool_name").`is`(toolName))
        if (null != startTime && null != endTime) {
            query.addCriteria(Criteria.where("updated_date").gte(startTime).lt(endTime))
        } else if (null != startTime) {
            query.addCriteria(Criteria.where("updated_date").gte(startTime))
        } else if (null != endTime) {
            query.addCriteria(Criteria.where("updated_date").lt(endTime))
        }
        if (null != buildId) {
            query.addCriteria(Criteria.where("build_id").`is`(buildId))
        }
        if (null != pageable) {
            query.with(pageable)
        }
        return defectMongoTemplate.find(query, CLOCStatisticModel::class.java, "t_cloc_statistic")
    }

    fun findFirstByTaskIdAndToolNameOrderByTimeDesc(taskId: Long, toolName: String): LintStatisticModel {
        val query = Query()
        query.addCriteria(Criteria.where("task_id").`is`(taskId).and("tool_name").`is`(toolName))
        query.with(Sort.by(Sort.Direction.DESC, "time"))
        val statisticModels = defectMongoTemplate.find(query, LintStatisticModel::class.java, "t_lint_statistic")

        return if (statisticModels.isEmpty()) LintStatisticModel() else statisticModels.first()
    }
}
