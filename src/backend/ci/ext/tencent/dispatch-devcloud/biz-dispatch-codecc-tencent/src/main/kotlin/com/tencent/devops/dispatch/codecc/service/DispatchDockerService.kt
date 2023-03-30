package com.tencent.devops.dispatch.codecc.service

import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.dispatch.codecc.pojo.DockerIpInfoVO
import com.tencent.devops.dispatch.codecc.pojo.DockerIpListPage
import com.tencent.devops.dispatch.codecc.dao.PipelineDockerBuildDao
import com.tencent.devops.dispatch.codecc.dao.PipelineDockerIPInfoDao
import com.tencent.devops.dispatch.codecc.dao.PipelineDockerTaskSimpleDao
import com.tencent.devops.dispatch.codecc.pojo.CodeccClusterEnum
import com.tencent.devops.dispatch.codecc.pojo.DockerHostLoadConfig
import com.tencent.devops.dispatch.codecc.pojo.DockerIpUpdateVO
import com.tencent.devops.dispatch.codecc.utils.CommonUtils
import com.tencent.devops.dispatch.codecc.utils.DockerHostUtils
import com.tencent.devops.dispatch.codecc.utils.RedisUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class DispatchDockerService @Autowired constructor(
    private val dslContext: DSLContext,
    private val gray: Gray,
    private val pipelineDockerIPInfoDao: PipelineDockerIPInfoDao,
    private val pipelineDockerTaskSimpleDao: PipelineDockerTaskSimpleDao,
    private val pipelineDockerBuildDao: PipelineDockerBuildDao,
    private val dockerHostUtils: DockerHostUtils,
    private val redisUtils: RedisUtils
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DispatchDockerService::class.java)
    }

    fun list(userId: String, page: Int?, pageSize: Int?): DockerIpListPage<DockerIpInfoVO> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 10

        try {
            val dockerIpList = pipelineDockerIPInfoDao.getDockerIpList(dslContext, pageNotNull, pageSizeNotNull)
            val count = pipelineDockerIPInfoDao.getDockerIpCount(dslContext)

            if (dockerIpList.size == 0 || count == 0L) {
                return DockerIpListPage(pageNotNull, pageSizeNotNull, 0, emptyList())
            }
            val dockerIpInfoVOList = mutableListOf<DockerIpInfoVO>()
            dockerIpList.forEach {
                dockerIpInfoVOList.add(
                    DockerIpInfoVO(
                    id = it.id,
                    dockerIp = it.dockerIp,
                    dockerHostPort = it.dockerHostPort,
                    capacity = it.capacity,
                    usedNum = it.usedNum,
                    averageCpuLoad = it.cpuLoad,
                    averageMemLoad = it.memLoad,
                    averageDiskLoad = it.diskLoad,
                    averageDiskIOLoad = it.diskIoLoad,
                    enable = it.enable,
                    grayEnv = it.grayEnv,
                    specialOn = it.specialOn,
                    clusterName = CodeccClusterEnum.valueOf(it.clusterName),
                    createTime = it.gmtCreate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                )
                )
            }

            return DockerIpListPage(pageNotNull, pageSizeNotNull, count, dockerIpInfoVOList)
        } catch (e: Exception) {
            logger.error("OP dispatchDocker list error.", e)
            throw RuntimeException("OP dispatchDocker list error.")
        }
    }

    fun create(userId: String, dockerIpInfoVOs: List<DockerIpInfoVO>): Boolean {
        logger.info("$userId create docker IP $dockerIpInfoVOs")
        dockerIpInfoVOs.forEach {
            if (!CommonUtils.verifyIp(it.dockerIp.trim())) {
                logger.warn("Dispatch create dockerIp error, invalid IP format: ${it.dockerIp}")
                throw RuntimeException("Dispatch create dockerIp error, invalid IP format: ${it.dockerIp}")
            }
        }

        try {
            dockerIpInfoVOs.forEach {
                pipelineDockerIPInfoDao.createOrUpdate(
                    dslContext = dslContext,
                    dockerIp = it.dockerIp.trim(),
                    dockerHostPort = it.dockerHostPort,
                    capacity = it.capacity,
                    used = it.usedNum,
                    cpuLoad = it.averageCpuLoad,
                    memLoad = it.averageMemLoad,
                    diskLoad = it.averageDiskLoad,
                    diskIOLoad = it.averageDiskIOLoad,
                    enable = it.enable,
                    grayEnv = it.grayEnv,
                    specialOn = it.specialOn,
                    clusterName = it.clusterName!!.name
                )
            }

            return true
        } catch (e: Exception) {
            logger.error("OP dispatchDocker create error.", e)
            throw RuntimeException("OP dispatchDocker create error.")
        }
    }

    fun update(userId: String, dockerIp: String, dockerIpUpdateVO: DockerIpUpdateVO): Boolean {
        logger.info("$userId update Docker IP: $dockerIp dockerIpUpdateVO: $dockerIpUpdateVO")
        try {
            pipelineDockerIPInfoDao.update(
                dslContext = dslContext,
                dockerIp = dockerIp,
                dockerHostPort = dockerIpUpdateVO.dockerHostPort,
                enable = dockerIpUpdateVO.enable,
                grayEnv = dockerIpUpdateVO.grayEnv,
                specialOn = dockerIpUpdateVO.specialOn,
                clusterName = dockerIpUpdateVO.clusterName.name
            )
            return true
        } catch (e: Exception) {
            logger.error("OP dispatchDocker update error.", e)
            throw RuntimeException("OP dispatchDocker update error.")
        }
    }

    fun updateAllDispatchDockerEnable(userId: String): Boolean {
        logger.info("$userId update all docker enable.")
        try {
            val dockerUnavailableList = pipelineDockerIPInfoDao.getDockerIpList(
                dslContext = dslContext,
                enable = false,
                grayEnv = gray.isGray()
            )

            dockerUnavailableList.forEach {
                pipelineDockerIPInfoDao.updateDockerIpStatus(dslContext, it.dockerIp, true)
            }

            return true
        } catch (e: Exception) {
            logger.error("OP updateAllDispatchDockerEnable error.", e)
            throw RuntimeException("OP updateAllDispatchDockerEnable error.")
        }
    }

    fun updateDockerIpLoad(userId: String, dockerIp: String, dockerIpInfoVO: DockerIpInfoVO): Boolean {
        // logger.info("$userId update Docker IP status enable: $dockerIp, dockerIpInfoVO: $dockerIpInfoVO")
        try {
            pipelineDockerIPInfoDao.updateDockerIpLoad(
                dslContext = dslContext,
                dockerIp = dockerIp,
                dockerHostPort = dockerIpInfoVO.dockerHostPort,
                used = dockerIpInfoVO.usedNum,
                cpuLoad = dockerIpInfoVO.averageCpuLoad,
                memLoad = dockerIpInfoVO.averageMemLoad,
                diskLoad = dockerIpInfoVO.averageDiskLoad,
                diskIOLoad = dockerIpInfoVO.averageDiskIOLoad,
                enable = dockerIpInfoVO.enable
            )
            return true
        } catch (e: Exception) {
            logger.error("OP dispatchDocker updateDockerIpEnable error.", e)
            throw RuntimeException("OP dispatchDocker updateDockerIpEnable error.")
        }
    }

    fun delete(userId: String, dockerIp: String): Boolean {
        logger.info("$userId delete docker: $dockerIp")

        if (dockerIp.isEmpty()) {
            throw RuntimeException("Docker IP is null or ''")
        }

        try {
            pipelineDockerIPInfoDao.delete(dslContext, dockerIp)
            // 清空与之关联构建分配
            pipelineDockerTaskSimpleDao.deleteByDockerIp(dslContext, dockerIp)
            return true
        } catch (e: Exception) {
            logger.error("OP dispatchDocker delete error.", e)
            throw RuntimeException("OP dispatchDocker delete error.")
        }
    }

    fun createDockerHostLoadConfig(
        userId: String,
        dockerHostLoadConfigMap: Map<String, DockerHostLoadConfig>
    ): Boolean {
        logger.info("$userId createDockerHostLoadConfig $dockerHostLoadConfigMap")
        if (dockerHostLoadConfigMap.size != 3) {
            throw RuntimeException("Parameter dockerHostLoadConfigMap`size is not 3.")
        }

        try {
            dockerHostUtils.createLoadConfig(dockerHostLoadConfigMap)
            return true
        } catch (e: Exception) {
            logger.error("OP dispatcheDocker create dockerhost loadConfig error.", e)
            throw RuntimeException("OP dispatcheDocker create dockerhost loadConfig error.")
        }
    }

    fun updateDockerDriftThreshold(userId: String, threshold: Int): Boolean {
        logger.info("$userId updateDockerDriftThreshold $threshold")
        if (threshold < 0 || threshold > 100) {
            throw RuntimeException("Parameter threshold must in (0-100).")
        }

        try {
            dockerHostUtils.updateDockerDriftThreshold(threshold)
            return true
        } catch (e: Exception) {
            logger.error("OP dispatcheDocker update Docker DriftThreshold error.", e)
            throw RuntimeException("OP dispatcheDocker update Docker DriftThreshold error.")
        }
    }

    fun deleteDockerBuildRedisAuth(
        userId: String,
        startTime: String,
        endTime: String,
        limit: Int
    ): Boolean {
        logger.info("$userId deleteDockerBuildRedisAuth startTime: $startTime endTime: $endTime limit: $limit")

        try {
            val buildList = pipelineDockerBuildDao.getBuildListByLimit(dslContext, startTime, endTime, limit)
            buildList.forEach {
                if (!it.secretKey.isNullOrEmpty()) {
                    logger.info("BuildId: ${it.buildId} deleteDockerBuildRedisAuth ${it.id} | ${it.secretKey}")
                    redisUtils.deleteDockerBuild(it.id, it.secretKey)
                }
            }
            return true
        } catch (e: Exception) {
            logger.info("$userId deleteDockerBuildRedisAuth error.", e)
            throw RuntimeException("OP deleteDockerBuildRedisAuth error.")
        }
    }
}
