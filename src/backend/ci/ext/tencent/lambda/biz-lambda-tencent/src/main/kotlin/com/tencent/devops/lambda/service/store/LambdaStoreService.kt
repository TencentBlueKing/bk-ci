package com.tencent.devops.lambda.service.store

import com.tencent.devops.lambda.dao.store.LambdaStoreDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class LambdaStoreService @Autowired constructor(
    private val dslContext: DSLContext,
    private val lambdaStoreDao: LambdaStoreDao
) {
    fun getPlatName(platformCode: String?): String? {
        return lambdaStoreDao.getPlatformName(
            dslContext,
            platformCode
        )
    }
}