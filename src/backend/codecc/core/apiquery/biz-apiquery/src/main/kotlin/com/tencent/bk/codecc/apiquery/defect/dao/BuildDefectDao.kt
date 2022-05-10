package com.tencent.bk.codecc.apiquery.defect.dao

import com.tencent.bk.codecc.apiquery.defect.model.BuildDefectIdsModel
import com.tencent.bk.codecc.apiquery.defect.model.BuildDefectModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOptions
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository

@Repository
class BuildDefectDao @Autowired constructor(
    private val defectMongoTemplate: MongoTemplate
) {

    fun findByTaskIdToolNameAndBuildId(
        taskIdList: List<Long>,
        toolName: String,
        buildId: String
    ): List<BuildDefectModel> {
        val query = Query.query(Criteria.where("task_id").`in`(taskIdList)
            .and("tool_name").`is`(toolName)
            .and("build_id").`is`(buildId))
        return defectMongoTemplate.find(query, BuildDefectModel::class.java, "t_build_defect")
    }

    fun findByTaskIdToolNameAndBuildIdOrderByFilePath(
        taskIdList: List<Long>,
        toolName: String,
        buildId: String,
        pageNum: Int?,
        pageSize: Int?,
        sortField : String?,
        sortType : String?
    ): List<BuildDefectIdsModel> {
        val match = Aggregation.match(
            Criteria.where("task_id").`in`(taskIdList)
                .and("tool_name").`is`(toolName)
                .and("build_id").`is`(buildId)
        )

        val unwind = Aggregation.unwind("file_defect_ids")
        val project = Aggregation.project("file_defect_ids")

        val skip = Aggregation.skip((((pageNum ?: 1) - 1) * (pageSize ?: 100)).toLong())
        val limit = Aggregation.limit((pageSize ?: 100).toLong())
        //允许磁盘操作
        val options = AggregationOptions.Builder().allowDiskUse(true).build()
        val agg = if(!sortField.isNullOrBlank() && !sortType.isNullOrBlank()){
            val sort = Aggregation.sort(Sort.by(Sort.Direction.fromString(sortType), sortField))
            Aggregation.newAggregation(match, sort, unwind, project, skip, limit).withOptions(options)
        } else {
            Aggregation.newAggregation(match, unwind, project, skip, limit).withOptions(options)
        }



        return defectMongoTemplate.aggregate(agg, "t_build_defect", BuildDefectIdsModel::class.java).mappedResults
    }
}
