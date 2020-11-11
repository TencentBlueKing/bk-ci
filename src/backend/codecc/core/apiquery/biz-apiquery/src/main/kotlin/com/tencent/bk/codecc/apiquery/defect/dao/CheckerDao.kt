package com.tencent.bk.codecc.apiquery.defect.dao


import com.mongodb.BasicDBObject
import com.tencent.bk.codecc.apiquery.defect.model.CheckerDetailModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.BasicQuery
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Repository

@Repository
class CheckerDao @Autowired constructor(
    private val defectMongoTemplate: MongoTemplate
){

    /**
     * 根据工具名和规则key批量查询规则详情
     */
    fun batchFindByToolNameAndCheckerKey(toolNameList: List<String>, checkerKeyList: List<String>): List<CheckerDetailModel> {
        val query = BasicQuery(BasicDBObject())
        query.addCriteria(
            Criteria.where("tool_name").`in`(toolNameList)
                .and("checker_key").`in`(checkerKeyList)
        )

        return defectMongoTemplate.find(query, CheckerDetailModel::class.java, "t_checker_detail")
    }
}