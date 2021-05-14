package com.tencent.bk.codecc.apiquery.defect.dao

import com.tencent.bk.codecc.apiquery.defect.model.CodeRepoFromAnalyzeLogModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository

@Repository
class CodeRepoAnalyzeLogDao @Autowired constructor(
    private val defectMongoTemplate: MongoTemplate
){

    fun getRepoListByTaskIdList(taskIdList: List<Long>) : List<CodeRepoFromAnalyzeLogModel>{
        val query = Query()
        query.addCriteria(Criteria.where("task_id").`in`(taskIdList))
        return defectMongoTemplate.find(query, CodeRepoFromAnalyzeLogModel::class.java, "t_code_repo_from_analyzelog")
    }
}
