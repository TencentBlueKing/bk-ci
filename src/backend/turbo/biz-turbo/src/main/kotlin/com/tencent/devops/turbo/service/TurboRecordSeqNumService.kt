package com.tencent.devops.turbo.service

import com.tencent.devops.turbo.dao.mongotemplate.TurboRecordSeqNumDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TurboRecordSeqNumService @Autowired constructor(
    private val turboRecordSeqNumDao: TurboRecordSeqNumDao
) {

    /**
     * 获取项目维度的最新序号
     */
    fun getLatestSeqNum(projectId: String): Int {
        return turboRecordSeqNumDao.increaseSeqNum(projectId)
    }
}
