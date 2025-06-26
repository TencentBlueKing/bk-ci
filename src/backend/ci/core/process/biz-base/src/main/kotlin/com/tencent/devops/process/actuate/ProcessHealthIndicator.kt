package com.tencent.devops.process.actuate

import com.tencent.devops.process.dao.PipelineFavorDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.AbstractHealthIndicator
import org.springframework.boot.actuate.health.Health
import org.springframework.stereotype.Component

@Component
class ProcessHealthIndicator @Autowired constructor(
    private val pipelineFavorDao: PipelineFavorDao,
    private val dslContext: DSLContext
) : AbstractHealthIndicator() {
    @SuppressWarnings("SwallowedException", "TooGenericExceptionCaught")
    override fun doHealthCheck(builder: Health.Builder) {
        try {
            val maxId = pipelineFavorDao.getMaxId(dslContext)
            builder.up().withDetail("MaxId", maxId)
        } catch (e: Exception) {
            logger.error("Get max id failed")
            builder.down()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProcessHealthIndicator::class.java)
    }
}