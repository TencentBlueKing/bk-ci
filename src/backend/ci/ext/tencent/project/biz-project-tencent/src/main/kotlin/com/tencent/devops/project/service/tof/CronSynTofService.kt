package com.tencent.devops.project.service.tof

import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.ProjectUserRefreshService
import com.tencent.devops.project.service.ProjectUserService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class CronSynTofService @Autowired constructor(
    val projectUserService: ProjectUserService,
    val projectUserRefreshService: ProjectUserRefreshService,
    val redisOperation: RedisOperation
) {
// 	@Scheduled(cron = "0 15 1 ? * 0")
    @Scheduled(cron = "0 * 1 * * ?")
    fun newClearTimeoutCache() {
        logger.info("syn bkuser from tof start")
        val startTime = System.currentTimeMillis()
        val redisLock = RedisLock(redisOperation, instanceLockKey, 10)
        // 多实例抢锁， 且最后同步时间为一天前
        try {
            redisLock.lock()
            val lastSynTime = redisOperation.get(lastTimeLockKey)
            if (lastSynTime == null || startTime - lastSynTime.toLong() > TimeUnit.DAYS.toMillis(1)) {
                redisOperation.set(lastTimeLockKey, startTime.toString())
                logger.info("start syn tof user")
            } else {
                return
            }
        } finally {
            redisLock.unlock()
            logger.info("start syn tof cron unlock")
        }

        // 开始同步数据
        var page = 0
        val pageSize = 500
        while (true) {
            val pageLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
            val userList = projectUserService.listUser(pageLimit.limit, pageLimit.offset) ?: break

            userList.forEach {
                val userInfo = UserDeptDetail(
                        bgName = it!!.bgName,
                        bgId = it!!.bgId.toString(),
                        centerName = it.centerName,
                        centerId = it!!.centerId.toString(),
                        deptName = it.deptName,
                        deptId = it.deptId.toString(),
                        groupName = it.groupName,
                        groupId = it.groypId.toString()
                )
                val synUser = projectUserRefreshService.synUserInfo(userInfo, it.userId)
                if (synUser != null) {
                    logger.info("syn userdata: old: $userInfo, new:$synUser")
                }
                // 页内间隔5ms
                Thread.sleep(5)
            }

            if (userList.size < pageSize) {
                break
            } else {
                page ++
                // 翻页间隔5s
                Thread.sleep(5000)
            }
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
        val lastTimeLockKey = "project:tof:syn:lastTime"
        val instanceLockKey = "project:syn:tof:instance:lock"
    }
}