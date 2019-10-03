package com.tencent.devops.dispatch.service

import com.tencent.devops.dispatch.dao.PrivateVMDao
import com.tencent.devops.dispatch.dao.VMDao
import com.tencent.devops.dispatch.pojo.VMWithPrivateProject
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PrivateVMService @Autowired constructor(
    private val dslContext: DSLContext,
    private val privateVMDao: PrivateVMDao,
    private val vmDao: VMDao
) {

    fun list(): List<VMWithPrivateProject> {
        val records = privateVMDao.list(dslContext)
        val vmIds = records.map {
            it.vmId
        }.toSet()

        val map = records.map {
            it.vmId to it.projectId
        }.toMap()
        val vmRecords = vmDao.findVMByIds(dslContext, vmIds)
        return vmRecords.map {
            VMWithPrivateProject(
                    it.vmId,
                    it.vmMachineId,
                    it.vmTypeId,
                    it.vmIp,
                    it.vmName,
                    map[it.vmId] ?: ""
            )
        }
    }

    fun add(vmId: Int, projectId: String) {
        privateVMDao.add(dslContext, vmId, projectId)
    }

    fun delete(vmId: Int, projectId: String) {
        privateVMDao.delete(dslContext, vmId, projectId)
    }
}