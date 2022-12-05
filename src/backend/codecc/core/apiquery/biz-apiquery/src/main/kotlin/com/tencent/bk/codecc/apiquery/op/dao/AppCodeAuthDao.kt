package com.tencent.bk.codecc.apiquery.op.dao

import com.mongodb.BasicDBObject
import com.tencent.bk.codecc.apiquery.op.model.AppCodeAdminEntity
import com.tencent.bk.codecc.apiquery.op.model.AppCodeOrgEntity
import com.tencent.bk.codecc.apiquery.op.model.AppCodeProjectEntity
import com.tencent.bk.codecc.apiquery.op.model.AppCodeToolEntity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.BasicQuery
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Repository

@Repository
class AppCodeAuthDao @Autowired constructor(
    private val opMongoTemplate: MongoTemplate
) {

    fun findOrgInfoByAppCode(appCode: String): AppCodeOrgEntity? {
        val fieldsObj = BasicDBObject()
        val query = BasicQuery(BasicDBObject().toJson(), fieldsObj.toJson())
        query.addCriteria(Criteria.where("app_code").`is`(appCode))
        return opMongoTemplate.findOne(query, AppCodeOrgEntity::class.java, "t_appcode_org")
    }

    fun findProjectInfoByAppCode(appCode: String): AppCodeProjectEntity? {
        val fieldsObj = BasicDBObject()
        val query = BasicQuery(BasicDBObject().toJson(), fieldsObj.toJson())
        query.addCriteria(Criteria.where("app_code").`is`(appCode))
        return opMongoTemplate.findOne(query, AppCodeProjectEntity::class.java, "t_appcode_project")
    }

    fun findAdminInfoByAppCode(): List<AppCodeAdminEntity>? {
        return opMongoTemplate.findAll(AppCodeAdminEntity::class.java, "t_appcode_admin")
    }

    fun findToolInfoByAppCode(appCode: String): AppCodeToolEntity? {
        val fieldsObj = BasicDBObject()
        val query = BasicQuery(BasicDBObject().toJson(), fieldsObj.toJson())
        query.addCriteria(Criteria.where("app_code").`is`(appCode))
        return opMongoTemplate.findOne(query, AppCodeToolEntity::class.java, "t_appcode_tool")
    }
}
