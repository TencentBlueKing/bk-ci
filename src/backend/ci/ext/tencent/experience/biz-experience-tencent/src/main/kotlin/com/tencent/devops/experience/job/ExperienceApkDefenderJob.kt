package com.tencent.devops.experience.job

import com.tencent.devops.artifactory.api.service.ServiceArtifactoryDownLoadResource
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.experience.constant.ExperienceConstant
import com.tencent.devops.experience.dao.ExperienceDao
import com.tencent.devops.experience.service.ExperienceService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class ExperienceApkDefenderJob @Autowired constructor(
    private val experienceService: ExperienceService,
    private val redisOperation: RedisOperation,
    private val experienceDao: ExperienceDao,
    private val dslContext: DSLContext,
    private val client: Client
) {
    @Scheduled(cron = "0/10 * * * * ?")
    fun checkTask() {
        redisOperation.rightPop(ExperienceConstant.APK_DEFENDER_EXPERIENCE_IDS)?.let { e ->
            try {
                val data = e.split(",")
                val experienceId = data[0].toLong()
                val ddl = data[1].toLong()
                val apkDefendersKey = ExperienceConstant.apkDefendersKey(experienceId)
                logger.info("check apk defender : $experienceId")
                if (LocalDateTime.now().timestamp() > ddl) {
                    logger.warn("checkTask , time is ddl , e: $e")
                    redisOperation.delete(apkDefendersKey)
                }
                val experience = experienceDao.get(dslContext, experienceId)
                redisOperation.getSetMembers(apkDefendersKey)?.forEach { t ->
                    val success = client.get(ServiceArtifactoryDownLoadResource::class).checkApkDefenderTask(
                        projectId = experience.projectId,
                        userId = experience.creator,
                        taskId = t
                    ).data
                    logger.info("checkTask , task: $t , success: $success")
                    if (success == true) {
                        redisOperation.sremove(apkDefendersKey, t)
                    }
                }
                if (redisOperation.hasKey(apkDefendersKey)) {
                    logger.info("some tasks are running , experienceId: $experienceId")
                    redisOperation.leftPush(ExperienceConstant.APK_DEFENDER_EXPERIENCE_IDS, e)
                } else {
                    logger.info("all tasks have done , experienceId: $experienceId")
                    experienceService.sendNotification(experienceId)
                }
            } catch (t: Throwable) {
                logger.error("check task failed , e: $e", t)
                redisOperation.leftPush(ExperienceConstant.APK_DEFENDER_EXPERIENCE_IDS, e)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ExperienceApkDefenderJob::class.java)
    }
}
