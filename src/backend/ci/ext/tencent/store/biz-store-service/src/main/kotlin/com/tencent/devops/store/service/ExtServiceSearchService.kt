package com.tencent.devops.store.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.dao.ExtServiceDao
import com.tencent.devops.store.dao.ExtServiceLableRelDao
import com.tencent.devops.store.dao.common.LabelDao
import com.tencent.devops.store.dao.common.StoreStatisticDao
import com.tencent.devops.store.pojo.vo.ExtServiceMainItemVo
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ExtServiceSearchService @Autowired constructor(
    val dslContext: DSLContext,
    val extServiceDao: ExtServiceDao,
    val storeStatisticDao: StoreStatisticDao,
    val lableDao: LabelDao,
    val extServiceLableRelDao: ExtServiceLableRelDao
) {
    fun mainPageList(
        userId: String,
        page: Int?,
        pageSize: Int?
    ): Result<List<ExtServiceMainItemVo>> {
        return Result(emptyList())
    }

    companion object{
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}