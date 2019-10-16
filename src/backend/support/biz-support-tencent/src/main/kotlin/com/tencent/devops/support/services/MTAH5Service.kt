package com.tencent.devops.support.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.support.model.mta.h5.message.CoreDataMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import com.tencent.devops.support.util.mta.h5.MTAH5Util

@Service
class MTAH5Service @Autowired constructor(
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(MTAH5Service::class.java)

    fun getCoreData(coreDataMessage: CoreDataMessage) = MTAH5Util.getCoreData(coreDataMessage)
}
