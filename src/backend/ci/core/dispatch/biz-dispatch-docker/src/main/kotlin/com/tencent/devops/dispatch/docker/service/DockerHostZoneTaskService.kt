/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dispatch.docker.service

import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.dispatch.docker.dao.PipelineDockerHostDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerHostZoneDao
import com.tencent.devops.dispatch.docker.pojo.DockerHostZone
import com.tencent.devops.dispatch.docker.pojo.SpecialDockerHostVO
import com.tencent.devops.dispatch.docker.utils.DispatchDockerRedisUtils
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerHostZoneRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service@Suppress("ALL")
class DockerHostZoneTaskService @Autowired constructor(
    private val dockerHostZoneDao: PipelineDockerHostZoneDao,
    private val pipelineDockerHostDao: PipelineDockerHostDao,
    private val dslContext: DSLContext,
    private val dispatchDockerRedisUtils: DispatchDockerRedisUtils
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DockerHostZoneTaskService::class.java)
    }

    fun create(
        hostIp: String,
        zone: String,
        remark: String?
    ) = dockerHostZoneDao.insertHostZone(dslContext, hostIp, zone, remark)

    fun delete(hostIp: String) = dockerHostZoneDao.delete(dslContext, hostIp)

    fun count() = dockerHostZoneDao.count(dslContext)

    fun list(page: Int, pageSize: Int): List<DockerHostZone> {
        val dockerHostZoneList = ArrayList<DockerHostZone>()
        dockerHostZoneDao.getList(dslContext, page, pageSize)?.forEach {
            val m = parse(it)
            if (m != null) {
                dockerHostZoneList.add(m)
            }
        }
        return dockerHostZoneList
    }

    fun listSpecialDockerHosts(userId: String): List<SpecialDockerHostVO> {
        try {
            val list = pipelineDockerHostDao.getHostList(dslContext)
            return list.map { t -> SpecialDockerHostVO(t.projectCode, t.hostIp, t.remark) }.toList()
        } catch (e: Exception) {
            logger.error("OP listSpecialDockerHosts error.", e)
            throw RuntimeException("OP listSpecialDockerHosts error.")
        }
    }

    fun create(userId: String, specialDockerHostVOs: List<SpecialDockerHostVO>): Boolean {
        logger.info("$userId create specialDockerHost: $specialDockerHostVOs")
        try {
            specialDockerHostVOs.forEach { specialDockerHostVO ->
                val history = pipelineDockerHostDao.getHost(
                    dslContext = dslContext,
                    projectId = specialDockerHostVO.projectId
                )
                if (history == null) {
                    pipelineDockerHostDao.insertHost(
                        dslContext = dslContext,
                        projectId = specialDockerHostVO.projectId,
                        hostIp = specialDockerHostVO.hostIp,
                        remark = specialDockerHostVO.remark
                    )
                } else {
                    logger.info("userId:$userId had created specialDockerHost: $specialDockerHostVOs")
                    pipelineDockerHostDao.updateHost(
                        dslContext = dslContext,
                        projectId = specialDockerHostVO.projectId,
                        hostIp = specialDockerHostVO.hostIp,
                        remark = specialDockerHostVO.remark
                    )
                }
                if (specialDockerHostVO.nfsShare != null && specialDockerHostVO.nfsShare!!) {
                    dispatchDockerRedisUtils.getSpecialProjectListKey().let {
                        val projectIdList = it?.split(",") ?: emptyList()
                        if (!projectIdList.contains(specialDockerHostVO.projectId)) {
                            dispatchDockerRedisUtils.setSpecialProjectList(it + "," + specialDockerHostVO.projectId)
                        }
                    }
                }
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

    fun enable(hostIp: String, enable: Boolean) = dockerHostZoneDao.enable(dslContext, hostIp, enable)

    private fun parse(record: TDispatchPipelineDockerHostZoneRecord?): DockerHostZone? {
        return if (record == null) {
            null
        } else DockerHostZone(record.hostIp,
                record.zone,
                record.enable == 1.toByte(),
                record.remark,
                record.createdTime.timestamp(),
                record.updatedTime.timestamp())
    }
}
