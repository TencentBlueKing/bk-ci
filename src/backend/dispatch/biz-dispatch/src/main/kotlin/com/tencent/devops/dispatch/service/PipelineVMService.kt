package com.tencent.devops.dispatch.service

import com.tencent.devops.dispatch.dao.PipelineVMDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineVMService @Autowired constructor(
    private val pipelineVMDao: PipelineVMDao,
    private val dslContext: DSLContext
) {

    fun getVMs(pipelineId: String, vmSeqId: Int?): String? {
        return pipelineVMDao.getVMs(dslContext, pipelineId, vmSeqId)
    }

    fun getVMsByPipelines(pipelineId: String, vmSeqId: Int?): String? {
        val vmNames = getVMs(pipelineId, vmSeqId)
        return if (vmNames.isNullOrBlank()) {
            if (vmSeqId == null) {
                null
            } else {
                // If the special vm of the pipeline & vmSeqId is empty, try to get it only by pipelineId
                getVMs(pipelineId, null)
            }
        } else {
            vmNames
        }
    }

    /**
     * vmName separate by ','
     */
    fun setVMs(pipelineId: String, vmNames: String, vmSeqId: Int?) {
        pipelineVMDao.setVMs(dslContext, pipelineId, vmNames, vmSeqId)
    }
}