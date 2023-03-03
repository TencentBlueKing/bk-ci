package com.tencent.devops.dispatch.macos.service

import com.google.common.base.Preconditions
import com.tencent.devops.dispatcher.macos.dao.MacHostMachineDao
import com.tencent.devops.dispatch.macos.pojo.HostMachineInfo
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.model.dispatcher.macos.tables.records.TMacHostMachineRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class MacHostService @Autowired constructor(
    private val dslContext: DSLContext,
    private val macHostMachineDao: MacHostMachineDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(MacHostService::class.java)
    }

    @Value("\${credential.aes-key:C/R%3{?OS}IeGT21}")
    private lateinit var aesKey: String

    fun createHostInfo(info: HostMachineInfo): Boolean {
        Preconditions.checkArgument(!info.name.isNullOrBlank(), "name can not be null")
        Preconditions.checkArgument(!info.ip.isNullOrBlank(), "ip can not be null")
        Preconditions.checkArgument(!info.userName.isNullOrBlank(), "userName can not be null")
        Preconditions.checkArgument(!info.password.isNullOrBlank(), "password can not be null")
        info.password = AESUtil.encrypt(aesKey, info.password ?: "")
        val result = macHostMachineDao.saveHostInfo(dslContext, info)
        return result > 0
    }

    fun updateHostInfo(info: HostMachineInfo): Boolean {
        info.password = AESUtil.encrypt(aesKey, info.password ?: "")
        val result = macHostMachineDao.updateHostInfo(dslContext, info)
        return result > 0
    }

    fun getAllHosts(): List<HostMachineInfo> {
        val result = macHostMachineDao.findAll(dslContext)
        return result?.map { tranferHostInfo(it) } ?: listOf()
    }

    fun searchHosts(
        name: String?,
        ip: String?
    ): List<HostMachineInfo> {
        val result = macHostMachineDao.search(dslContext, name, ip)
        return result?.map { tranferHostInfo(it) } ?: listOf()
    }

    fun get(hostId: Int): HostMachineInfo? {
        val result = macHostMachineDao.get(dslContext, hostId)
        return if (result != null) {
            tranferHostInfo(result)
        } else {
            null
        }
    }

    fun delete(hostId: Int): Boolean {
        val result = macHostMachineDao.delete(dslContext, hostId)
        return result > 0
    }

    private fun tranferHostInfo(rec: TMacHostMachineRecord): HostMachineInfo {
        val info = HostMachineInfo(rec.id)
        info.name = rec.name
        info.ip = rec.ip
        info.userName = rec.userName
        info.password = "******"
        info.createTime = rec.createTime.timestampmilli()
        info.updateTime = rec.updateTime.timestampmilli()
        return info
    }
}
