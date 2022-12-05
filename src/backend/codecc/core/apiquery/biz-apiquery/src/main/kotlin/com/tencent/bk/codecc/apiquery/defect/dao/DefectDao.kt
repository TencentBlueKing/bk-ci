package com.tencent.bk.codecc.apiquery.defect.dao

import com.google.common.collect.Lists
import com.mongodb.BasicDBObject
import com.tencent.bk.codecc.apiquery.defect.model.CCNDefectModel
import com.tencent.bk.codecc.apiquery.defect.model.CLOCDefectModel
import com.tencent.bk.codecc.apiquery.defect.model.DUPCDefectModel
import com.tencent.bk.codecc.apiquery.defect.model.DefectModel
import com.tencent.bk.codecc.apiquery.defect.model.DefectStatModel
import com.tencent.bk.codecc.apiquery.defect.model.LintDefectV2Model
import com.tencent.bk.codecc.apiquery.utils.ConvertUtil
import com.tencent.bk.codecc.apiquery.utils.PageUtils
import com.tencent.devops.common.constant.ComConstants
import org.apache.commons.collections.CollectionUtils
import org.bson.Document
import org.bson.types.ObjectId
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOptions
import org.springframework.data.mongodb.core.aggregation.MatchOperation
import org.springframework.data.mongodb.core.query.BasicQuery
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
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
        defectIds: List<String>?,
        pageNum: Int?,
        pageSize: Int?,
        sortField : String?,
        sortType : String?
    ): List<DefectModel> {
        val criteriaList = mutableListOf<Criteria>()
        criteriaList.add(Criteria.where("task_id").`in`(taskIds))
        if (!toolName.isNullOrBlank()) {
            criteriaList.add(Criteria.where("tool_name").`is`(toolName))
        }
        if (!status.isNullOrEmpty()) {
            criteriaList.add(Criteria.where("status").`in`(status))
        }
        if (!checker.isNullOrEmpty()) {
            criteriaList.add(Criteria.where("checker").`in`(checker))
        }
        if (!defectIds.isNullOrEmpty()) {
            criteriaList.add(Criteria.where("_id").`in`(defectIds))
        }
        val match = Aggregation.match(Criteria().andOperator(*criteriaList.toTypedArray()))
        val project = if(!filterFields.isNullOrEmpty()) {
            Aggregation.project(*filterFields.toTypedArray())
        } else {
            null
        }

        val finalPageSize = if (pageSize == null) 100 else if (pageSize >= 10000) 10000 else pageSize
        val skip = if(defectIds.isNullOrEmpty()) {
            Aggregation.skip((((pageNum ?: 1) - 1) * finalPageSize).toLong())
        } else {
            null
        }
        val limit = if(defectIds.isNullOrEmpty()){
            Aggregation.limit(finalPageSize.toLong())
        } else {
            null
        }
        //允许磁盘操作
        val options = AggregationOptions.Builder().allowDiskUse(true).build()
        val agg = if(defectIds.isNullOrEmpty()){
            val sort = Aggregation.sort(Sort.by(Sort.Direction.fromString(sortType?:"DESC"), sortField?:"task_id"))
            if(null != project) {
                Aggregation.newAggregation(match, sort, project, skip, limit).withOptions(options)
            } else {
                Aggregation.newAggregation(match, sort, skip, limit).withOptions(options)
            }
        } else {
            if(null != project) {
                Aggregation.newAggregation(match, project).withOptions(options)
            } else {
                Aggregation.newAggregation(match).withOptions(options)
            }
        }
        return defectMongoTemplate.aggregate(agg, "t_defect", DefectModel::class.java).mappedResults
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
        notChecker: String?,
        defectIds: List<ObjectId>?,
        pageNum: Int?,
        pageSize: Int?,
        sortField : String?,
        sortType : String?
    ): List<LintDefectV2Model> {
        val criteriaList = mutableListOf<Criteria>()
        criteriaList.add(Criteria.where("task_id").`in`(taskIds))
        if (!toolName.isNullOrBlank()) {
            criteriaList.add(Criteria.where("tool_name").`is`(toolName))
        }
        if (!status.isNullOrEmpty()) {
            criteriaList.add(Criteria.where("status").`in`(status))
        }
        if (!checker.isNullOrEmpty()) {
            criteriaList.add(Criteria.where("checker").`in`(checker))
        }
        if (!notChecker.isNullOrBlank()) {
            criteriaList.add(Criteria.where("checker").ne(notChecker))
        }
        if (!defectIds.isNullOrEmpty()) {
            criteriaList.add(Criteria.where("_id").`in`(defectIds))
        }
        val agg = getAggragation(
            criteriaList, filterFields, defectIds, pageNum, pageSize, sortField, sortType
        )
        return defectMongoTemplate.aggregate(agg, "t_lint_defect_v2", LintDefectV2Model::class.java).mappedResults
    }

    /**
     * 通过任务id清单查询圈复杂度告警信息
     */
    fun findCCNByTaskIdInAndToolName(
        taskIds: List<Long>,
        filterFields: List<String>?,
        status: List<Int>?,
        defectIds: List<ObjectId>?,
        pageNum: Int?,
        pageSize: Int?,
        sortField : String?,
        sortType : String?
    ): List<CCNDefectModel> {
        val criteriaList = mutableListOf<Criteria>()
        criteriaList.add(Criteria.where("task_id").`in`(taskIds))
        if (!status.isNullOrEmpty()) {
            criteriaList.add(Criteria.where("status").`in`(status))
        }
        if (!defectIds.isNullOrEmpty()) {
            criteriaList.add(Criteria.where("_id").`in`(defectIds))
        }
        val agg = getAggragation(
            criteriaList, filterFields, defectIds, pageNum, pageSize, sortField, sortType
        )
        return defectMongoTemplate.aggregate(agg, "t_ccn_defect", CCNDefectModel::class.java).mappedResults
    }


    private fun getAggragation(criteriaList : List<Criteria>,
        filterFields : List<String>?,
        defectIds: List<ObjectId>?,
        pageNum: Int?,
        pageSize: Int?,
        sortField : String?,
        sortType : String?) : Aggregation {
        val match = Aggregation.match(Criteria().andOperator(*criteriaList.toTypedArray()))
        val project = if(!filterFields.isNullOrEmpty()) {
            Aggregation.project(*filterFields.toTypedArray())
        } else {
            null
        }

        val finalPageSize = if (pageSize == null) 100 else if (pageSize >= 10000) 10000 else pageSize
        val skip = if(defectIds.isNullOrEmpty()) {
            Aggregation.skip((((pageNum ?: 1) - 1) * finalPageSize).toLong())
        } else {
            null
        }
        val limit = if(defectIds.isNullOrEmpty()){
            Aggregation.limit(finalPageSize.toLong())
        } else {
            null
        }
        //允许磁盘操作
        val options = AggregationOptions.Builder().allowDiskUse(true).build()
        return if(defectIds.isNullOrEmpty()){
            val sort = Aggregation.sort(Sort.by(Sort.Direction.fromString(sortType?:"DESC"), sortField?:"task_id"))
            if(null != project) {
                Aggregation.newAggregation(match, sort, project, skip, limit).withOptions(options)
            } else {
                Aggregation.newAggregation(match, sort, skip, limit).withOptions(options)
            }
        } else {
            if(null != project) {
                Aggregation.newAggregation(match, project).withOptions(options)
            } else {
                Aggregation.newAggregation(match).withOptions(options)
            }
        }
    }

    /**
     * 通过任务id清单查询重复率告警信息
     */
    fun findDUPCByTaskIdInAndToolName(
        taskIds: List<Long>,
        filterFields: List<String>?,
        status: List<Int>?,
        relPathList: List<String>?,
        pageable: Pageable?
    ): List<DUPCDefectModel> {
        val fieldsObj = BasicDBObject()
        PageUtils.getFilterFields(filterFields, fieldsObj)
        val query = BasicQuery(BasicDBObject().toJson(), fieldsObj.toJson())
        query.addCriteria(Criteria.where("task_id").`in`(taskIds))
        if (!status.isNullOrEmpty()) {
            query.addCriteria(Criteria.where("status").`in`(status))
        }
        if (!relPathList.isNullOrEmpty()) {
            query.addCriteria(Criteria.where("rel_path").`in`(relPathList))
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
        toolName: String,
        filterFields: List<String>?,
        pageable: Pageable?
    ): List<CLOCDefectModel> {
        val fieldsObj = BasicDBObject()
        PageUtils.getFilterFields(filterFields, fieldsObj)
        val query = BasicQuery(BasicDBObject().toJson(), fieldsObj.toJson())
        val toolList = if(toolName == ComConstants.Tool.CLOC.name) {
            listOf(toolName, null)
        } else {
            listOf(toolName)
        }
        query.addCriteria(Criteria.where("task_id").`in`(taskIds).and("tool_name").`in`(toolList).and("status").`is`("ENABLED"))
        if (null != pageable) {
            query.with(pageable)
        }
        return defectMongoTemplate.find(query, CLOCDefectModel::class.java, "t_cloc_defect")
    }

    /**
     * 汇总统计告警
     */
    fun findDefectByTaskIdInAndToolName(taskIds: Collection<Long>, toolName: String): List<DefectStatModel> {
        val match = Aggregation.match(
            Criteria.where("task_id").`in`(taskIds).and("tool_name").`is`(toolName).and
                ("status").`in`(Lists.newArrayList(1, 3))
        )

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

    fun findStatByTaskIdAndToolNameAndTime(
        taskIds: Collection<Long>,
        toolName: String,
        startTime: Long,
        endTime: Long,
        pageable: Pageable
    ): MutableList<Document>? {
        val criteria =
            Criteria.where("task_id").`in`(taskIds).and("tool_name").`is`(toolName).and("status").`is`("ENABLED")
        criteria.andOperator(
            Criteria.where("time_stamp").gte("$startTime"),
            Criteria.where("time_stamp").lt("$endTime")
        )
        val fields = Document()
        fields["_id"] = 0
        fields["_class"] = 0
        val query = BasicQuery(criteria.criteriaObject, fields)
        query.with(pageable)
        return defectMongoTemplate.find(query, Document::class.java, "t_stat_defect")
    }

    fun findCommonByIds(
        taskId: Long,
        toolName: String,
        idList: List<String>,
        status: List<Int>?,
        pageable: Pageable?
    ): List<DefectModel> {
        val query = Query()
        query.addCriteria(
            Criteria.where("task_id").`is`(taskId)
                .and("tool_name").`is`(toolName)
                .and("id").`in`(idList)
        )

        if (!status.isNullOrEmpty()) {
            query.addCriteria(Criteria.where("status").`in`(status))
        }
        if (null != pageable) {
            query.with(pageable)
        }

        return defectMongoTemplate.find(query, DefectModel::class.java, "t_defect")
    }

    fun findLintByIds(idList: List<ObjectId>, status: List<Int>?, pageable: Pageable?): List<LintDefectV2Model> {
        val query = Query()
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
        val query = Query()
        query.addCriteria(Criteria.where("_id").`in`(idList))

        if (!status.isNullOrEmpty()) {
            query.addCriteria(Criteria.where("status").`in`(status))
        }
        if (null != pageable) {
            query.with(pageable)
        }

        return defectMongoTemplate.find(query, CCNDefectModel::class.java, "t_ccn_defect")
    }

    /**
     * 批量查询告警列表
     */
    fun batchQueryDefect(taskIds: Collection<Long>, toolName: String, status: List<Int>?): List<DefectModel> {
        val fieldsObj = BasicDBObject()
        val filterFields = Lists.newArrayList(
            "task_id", "tool_name", "status", "severity", "create_time", "fixed_time",
            "ignore_time", "id"
        )
        PageUtils.getFilterFields(filterFields, fieldsObj)
        val query = BasicQuery(BasicDBObject().toJson(), fieldsObj.toJson())
        query.addCriteria(Criteria.where("task_id").`in`(taskIds).and("tool_name").`is`(toolName))

        // 告警状态筛选
        if (!status.isNullOrEmpty()) {
            query.addCriteria(Criteria.where("status").`in`(status))
        }

        return defectMongoTemplate.find(query, DefectModel::class.java, "t_defect")
    }

    /**
     * 批量统计各严重级别的告警数
     */
    fun batchStatDefect(
        taskIds: Collection<Long>,
        toolName: String,
        status: List<Int>?,
        timeField: String,
        startTime: Long,
        endTime: Long
    ): List<DefectModel> {
        // 索引筛选1
        val match1 = getIndexMatchOperation(taskIds, toolName, status)

        // 选填筛选2
        val match2 = getFilterMatchOperation(startTime, endTime, timeField)

        val group = Aggregation.group("task_id", "severity")
            .first("task_id").`as`("task_id")
            .first("severity").`as`("severity")
            .count().`as`("line_number")

        val agg = Aggregation.newAggregation(match1, match2, group)
        return defectMongoTemplate.aggregate(agg, "t_defect", DefectModel::class.java).mappedResults
    }

    /**
     * 索引筛选
     */
    private fun getIndexMatchOperation(
        taskIds: Collection<Long>,
        toolName: String,
        status: List<Int>?
    ): MatchOperation {
        val criteriaIdxList: MutableList<Criteria?> = Lists.newArrayList()
        criteriaIdxList.add(Criteria.where("task_id").`in`(taskIds).and("tool_name").`is`(toolName))

        if (CollectionUtils.isNotEmpty(status)) {
            criteriaIdxList.add(Criteria.where("status").`in`(status))
        }

        val criteriaIdx = Criteria()
        if (CollectionUtils.isNotEmpty(criteriaIdxList)) {
            criteriaIdx.andOperator(*criteriaIdxList.toTypedArray())
        }
        return Aggregation.match(criteriaIdx)
    }

    /**
     * 非索引筛选
     */
    private fun getFilterMatchOperation(startTime: Long, endTime: Long, timeField: String): MatchOperation {
        val criteriaAfterList: MutableList<Criteria> = Lists.newArrayList()
        if (startTime != 0L && endTime != 0L) {
            criteriaAfterList.add(Criteria.where(timeField).gte(startTime).lte(endTime))
        }

        val criteriaAfter = Criteria()
        if (CollectionUtils.isNotEmpty(criteriaAfterList)) {
            criteriaAfter.andOperator(*criteriaAfterList.toTypedArray())
        }
        return Aggregation.match(criteriaAfter)
    }

    /**
     * 批量分页查询告警列表
     */
    fun batchQueryDefectPage(
        taskIds: List<Long>,
        toolName: String,
        status: List<Int>,
        timeField: String,
        startTime: Long,
        endTime: Long,
        fieldSet: Set<String>,
        pageable: Pageable
    ):
        List<DefectModel> {
        // 索引筛选
        val match1 = getIndexMatchOperation(taskIds, toolName, status)
        // 普通选填筛选
        val match2 = getFilterMatchOperation(startTime, endTime, timeField)
        // 获取指定字段
        val project = Aggregation.project(*fieldSet.toTypedArray())
        // 排序分页
        val pageSize = pageable.pageSize
        val pageNumber = pageable.pageNumber
        val sort = Aggregation.sort(pageable.sort)
        val skip = Aggregation.skip((pageNumber * pageSize).toLong())
        val limit = Aggregation.limit(pageSize.toLong())

        val agg = Aggregation.newAggregation(match1, match2, sort, skip, project, limit)

        return defectMongoTemplate.aggregate(agg, "t_defect", DefectModel::class.java).mappedResults
    }

    /**
     * 批量统计多状态的告警数
     */
    fun batchStatDefectCountInStatus(
            taskIds: Collection<Long>,
            toolName: String,
            status: List<Int>?,
            timeField: String,
            startTime: Long,
            endTime: Long
    ): List<DefectStatModel> {
        // 索引筛选1
        val match1 = getIndexMatchOperation(taskIds, toolName, status)

        // 选填筛选2
        val match2 = getFilterMatchOperation(startTime, endTime, timeField)

        val group = Aggregation.group("task_id")
                .first("task_id").`as`("task_id")
                .count().`as`("count")

        val agg = Aggregation.newAggregation(match1, match2, group)
        return defectMongoTemplate.aggregate(agg, "t_defect", DefectStatModel::class.java).mappedResults
    }

    /**
     * 以规则维度统计缺陷类告警数
     *
     * @param taskIds   任务ID集合
     * @param toolName  工具名
     * @param status    告警状态
     * @return list
     */
    fun findDefectByGroupChecker(
            taskIds: Collection<Long>,
            toolName: String,
            status: Int,
            startTime: Long,
            endTime: Long
    ): List<DefectModel> {
        val matchIdx = Aggregation.match(Criteria.where("task_id").`in`(taskIds)
                .and("tool_name").`is`(toolName).and("status").`is`(status))
        // 选填筛选
        val criteriaList: MutableList<Criteria> = Lists.newArrayList()
        if (startTime != 0L && endTime != 0L) {
            // 按告警状态匹配对应时间字段
            val timeField = ConvertUtil.timeField4DefectStatus(status)
            criteriaList.add(Criteria.where(timeField).gte(startTime).lte(endTime))
        }
        val criteria = Criteria()
        if (CollectionUtils.isNotEmpty(criteriaList)) {
            criteria.andOperator(*criteriaList.toTypedArray())
        }

        val afterMatch = Aggregation.match(criteria)
        val sort = Aggregation.sort(Sort.Direction.ASC, "task_id")

        // 以规则为维度统计告警数
        val group = Aggregation.group("task_id", "tool_name", "checker_name")
                .first("task_id").`as`("task_id")
                .first("tool_name").`as`("tool_name")
                .first("checker_name").`as`("checker_name")
                .first("severity").`as`("severity")
                .count().`as`("line_number")

        // 允许磁盘操作(支持较大数据集合的处理)
        val options = AggregationOptions.Builder().allowDiskUse(true).build()
        val agg = Aggregation.newAggregation(matchIdx, afterMatch, group, sort).withOptions(options)
        return defectMongoTemplate.aggregate(agg, "t_defect", DefectModel::class.java).mappedResults
    }

}
