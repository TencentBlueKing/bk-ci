package com.tencent.devops.dispatch.macos.service

import com.tencent.devops.dispatch.macos.dao.VirtualMachineTypeDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class MacVmTypeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val virtualMachineTypeDao: VirtualMachineTypeDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(MacVmTypeService::class.java)
    }

    fun getSystemVersionByVersion(version: String?): String? {
        return virtualMachineTypeDao.getSystemVersionByVersion(dslContext, version)
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
                xcodeVersionList.addAll(vmType.xcodeVersion.trim(';').split(";")
                                            .filter { xcodeVersion ->
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
                xcodeVersionList.addAll(vmType.xcodeVersion.trim(';').split(";")
                                            .filter { xcodeVersion ->
                    xcodeVersion.isNotBlank() }
                )
            }
            return xcodeVersionList.distinct().sorted()
        } else {
            xcodeVersionList
        }
    }
}
