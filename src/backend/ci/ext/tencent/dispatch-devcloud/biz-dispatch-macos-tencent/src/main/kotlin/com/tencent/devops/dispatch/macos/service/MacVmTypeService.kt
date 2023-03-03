package com.tencent.devops.dispatch.macos.service

import com.google.common.base.Preconditions
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.dispatcher.macos.dao.VirtualMachineTypeDao
import com.tencent.devops.dispatch.macos.pojo.VMType
import com.tencent.devops.dispatch.macos.pojo.VMTypeCreate
import com.tencent.devops.dispatch.macos.pojo.VMTypeUpdate
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
open class MacVmTypeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val virtualMachineTypeDao: VirtualMachineTypeDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(MacVmTypeService::class.java)
    }

    fun getVMType(vmTypeId: Int): VMType? {
        val record = virtualMachineTypeDao.getVmType(dslContext, vmTypeId)
        return if (record != null) {
            VMType(
                id = record.id,
                name = record.name,
                systemVersion = record.systemVersion,
                xcodeVersionList = record.xcodeVersion.trim(';').split(";"),
                createTime = record.createTime.timestampmilli(),
                updateTime = record.updateTime.timestampmilli()
            )
        } else {
            null
        }
    }

    fun getSystemVersionByVersion(version: String?): String? {
        return virtualMachineTypeDao.getSystemVersionByVersion(dslContext, version)
    }

    fun listVMType(): List<VMType>? {
        val recordList = virtualMachineTypeDao.listVmType(dslContext)
        return if (recordList != null && recordList.isNotEmpty) {
            val resultList = mutableListOf<VMType>()
            recordList.forEach { record ->
                resultList.add(
                    VMType(
                        id = record.id,
                        name = record.name,
                        systemVersion = record.systemVersion,
                        xcodeVersionList = record.xcodeVersion.trim(';').split(";"),
                        createTime = record.createTime.timestampmilli(),
                        updateTime = record.updateTime.timestampmilli()
                    )
                )
            }
            resultList
        } else {
            null
        }
    }

    fun listSystemVersion(): List<String> {
        val recordList = virtualMachineTypeDao.listVmType(dslContext)
        return if (recordList != null && recordList.isNotEmpty) {
            val systemVersionList = recordList.map {
                it.systemVersion
            }
            return systemVersionList.distinct().sorted()
        } else {
            mutableListOf()
        }
    }

    fun listXcodeVersion(): List<String> {
        val recordList = virtualMachineTypeDao.listVmType(dslContext)
        val xcodeVersionList = mutableListOf<String>()
        return if (recordList != null && recordList.isNotEmpty) {
            recordList.forEach { vmType ->
                xcodeVersionList.addAll(vmType.xcodeVersion.trim(';').split(";").filter { xcodeVersion ->
                    xcodeVersion.isNotBlank() }
                )
            }
            return xcodeVersionList.distinct().sorted()
        } else {
            xcodeVersionList
        }
    }

    fun listXcodeVersion(systemVersion: String?): List<String> {
        val recordList = virtualMachineTypeDao.search(dslContext, systemVersion, null)
        val xcodeVersionList = mutableListOf<String>()
        return if (recordList != null && recordList.isNotEmpty) {
            recordList.forEach { vmType ->
                xcodeVersionList.addAll(vmType.xcodeVersion.trim(';').split(";").filter { xcodeVersion ->
                    xcodeVersion.isNotBlank() }
                )
            }
            return xcodeVersionList.distinct().sorted()
        } else {
            xcodeVersionList
        }
    }

    fun createVMType(vmType: VMTypeCreate): Boolean {
        Preconditions.checkArgument(!vmType.name.isNullOrBlank(), "name can not be null")
        Preconditions.checkArgument(!vmType.systemVersion.isNullOrBlank(), "systemVersion can not be null")
        Preconditions.checkArgument(vmType.xcodeVersionList.isNotEmpty(), "xcodeVersionList can not be null")
        return virtualMachineTypeDao.create(dslContext, vmType)
    }

    fun updateVMType(vmType: VMTypeUpdate): Boolean {
        Preconditions.checkArgument(vmType.id != 0, "id can not be 0")
        Preconditions.checkArgument(!vmType.name.isNullOrBlank(), "name can not be null")
        Preconditions.checkArgument(!vmType.systemVersion.isNullOrBlank(), "systemVersion can not be null")
        Preconditions.checkArgument(!vmType.version.isNullOrBlank(), "version can not be null")
        Preconditions.checkArgument(vmType.xcodeVersionList.isNotEmpty(), "xcodeVersionList can not be null")
        val result = virtualMachineTypeDao.update(dslContext, vmType)
        return result > 0
    }

    fun deleteVMType(vmTypeId: Int): Boolean {
        Preconditions.checkArgument(vmTypeId != 0, "id can not be 0")
        val result = virtualMachineTypeDao.delete(dslContext, vmTypeId)
        return result > 0
    }

    fun searchVmTypes(systemVersion: String?, xcodeVersion: String?): List<VMType>? {
        val recordList = virtualMachineTypeDao.search(dslContext, systemVersion, xcodeVersion)
        return if (recordList != null && recordList.isNotEmpty) {
            val resultList = mutableListOf<VMType>()
            recordList.forEach { record ->
                resultList.add(
                    VMType(
                        id = record.id,
                        name = record.name,
                        systemVersion = record.systemVersion,
                        xcodeVersionList = record.xcodeVersion.trim(';').split(";"),
                        createTime = record.createTime.timestampmilli(),
                        updateTime = record.updateTime.timestampmilli()
                    )
                )
            }
            resultList
        } else {
            null
        }
    }
}
