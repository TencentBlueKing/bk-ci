package com.tencent.bk.codecc.apiquery.task.dao

import com.tencent.bk.codecc.apiquery.task.model.ToolConfigInfoModel
import com.tencent.devops.common.constant.ComConstants
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository

@Repository
class ToolConfigDao @Autowired constructor(
    private val taskMongoTemplate: MongoTemplate
) {

    fun findByToolName(
        toolName: String,
        pageable: Pageable?
    ): List<ToolConfigInfoModel> {
        val query = Query()
        query.addCriteria(Criteria.where("tool_name").`is`(toolName))
            .addCriteria(Criteria.where("follow_status").ne(ComConstants.FOLLOW_STATUS.WITHDRAW.value()))
        if (null != pageable) {
            query.with(pageable)
        }
        return taskMongoTemplate.find(query, ToolConfigInfoModel::class.java, "t_tool_config")
    }
}
