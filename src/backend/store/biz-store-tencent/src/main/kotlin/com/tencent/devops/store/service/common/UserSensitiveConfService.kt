package com.tencent.devops.store.service.common

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.AESUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.common.SensitiveConfDao
import com.tencent.devops.store.pojo.common.SensitiveConfReq
import com.tencent.devops.store.pojo.common.SensitiveConfResp
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

@Service
class UserSensitiveConfService @Autowired constructor(
    private val dslContext: DSLContext,
    private val sensitiveConfDao: SensitiveConfDao
) {
    private val logger = LoggerFactory.getLogger(StoreVisibleDeptService::class.java)

    @Value("\${aes.aesKey}")
    private lateinit var aesKey: String

    @Value("\${aes.aesMock}")
    private lateinit var aesMock: String

    /**
     * 新增敏感配置
     */
    fun create(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        sensitiveConfReq: SensitiveConfReq
    ): Result<Boolean> {
        logger.info("create: $storeType | $storeCode | $sensitiveConfReq")
        val fieldName = sensitiveConfReq.fieldName
        if (fieldName.isEmpty()) return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_NULL, arrayOf(fieldName), false)
        val fieldValue = sensitiveConfReq.fieldValue
        if (fieldValue.isEmpty()) return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_NULL, arrayOf(fieldValue), false)
        // 判断同名
        val isNameExist = sensitiveConfDao.check(dslContext, storeCode, storeType.type.toByte(), fieldName, null)
        logger.info("fieldName: $fieldName, isNameExist: $isNameExist")
        if (isNameExist) {
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_SENSITIVE_CONF_EXIST, arrayOf(fieldName), false)
        }
        // 对字段值进行加密
        val fieldValueEncrypted = AESUtil.encrypt(aesKey, fieldValue)
        logger.info("fieldValue: $fieldValueEncrypted")
        sensitiveConfDao.create(
            dslContext = dslContext,
            userId = userId,
            id = UUIDUtil.generate(),
            storeCode = storeCode,
            storeType = storeType.type.toByte(),
            fieldName = fieldName,
            fieldValue = fieldValueEncrypted,
            fieldDesc = sensitiveConfReq.fieldDesc
            )
        return Result(true)
    }

    /**
     * 更新配置信息
     */
    fun update(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        id: String,
        sensitiveConfReq: SensitiveConfReq
    ): Result<Boolean> {
        logger.info("update: $storeType | $storeCode | $id | $sensitiveConfReq")
        val fieldName = sensitiveConfReq.fieldName
        if (fieldName.isEmpty()) return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_NULL, arrayOf(fieldName), false)
        val fieldValue = sensitiveConfReq.fieldValue
        if (fieldValue.isEmpty()) return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_NULL, arrayOf(fieldValue), false)
        // 判断同名
        val isNameExist = sensitiveConfDao.check(dslContext, storeCode, storeType.type.toByte(), fieldName, id)
        logger.info("fieldName: $fieldName, isNameExist: $isNameExist")
        if (isNameExist) {
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_SENSITIVE_CONF_EXIST, arrayOf(fieldName), false)
        }
        // 对字段值进行加密
        val fieldValueEncrypted = if (fieldValue == aesMock) {
            null
        } else {
            AESUtil.encrypt(aesKey, fieldValue)
        }
        logger.info("fieldValue: $fieldValueEncrypted")
        sensitiveConfDao.update(
            dslContext = dslContext,
            userId = userId,
            id = id,
            fieldName = fieldName,
            fieldValue = fieldValueEncrypted,
            fieldDesc = sensitiveConfReq.fieldDesc
        )
        return Result(true)
    }

    /**
     * 删除
     */
    fun delete(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        ids: String
    ): Result<Boolean> {
        logger.info("delete: $userId | $storeType | $storeCode | $ids")
        sensitiveConfDao.batchDelete(dslContext, storeType.type.toByte(), storeCode, ids.split(","))
        return Result(true)
    }

    /**
     * 获取单个数据
     */
    fun get(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        id: String
    ): Result<SensitiveConfResp?> {
        val record = sensitiveConfDao.getById(dslContext, id)
        logger.info("the record is :$record")
        val df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return Result(if (null != record) {
            SensitiveConfResp(
                fieldId = record["ID"] as String,
                fieldName = record["FIELD_NAME"] as String,
                fieldValue = aesMock,
                fieldDesc = record["FIELD_DESC"] as? String,
                creator = record["CREATOR"] as String,
                modifier = record["MODIFIER"] as String,
                createTime = df.format(record["CREATE_TIME"] as TemporalAccessor),
                updateTime = df.format(record["UPDATE_TIME"] as TemporalAccessor)
            )
        } else {
            null
        })
    }

    /**
     * 获取列表
     */
    fun list(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        isDecrypt: Boolean
    ): Result<List<SensitiveConfResp>?> {
        val records = sensitiveConfDao.list(dslContext, storeType.type.toByte(), storeCode)
        val sensitiveConfRespList = mutableListOf<SensitiveConfResp>()
        val df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        records?.forEach {
            val fieldValue = if (isDecrypt) AESUtil.decrypt(aesKey, it["FIELD_VALUE"] as String) else aesMock
            sensitiveConfRespList.add(
                SensitiveConfResp(
                    fieldId = it["ID"] as String,
                    fieldName = it["FIELD_NAME"] as String,
                    fieldValue = fieldValue,
                    fieldDesc = it["FIELD_DESC"] as? String,
                    creator = it["CREATOR"] as String,
                    modifier = it["MODIFIER"] as String,
                    createTime = df.format(it["CREATE_TIME"] as TemporalAccessor),
                    updateTime = df.format(it["UPDATE_TIME"] as TemporalAccessor)
                )
            )
        }
        logger.info("getSensitiveConfRespList storeType: $storeType,storeCode: $storeCode,sensitiveConfRespList: $sensitiveConfRespList")
        return Result(sensitiveConfRespList)
    }
}