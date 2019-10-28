package com.tencent.devops.store.service.common

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.common.ReasonDao
import com.tencent.devops.store.dao.common.ReasonRelDao
import com.tencent.devops.store.pojo.common.Reason
import com.tencent.devops.store.pojo.common.ReasonReq
import com.tencent.devops.store.pojo.common.enums.ReasonTypeEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 原因定义类
 * since: 2019-03-22
 */
@Service
class ReasonService @Autowired constructor(
    private val dslContext: DSLContext,
    private val reasonDao: ReasonDao,
    private val reasonRelDao: ReasonRelDao
) {

    private val logger = LoggerFactory.getLogger(ReasonService::class.java)

    fun list(
        type: ReasonTypeEnum,
        enable: Boolean?
    ): Result<List<Reason>> {
        val reasons = reasonDao.list(dslContext, type.type, enable).map { reasonDao.convert(it) }

        return Result(reasons)
    }

    fun add(
        userId: String,
        type: ReasonTypeEnum,
        reasonReq: ReasonReq
    ): Result<Boolean> {
        logger.info("add reason, $userId | $reasonReq")

        val id = UUIDUtil.generate()
        reasonDao.add(dslContext, id, userId, type.type, reasonReq)
        return Result(true)
    }

    fun update(
        userId: String,
        id: String,
        reasonReq: ReasonReq
    ): Result<Boolean> {
        logger.info("update reason, $userId | $reasonReq | $id")

        reasonDao.update(dslContext, id, userId, reasonReq)
        return Result(true)
    }

    fun enable(
        userId: String,
        id: String,
        enable: Boolean
    ): Result<Boolean> {
        logger.info("enable reason, $userId | $enable | $id")

        reasonDao.enable(dslContext, id, userId, enable)
        return Result(true)
    }

    fun delete(
        userId: String,
        id: String
    ): Result<Boolean> {
        logger.info("delete reason, $userId | $id")

        val reason = reasonDao.get(dslContext, id) ?: return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_INVALID, arrayOf(id))

        // 若已被使用，不允许删除
        val reasonContent = reason["CONTENT"] as String
        if (reasonRelDao.isUsed(dslContext, id)) {
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_ATOM_UNINSTALL_REASON_USED, arrayOf(reasonContent))
        }

        reasonDao.delete(dslContext, id)
        return Result(true)
    }
}