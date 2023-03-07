package com.tencent.devops.dispatch.windows.service

import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.dispatch.windows.dao.WindowsTypeDao
import com.tencent.devops.dispatch.windows.pojo.VMType
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
open class WindowsTypeService  @Autowired constructor(
    private val dslContext: DSLContext,
    private val windowsTypeDao: WindowsTypeDao
) {

    // 查询对应的windows构建机型
    fun searchVmTypes(systemVersion: String?): List<VMType>? {
        val recordList = windowsTypeDao.search(dslContext, systemVersion)
        return if (recordList != null && recordList.isNotEmpty) {
            val resultList = mutableListOf<VMType>()
            recordList.forEach { record ->
                resultList.add(
                    VMType(
                        id = record.id,
                        name = record.name,
                        systemVersion = record.systemVersion,
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
    // 列出当前已有的系统版本下拉表信息
    fun listSystemVersion(): List<VMType> {
        val recordList = windowsTypeDao.listVmType(dslContext)
        return if (recordList != null && recordList.isNotEmpty) {
            val resultList = mutableListOf<VMType>()
            recordList.forEach { record ->
                resultList.add(
                    VMType(
                        id = record.id,
                        name = record.name,
                        systemVersion = record.systemVersion,
                        createTime = record.createTime.timestampmilli(),
                        updateTime = record.updateTime.timestampmilli()
                    )
                )
            }
            resultList
        } else {
            mutableListOf()
        }
    }
}
