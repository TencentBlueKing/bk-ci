package com.tencent.devops.plugin.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.plugin.dao.WetestTaskInstResultDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WetestTaskInstResultService @Autowired constructor(
    private val wetestTaskInstResultDao: WetestTaskInstResultDao,
    private val objectMapper: ObjectMapper,
    private val dslContext: DSLContext
) {

    fun saveResult(testId: String, callback: Map<String, Any>): String {
        return wetestTaskInstResultDao.insert(dslContext, testId, objectMapper.writeValueAsString(callback)).toString()
    }
}