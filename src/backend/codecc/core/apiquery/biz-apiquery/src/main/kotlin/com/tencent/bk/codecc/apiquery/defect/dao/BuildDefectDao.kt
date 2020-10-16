package com.tencent.bk.codecc.apiquery.defect.dao

import com.mongodb.BasicDBObject
import com.tencent.bk.codecc.apiquery.defect.model.BuildDefectModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.BasicQuery
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Repository

@Repository
class BuildDefectDao @Autowired constructor(
        private val defectMongoTemplate: MongoTemplate
) {

    fun findByTaskIdToolNameAndBuildId(taskId: Long, toolName: String, buildId: String): List<BuildDefectModel> {
        val query = BasicQuery(BasicDBObject())
        query.addCriteria(
                Criteria.where("task_id").`is`(taskId)
                        .and("tool_name").`is`(toolName)
                        .and("build_id").`is`(buildId)
        )

        return defectMongoTemplate.find(query, BuildDefectModel::class.java, "t_build_defect")
    }
}