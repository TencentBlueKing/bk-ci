package com.tencent.devops.experience.job

import com.tencent.devops.common.archive.client.BkRepoClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@RefreshScope
class ExperienceRenewJob @Autowired constructor(
    val bkRepoClient: BkRepoClient
) {
    @Scheduled(cron = "0 * * * * ?")
    @SuppressWarnings("MagicNumber", "NestedBlockDepth", "SwallowedException")
    fun jobRenew() {
        bkRepoClient.setMetadata(
            "admin",
            "stuben-test",
            "pipeline",
            "/p-537e0539c6cd4fc4a24230ea89052914/b-01810bbe654243c2919dbd1024d424cd/1-Dispatcher-sdk-1_Bash_1.log",
            mapOf("test" to "1")
        )

        val fileDetail = bkRepoClient.getFileDetail(
            "admin",
            "stuben-test",
            "pipeline",
            "/p-537e0539c6cd4fc4a24230ea89052914/b-01810bbe654243c2919dbd1024d424cd/1-Dispatcher-sdk-1_Bash_1.log"
        )

        logger.info("file detail is : {}", fileDetail)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExperienceRenewJob::class.java)
    }
}
