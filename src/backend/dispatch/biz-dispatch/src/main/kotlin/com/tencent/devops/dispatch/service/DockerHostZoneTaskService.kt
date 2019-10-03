package com.tencent.devops.dispatch.service

import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.dispatch.dao.PipelineDockerHostZoneDao
import com.tencent.devops.dispatch.pojo.DockerHostZone
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerHostZoneRecord
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DockerHostZoneTaskService @Autowired constructor(
    private val dockerHostZoneDao: PipelineDockerHostZoneDao,
    private val dslContext: DSLContext
) {
    fun create(hostIp: String, zone: String, remark: String?) = dockerHostZoneDao.insertHostZone(dslContext, hostIp, zone, remark)

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