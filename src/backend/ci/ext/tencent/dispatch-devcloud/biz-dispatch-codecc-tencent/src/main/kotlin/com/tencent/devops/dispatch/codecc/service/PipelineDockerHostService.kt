package com.tencent.devops.dispatch.codecc.service

import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.dispatch.codecc.pojo.DockerIpInfoVO
import com.tencent.devops.dispatch.codecc.pojo.DockerIpListPage
import com.tencent.devops.dispatch.codecc.dao.PipelineDockerHostDao
import com.tencent.devops.dispatch.codecc.dao.PipelineDockerIPInfoDao
import com.tencent.devops.dispatch.codecc.dao.PipelineDockerTaskSimpleDao
import com.tencent.devops.dispatch.codecc.pojo.CodeccClusterEnum
import com.tencent.devops.dispatch.codecc.pojo.SpecialDockerHostVO
import com.tencent.devops.dispatch.codecc.utils.DockerHostUtils
import com.tencent.devops.dispatch.codecc.utils.RedisUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class PipelineDockerHostService @Autowired constructor(
    private val dslContext: DSLContext,
    private val gray: Gray,
    private val pipelineDockerIPInfoDao: PipelineDockerIPInfoDao,
    private val pipelineDockerTaskSimpleDao: PipelineDockerTaskSimpleDao,
    private val pipelineDockerHostDao: PipelineDockerHostDao,
    private val dockerHostUtils: DockerHostUtils,
    private val redisUtils: RedisUtils
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineDockerHostService::class.java)
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

    fun create(userId: String, specialDockerHostVOs: List<SpecialDockerHostVO>): Boolean {
        logger.info("$userId create specialDockerHost: $specialDockerHostVOs")

        try {
            specialDockerHostVOs.forEach {
                pipelineDockerHostDao.insertHost(
                    dslContext = dslContext,
                    projectId = it.projectId,
                    hostIp = it.hostIp,
                    remark = it.remark
                )
            }

            return true
        } catch (e: Exception) {
            logger.error("OP create specialDockerHost error.", e)
            throw RuntimeException("OP create specialDockerHost error.")
        }
    }

    fun update(userId: String, specialDockerHostVO: SpecialDockerHostVO): Boolean {
        logger.info("$userId update specialDockerhost specialDockerHostVO: $specialDockerHostVO")
        try {
            pipelineDockerHostDao.updateHost(
                dslContext = dslContext,
                projectId = specialDockerHostVO.projectId,
                hostIp = specialDockerHostVO.hostIp,
                remark = specialDockerHostVO.remark
            )
            return true
        } catch (e: Exception) {
            logger.error("OP update specialDockerhost error.", e)
            throw RuntimeException("OP update specialDockerhost error.")
        }
    }

    fun delete(userId: String, projectId: String): Boolean {
        logger.info("$userId delete specialDockerhost: $projectId")

        if (projectId.isEmpty()) {
            throw RuntimeException("projectId is null or ''")
        }

        try {
            pipelineDockerHostDao.delete(dslContext, projectId)
            return true
        } catch (e: Exception) {
            logger.error("OP specialDockerhost delete error.", e)
            throw RuntimeException("OP specialDockerhost delete error.")
        }
    }
}
