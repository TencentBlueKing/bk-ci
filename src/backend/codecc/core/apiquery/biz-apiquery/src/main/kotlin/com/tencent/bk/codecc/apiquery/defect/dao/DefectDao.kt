package com.tencent.bk.codecc.apiquery.defect.dao

import com.google.common.collect.Lists
import com.mongodb.BasicDBObject
import com.tencent.bk.codecc.apiquery.defect.model.*
import com.tencent.bk.codecc.apiquery.utils.PageUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOptions
import org.springframework.data.mongodb.core.aggregation.MatchOperation
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
    ): List<LintDefectModel> {
        var criteria = Criteria.where("task_id").`in`(taskIds)
        if (!toolName.isNullOrBlank()) {
            criteria = criteria.and("tool_name").`is`(toolName)
        }
        if (!status.isNullOrEmpty()) {
            criteria = criteria.and("status").`in`(status)
        }
        if (!checker.isNullOrEmpty()) {
            criteria = criteria.and("checker_list").`in`(checker)
        }
        val match = Aggregation.match(criteria)
        val unwind = Aggregation.unwind("defect_list")
        val project1 = Aggregation.project().
            and("task_id").`as`("task_id").
            and("tool_name").`as`("tool_name").
            and("rel_path").`as`("rel_path").
            and("file_path").`as`("file_path").
            and("repo_id").`as`("repo_id").
            and("revision").`as`("revision").
            and("branch").`as`("branch").
            and("submodule").`as`("submodule").
            and("defect_list.defect_id").`as`("defect_id").
            and("defect_list.line_num").`as`("line_num").
            and("defect_list.author").`as`("author").
            and("defect_list.checker").`as`("checker").
            and("defect_list.severity").`as`("severity").
            and("defect_list.message").`as`("message").
            and("defect_list.defect_type").`as`("defect_type").
            and("defect_list.status").`as`("status").
            and("defect_list.linenum_datetime").`as`("linenum_datetime").
            and("defect_list.fixed_time").`as`("fixed_time").
            and("defect_list.fixed_build_number").`as`("fixed_build_number").
            and("defect_list.fixed_repo_id").`as`("fixed_repo_id").
            and("defect_list.fixed_branch").`as`("fixed_branch").
            and("defect_list.ignore_time").`as`("ignore_time").
            and("defect_list.ignore_author").`as`("ignore_author").
            and("defect_list.ignore_reason").`as`("ignore_reason").
            and("defect_list.ignore_reason_type").`as`("ignore_reason_type").
            and("defect_list.exclude_time").`as`("exclude_time").
            and("defect_list.create_time").`as`("create_time").
            and("defect_list.create_build_number").`as`("createa_build_number").
            and("defect_list.mark").`as`("mark").
            and("defect_list.mark_time").`as`("mark_time")

        val criteriaList = mutableListOf<Criteria>()
        if (!status.isNullOrEmpty()) {
            criteriaList.add(Criteria.where("status").`in`(status))
        }
        if (!checker.isNullOrEmpty()) {
            criteriaList.add(Criteria.where("checker").`in`(checker))
        }
        if (!notChecker.isNullOrBlank()){
            criteriaList.add(Criteria.where("checker").ne(notChecker))
        }
        var matchAfter : MatchOperation? = null
        if(!criteriaList.isNullOrEmpty()){
            matchAfter = Aggregation.match(Criteria().andOperator(*criteriaList.toTypedArray()))
        }



        val sort =
            Aggregation.sort(
                pageable!!.sort
            )
        val skip = Aggregation.skip((pageable.pageNumber * pageable.pageSize).toLong())
        val limit = Aggregation.limit(pageable.pageSize.toLong())
        //允许磁盘操作
        val options = AggregationOptions.Builder().allowDiskUse(true).build()

        return if(null == matchAfter){
            if(!filterFields.isNullOrEmpty()){
                var project2 = Aggregation.project()
                filterFields.forEach {
                    project2 = project2.andInclude(it)
                }
                val agg = Aggregation.newAggregation(match, unwind, project1, project2, sort, skip, limit).withOptions(options)
                defectMongoTemplate.aggregate(agg, "t_lint_defect", LintDefectModel::class.java).mappedResults
            }
            else{
                val agg = Aggregation.newAggregation(match, unwind, project1, sort, skip, limit).withOptions(options)
                defectMongoTemplate.aggregate(agg, "t_lint_defect", LintDefectModel::class.java).mappedResults
            }
        } else {
            if(!filterFields.isNullOrEmpty()){
                var project2 = Aggregation.project()
                filterFields.forEach {
                    project2 = project2.andInclude(it)
                }
                val agg = Aggregation.newAggregation(match, unwind, project1, matchAfter, project2, sort, skip, limit).withOptions(options)
                defectMongoTemplate.aggregate(agg, "t_lint_defect", LintDefectModel::class.java).mappedResults
            }
            else{
                val agg = Aggregation.newAggregation(match, unwind, project1, matchAfter, sort, skip, limit).withOptions(options)
                defectMongoTemplate.aggregate(agg, "t_lint_defect", LintDefectModel::class.java).mappedResults
            }
        }


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

}