package com.tencent.devops.turbo.dao.mongotemplate

import com.tencent.devops.turbo.model.TTurboRecordSeqNumEntity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository

@Repository
class TurboRecordSeqNumDao @Autowired constructor(
    private val mongoTemplate: MongoTemplate
) {

    /**
     * 生成以项目维度的自增id
     */
    fun increaseSeqNum(projectId: String): Int {
        val query = Query()
        query.addCriteria(Criteria.where("project_id").`is`(projectId))
        return mongoTemplate.findAndModify(
            query, Update().inc("seq_num", 1),
            FindAndModifyOptions.options().upsert(true).returnNew(true),
            TTurboRecordSeqNumEntity::class.java
        )?.seqNum ?: 1
    }
}
