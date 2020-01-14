package com.tencent.devops.store.service.common.impl

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.store.dao.common.StoreMediaInfoDao
import com.tencent.devops.store.pojo.common.StoreMediaInfo
import com.tencent.devops.store.pojo.common.StoreMediaInfoRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreMediaService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
abstract class StoreMedaiServiceImpl : StoreMediaService{

    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var storeMediaInfoDao: StoreMediaInfoDao

    override fun add(userId: String, type: StoreTypeEnum, storeMediaInfo: StoreMediaInfoRequest): Result<Boolean> {
        logger.info("addMedia input: userId[$userId] type[$type] storeMediaInfo:[$storeMediaInfo]")
        storeMediaInfoDao.add(
            dslContext = dslContext,
            id =  UUIDUtil.generate(),
            userId = userId,
            type = type.type.toByte(),
            storeMediaInfoReq = storeMediaInfo
        )
        return Result(true)
    }

    override fun update(
        userId: String,
        id: String,
        storeMediaInfo: StoreMediaInfoRequest
    ): Result<Boolean> {
        logger.info("updateMedia input: userId[$userId] id[$id] storeMediaInfo:[$storeMediaInfo]")
        storeMediaInfoDao.updateById(
            dslContext = dslContext,
            id =  id,
            userId = userId,
            storeMediaInfoReq = storeMediaInfo
        )
        return Result(true)
    }

    override fun get(userId: String, id: String): Result<StoreMediaInfo?> {
        logger.info("getMedia input: userId[$userId] id[$id] ")
        val storeMediaRecord = storeMediaInfoDao.getMediaInfo(
            dslContext = dslContext,
            id = id
        )
        return Result(if (storeMediaRecord == null) {
            null
        } else {
            storeMediaInfoDao.convert(storeMediaRecord)
        })
    }

    override fun getByCode(storeCode: String, storeType: StoreTypeEnum): Result<List<StoreMediaInfo>?> {
        logger.info("getMedia input: storeCode[$storeCode] storeType[$storeType] ")
        var storeMediaInfoList = mutableListOf<StoreMediaInfo>()
        val storeMediaRecord = storeMediaInfoDao.getMediaInfoByStoreCode(
            dslContext = dslContext,
            storeCode = storeCode,
            type = storeType.type.toByte()
        )
        return if(storeMediaInfoList == null){
            Result(emptyList<StoreMediaInfo>())
        }else{
            storeMediaRecord!!.forEach {
                storeMediaInfoList.add(storeMediaInfoDao.convert(it))
            }
            Result(storeMediaInfoList)
        }
    }

    companion object{
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}