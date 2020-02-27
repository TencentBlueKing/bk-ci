package com.tencent.devops.store.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.dao.ExtServiceLableRelDao
import com.tencent.devops.store.pojo.common.Label
import com.tencent.devops.store.service.common.LabelService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ExtServiceLableService @Autowired constructor(
    private val dslContext: DSLContext,
    private val labelService: LabelService,
    private val lableRelDao: ExtServiceLableRelDao
) {

    fun getLabelsByServiceId(serviceId: String): Result<List<Label>?> {
        logger.info("the serviceId is :$serviceId")
        val serviceLabelList = mutableListOf<Label>()
        val serviceLabelRecords = lableRelDao.getLabelsByServiceId(dslContext, serviceId) // 查询插件标签信息
        serviceLabelRecords?.forEach {
            labelService.addLabelToLabelList(it, serviceLabelList)
        }
        return Result(serviceLabelList)
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}