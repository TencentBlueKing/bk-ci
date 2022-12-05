package com.tencent.bk.codecc.apiquery.task.dao

import com.tencent.bk.codecc.apiquery.task.model.BaseDataModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository

@Repository
class BaseDataDao @Autowired constructor(
    private val taskMongoTemplate: MongoTemplate
) {

    /**
     * 根据参数类型查询基础数据
     */
    fun findByParamType(paramType: String): List<BaseDataModel> {
        val query = Query()
        query.addCriteria(
            Criteria.where("param_type").`is`(paramType)
        )
        return taskMongoTemplate.find(query, BaseDataModel::class.java, "t_base_data")
    }

    fun findAllByParamTypeAndParamCode(paramType: String, paramCode: String): List<BaseDataModel> {
        val query = Query()
        query.addCriteria(
                Criteria.where("param_type").`is`(paramType).and("param_code").`is`(paramCode)
        )
        val baseDataList = taskMongoTemplate.find(query, BaseDataModel::class.java, "t_base_data")
        return if (baseDataList.isEmpty()) mutableListOf<BaseDataModel>() else baseDataList
    }
}