package com.tencent.bkrepo.rpm.job

import com.tencent.bkrepo.rpm.pojo.IndexType
import com.tencent.bkrepo.rpm.util.RpmCollectionUtils
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class OthersJob {

    @Autowired
    private lateinit var jobService: JobService

    // 每次任务间隔 ms
    @Scheduled(fixedDelay = 30 * 1000)
    @SchedulerLock(name = "OthersJob", lockAtMostFor = "PT30M")
    fun updateOthersIndex() {
        logger.info("update others index start")
        val startMillis = System.currentTimeMillis()
        val repoList = jobService.getAllRpmRepo()
        repoList?.let {
            for (repo in repoList) {
                logger.info("update others index [${repo.projectId}|${repo.name}] start")
                val rpmConfiguration = repo.configuration
                val repodataDepth = rpmConfiguration.getIntegerSetting("repodataDepth") ?: 0
                val targetSet = RpmCollectionUtils.filterByDepth(jobService.findRepodataDirs(repo), repodataDepth)
                for (repoDataPath in targetSet) {
                    logger.info("update others index [${repo.projectId}|${repo.name}|$repoDataPath] start")
                    jobService.batchUpdateIndex(repo, repoDataPath, IndexType.OTHERS, 20)
                    logger.info("update others index [${repo.projectId}|${repo.name}|$repoDataPath] done")
                }
                logger.info("update others index [${repo.projectId}|${repo.name}] done")
            }
        }
        logger.info("update others index done, cost time: ${System.currentTimeMillis() - startMillis} ms")
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(OthersJob::class.java)
    }
}
