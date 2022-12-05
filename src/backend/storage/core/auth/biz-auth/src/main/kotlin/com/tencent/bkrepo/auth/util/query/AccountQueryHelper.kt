package com.tencent.bkrepo.auth.util.query

import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

object AccountQueryHelper {

    fun checkCredential(accessKey: String, secretKey: String): Query {
        return Query.query(
            Criteria.where("credentials.secretKey").`is`(secretKey)
                .and("credentials.accessKey").`is`(accessKey)
        )
    }
}
