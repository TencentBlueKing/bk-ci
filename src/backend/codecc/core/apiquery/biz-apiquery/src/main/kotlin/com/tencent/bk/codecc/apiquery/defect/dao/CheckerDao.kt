package com.tencent.bk.codecc.apiquery.defect.dao

import com.mongodb.BasicDBObject
import com.tencent.bk.codecc.apiquery.defect.model.CheckerDetailModel
import com.tencent.bk.codecc.apiquery.defect.model.CheckerSetModel
import com.tencent.devops.common.util.JsonUtil
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository

@Repository
class CheckerDao @Autowired constructor(
    private val defectMongoTemplate: MongoTemplate
) {

    /**
     * 根据工具名和规则key批量查询规则详情
     */
    fun batchFindByToolNameAndCheckerKey(
        toolNameList: List<String>,
        checkerKeyList: List<String>
    ): List<CheckerDetailModel> {
        val query = Query.query(
            Criteria.where("tool_name").`in`(toolNameList)
                .and("checker_key").`in`(checkerKeyList))
        return defectMongoTemplate.find(query, CheckerDetailModel::class.java, "t_checker_detail")
    }

    fun listByPage(
        pageNum: Int?,
        pageSize: Int?,
        toolName: String?,
        checkerKey: Set<String>?
    ): List<CheckerDetailModel> {
        val query = Query()

        if (StringUtils.isNotBlank(toolName)) {
            query.addCriteria(
                Criteria.where("tool_name").`is`(toolName)
            )
        }

        if (CollectionUtils.isNotEmpty(checkerKey)) {
            query.addCriteria(
                Criteria.where("checker_key").`in`(checkerKey)
            )
        }

        val queryPageNum = pageNum ?: 1
        val queryPageSize = (pageSize ?: 100).coerceAtMost(1000)

        query.with(PageRequest.of(queryPageNum - 1, queryPageSize))

        return defectMongoTemplate.find(query, CheckerDetailModel::class.java, "t_checker_detail")
    }

    /**
     * 对原有批量查询规则详情信息进行优化
     */
    fun batchFindCheckerDetailByToolAndCheckerKey(toolAndCheckerMap: Map<String, List<String>>): List<CheckerDetailModel> {
        val checkerDetailModelList = mutableListOf<CheckerDetailModel>()
        val query = BasicDBObject()
        query["tool_name"] = BasicDBObject("\$in", toolAndCheckerMap.keys)
        query["checker_key"] = BasicDBObject("\$in", toolAndCheckerMap.flatMap { it.value })

        val cursor = defectMongoTemplate.getCollection("t_checker_detail").find(query).cursor()
        while (cursor.hasNext()) {
            val checkerObj = cursor.next()
            val checkerKeyList = toolAndCheckerMap[checkerObj["tool_name"]]
            if (!checkerKeyList.isNullOrEmpty() && checkerKeyList.contains(checkerObj["checker_key"])) {
                checkerDetailModelList.add(JsonUtil.to(JsonUtil.toJson(checkerObj)))
            }
        }
        return checkerDetailModelList
    }

    /**
     * 通过工具名查询规则详细信息
     */
    fun findByToolName(toolName: String): List<CheckerDetailModel> {
        val query = Query()
        query.addCriteria(Criteria.where("tool_name").`is`(toolName))
        return defectMongoTemplate.find(query, CheckerDetailModel::class.java, "t_checker_detail")
    }

    /**
     * 通过规则集id集合查询详情
     */
    fun findLatestVersionByCheckerSetId(checkerSetIds: Set<String>) : List<CheckerSetModel>{
        val match = Aggregation.match(Criteria.where("checker_set_id").`in`(checkerSetIds))
        val sort = Aggregation.sort(Sort.Direction.DESC, "version")
        val group = Aggregation.group("checker_set_id")
                        .first("checker_set_id").`as`("checker_set_id")
                        .first("version").`as`("version")
                        .first("checker_set_name").`as`("checker_set_name")
                        .first("code_lang").`as`("code_lang")
                        .first("checker_set_lang").`as`("checker_set_lang")
                        .first("scope").`as`("scope")
                        .first("creator").`as`("creator")
                        .first("last_update_time").`as`("last_update_time")
                        .first("checker_count").`as`("checker_count")
                        .first("checker_props").`as`("checker_props")
                        .first("task_usage").`as`("task_usage")
                        .first("enable").`as`("enable")
                        .first("sort_weight").`as`("sort_weight")
                        .first("project_id").`as`("project_id")
                        .first("description").`as`("description")
                        .first("catagories").`as`("catagories")
                        .first("base_checker_set_id").`as`("base_checker_set_id")
                        .first("base_checker_set_version").`as`("base_checker_set_version")
                        .first("init_checkers").`as`("init_checkers")
                        .first("official").`as`("official")
                        .first("legacy").`as`("legacy")
                        .first("checker_set_source").`as`("checker_set_source")
        val aggregation = Aggregation.newAggregation(match, sort, group)
        return defectMongoTemplate.aggregate(aggregation, "t_checker_set", CheckerSetModel::class.java).mappedResults
    }
}
