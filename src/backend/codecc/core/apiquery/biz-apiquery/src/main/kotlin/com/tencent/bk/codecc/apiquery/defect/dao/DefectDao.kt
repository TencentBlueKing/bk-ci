package com.tencent.bk.codecc.apiquery.defect.dao

import com.google.common.collect.Lists
import com.mongodb.BasicDBObject
import com.tencent.bk.codecc.apiquery.defect.model.CCNDefectModel
import com.tencent.bk.codecc.apiquery.defect.model.CLOCDefectModel
import com.tencent.bk.codecc.apiquery.defect.model.DUPCDefectModel
import com.tencent.bk.codecc.apiquery.defect.model.DefectModel
import com.tencent.bk.codecc.apiquery.defect.model.DefectStatModel
import com.tencent.bk.codecc.apiquery.defect.model.LintDefectV2Model
import com.tencent.bk.codecc.apiquery.utils.PageUtils
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOptions
import org.springframework.data.mongodb.core.query.BasicQuery
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Repository

@Repository
class DefectDao @Autowired constructor(
    private val defectMongoTemplate: MongoTemplate
) {

    /**
     * 通过任务id清单查询圈复杂度告警信息
     */
    fun findCommonByTaskIdInAndToolName(
        taskIds: List<Long>,
        toolName: String?,
        filterFields: List<String>?,
        status: List<Int>?,
        checker: List<String>?,
        pageable: Pageable?
    ): List<DefectModel> {
        val fieldsObj = BasicDBObject()
        PageUtils.getFilterFields(filterFields, fieldsObj)
        val query = BasicQuery(BasicDBObject(), fieldsObj)
        query.addCriteria(Criteria.where("task_id").`in`(taskIds))
        if (!toolName.isNullOrBlank()) {
            query.addCriteria(Criteria.where("tool_name").`is`(toolName))
        }
        if (!status.isNullOrEmpty()) {
            query.addCriteria(Criteria.where("status").`in`(status))
        }
        if (!checker.isNullOrEmpty()) {
            query.addCriteria(Criteria.where("checker_name").`in`(checker))
        }
        if (null != pageable) {
            query.with(pageable)
        }
        return defectMongoTemplate.find(query, DefectModel::class.java, "t_defect")
    }

    /**
     * 通过任务id清单查询lint类告警信息
     */
    fun findLintByTaskIdInAndToolName(
        taskIds: List<Long>,
        toolName: String?,
        filterFields: List<String>?,
        status: List<Int>?,
        checker: List<String>?,
        notChecker : String?,
        pageable: Pageable?
    ): List<LintDefectV2Model> {
        val fieldsObj = BasicDBObject()
        PageUtils.getFilterFields(filterFields, fieldsObj)
        val query = BasicQuery(BasicDBObject(), fieldsObj)
        var criteria = Criteria.where("task_id").`in`(taskIds)
        if (!toolName.isNullOrBlank()) {
            criteria = criteria.and("tool_name").`is`(toolName)
        }
        if (!status.isNullOrEmpty()) {
            criteria = criteria.and("status").`in`(status)
        }
        if (!checker.isNullOrEmpty()) {
            criteria = criteria.and("checker").`in`(checker)
        }
        if(!notChecker.isNullOrBlank()){
            criteria = criteria.and("checker").ne(notChecker)
        }
        query.addCriteria(criteria)
        if(null != pageable){
            query.with(pageable)
        }
        return defectMongoTemplate.find(query, LintDefectV2Model::class.java, "t_lint_defect_v2")
    }

    /**
     * 通过任务id清单查询圈复杂度告警信息
     */
    fun findCCNByTaskIdInAndToolName(
        taskIds: List<Long>,
        filterFields: List<String>?,
        status: List<Int>?,
        pageable: Pageable?
    ): List<CCNDefectModel> {
        val fieldsObj = BasicDBObject()
        PageUtils.getFilterFields(filterFields, fieldsObj)
        val query = BasicQuery(BasicDBObject(), fieldsObj)
        query.addCriteria(Criteria.where("task_id").`in`(taskIds))
        if (!status.isNullOrEmpty()) {
            query.addCriteria(Criteria.where("status").`in`(status))
        }
        if (null != pageable) {
            query.with(pageable)
        }
        return defectMongoTemplate.find(query, CCNDefectModel::class.java, "t_ccn_defect")
    }

    /**
     * 通过任务id清单查询重复率告警信息
     */
    fun findDUPCByTaskIdInAndToolName(
        taskIds: List<Long>,
        filterFields: List<String>?,
        status: List<Int>?,
        pageable: Pageable?
    ): List<DUPCDefectModel> {
        val fieldsObj = BasicDBObject()
        PageUtils.getFilterFields(filterFields, fieldsObj)
        val query = BasicQuery(BasicDBObject(), fieldsObj)
        query.addCriteria(Criteria.where("task_id").`in`(taskIds))
        if (!status.isNullOrEmpty()) {
            query.addCriteria(Criteria.where("status").`in`(status))
        }
        if (null != pageable) {
            query.with(pageable)
        }
        return defectMongoTemplate.find(query, DUPCDefectModel::class.java, "t_dupc_defect")
    }

    /**
     * 通过任务id清单查询CLOC告警信息
     */
    fun findCLOCByTaskIdInAndToolName(
        taskIds: List<Long>,
        filterFields: List<String>?,
        pageable: Pageable?
    ): List<CLOCDefectModel> {
        val fieldsObj = BasicDBObject()
        PageUtils.getFilterFields(filterFields, fieldsObj)
        val query = BasicQuery(BasicDBObject(), fieldsObj)
        query.addCriteria(Criteria.where("task_id").`in`(taskIds).and("status").`is`("ENABLED"))
        if (null != pageable) {
            query.with(pageable)
        }
        return defectMongoTemplate.find(query, CLOCDefectModel::class.java, "t_cloc_defect")
    }

    /**
     * 汇总统计告警
     */
    fun findDefectByTaskIdInAndToolName(taskIds: Collection<Long>, toolName: String): List<DefectStatModel> {
        val match = Aggregation.match(Criteria.where("task_id").`in`(taskIds).and("tool_name").`is`(toolName).and
        ("status").`in`(Lists.newArrayList(1, 3)))

        // 分组统计
        val group = Aggregation.group("task_id", "status", "severity")
                .first("task_id").`as`("task_id")
                .first("status").`as`("status")
                .first("severity").`as`("severity")
                .count().`as`("count")

        // 允许磁盘操作(支持较大数据集合的处理)
        val options = AggregationOptions.Builder().allowDiskUse(true).build()
        val aggregation = Aggregation.newAggregation(match, group).withOptions(options)
        val queryResults = defectMongoTemplate.aggregate(aggregation, "t_defect", DefectStatModel::class.java)

        return queryResults.mappedResults
    }


    fun findCommonByIds(taskId: Long, toolName: String, idList: List<String>, status: List<Int>?, pageable: Pageable?): List<DefectModel> {
        val query = BasicQuery(BasicDBObject())
        query.addCriteria(
                Criteria.where("task_id").`is`(taskId)
                        .and("tool_name").`is`(toolName)
                        .and("id").`in`(idList))

        if (!status.isNullOrEmpty()) {
            query.addCriteria(Criteria.where("status").`in`(status))
        }
        if (null != pageable) {
            query.with(pageable)
        }

        return defectMongoTemplate.find(query, DefectModel::class.java, "t_defect")
    }

    fun findLintByIds(idList: List<ObjectId>, status: List<Int>?, pageable: Pageable?): List<LintDefectV2Model> {
        val query = BasicQuery(BasicDBObject())
        query.addCriteria(Criteria.where("_id").`in`(idList))

        if (!status.isNullOrEmpty()) {
            query.addCriteria(Criteria.where("status").`in`(status))
        }
        if (null != pageable) {
            query.with(pageable)
        }

        return defectMongoTemplate.find(query, LintDefectV2Model::class.java, "t_lint_defect_v2")
    }

    fun findCCNByIds(idList: List<ObjectId>, status: List<Int>?, pageable: Pageable?): List<CCNDefectModel> {
        val query = BasicQuery(BasicDBObject())
        query.addCriteria(Criteria.where("_id").`in`(idList))

        if (!status.isNullOrEmpty()) {
            query.addCriteria(Criteria.where("status").`in`(status))
        }
        if (null != pageable) {
            query.with(pageable)
        }

        return defectMongoTemplate.find(query, CCNDefectModel::class.java, "t_ccn_defect")
    }

}