package com.tencent.devops.store.common.service.impl

import com.tencent.devops.store.common.dao.TxUserStorePublishersDao
import com.tencent.devops.store.common.service.TxUserStorePublishersService
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.jooq.Record2
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.Executors
import java.util.concurrent.Future


@Service
class TxUserStorePublishersServiceImpl : TxUserStorePublishersService {

    @Autowired
    private lateinit var txUserStorePublishersDao: TxUserStorePublishersDao

    @Autowired
    private lateinit var dslContext: DSLContext

    companion object {
        private const val BATCH_SIZE = 500
        private val logger = LoggerFactory.getLogger(TxUserStorePublishersServiceImpl::class.java)
        private val totalResults = mutableListOf<Future<Boolean>>()
        private val HAS_VERSION_LOG =
            setOf(StoreTypeEnum.SERVICE, StoreTypeEnum.ATOM, StoreTypeEnum.IMAGE, StoreTypeEnum.IDE_ATOM)
    }


    override fun updateComponentFirstPublisher(userId: String): Boolean {
        var sucessFlag = true
        //矫正插件首次发布人数据
        modifyCompoentFirstPublisher(userId, StoreTypeEnum.ATOM)
        //矫正模板首次发布人数据
        modifyCompoentFirstPublisher(userId, StoreTypeEnum.TEMPLATE)
        //矫正镜像首次发布人数据
        modifyCompoentFirstPublisher(userId, StoreTypeEnum.IMAGE)
        //修正插件 ide插件首次发布人数据
        modifyCompoentFirstPublisher(userId, StoreTypeEnum.IDE_ATOM)
        //修正服务插件首次发布人数据
        modifyCompoentFirstPublisher(userId, StoreTypeEnum.SERVICE)

        if (totalResults.isEmpty()) {
            sucessFlag = false
        }

        for (totalResult in totalResults) {
            try {
                val result = totalResult.get()
                if (!result) {
                    sucessFlag = false
                    break
                }
            } catch (e: Exception) {
                logger.error("updateComponentFirstPublisher error:${e.message}")
                throw RuntimeException(e.message)
            }

        }

        return sucessFlag

    }


    private fun modifyCompoentFirstPublisher(userId: String, storeTypeEnum: StoreTypeEnum) {

        val count = countCompoentPublisherByStoreType(storeTypeEnum)
        var offset = 0
        try {
            while (offset < count) {


                val storeList = listCompoentByStoreType(storeTypeEnum, offset)
                if (storeList.isNullOrEmpty()) {
                    break
                }


                val componentList = if (storeTypeEnum in HAS_VERSION_LOG) {

                    val atomIds = storeList.map { it.value1() }


                    val versionLogList =
                        txUserStorePublishersDao.queryModifierByAtomId(dslContext, atomIds, storeTypeEnum)

                    versionLogList?.takeIf { it.isNotEmpty }?.let { logs ->
                        storeList.mapNotNull { store ->
                            logs.find { it.value2() == store.value1() }?.let { versionLog ->
                                Component(store.value2(), versionLog.value1())
                            }
                        }
                    }
                } else {
                    storeList.map { Component(it.value1(), it.value2()) }

                }

                updateFirstPublisherIfNecessary(StoreTypeEnum.ATOM, componentList, userId)


            }
            offset += BATCH_SIZE


        } catch (e: Exception) {
            logger.error("modify${storeTypeEnum.name}FirstPublisher error:${e.message}")
            throw RuntimeException(e.message)
        }


    }


    fun updateFirstPublisherIfNecessary(
        storeTypeEnum: StoreTypeEnum,
        list: List<Component>?,
        userId: String?
    ): List<Future<Boolean>>? {


        if (!list.isNullOrEmpty()) {
            val codes = list.map { it.code }
            val storeReleaseList = txUserStorePublishersDao.selectStoreReleaseInfoByStoreCodes(
                dslContext,
                codes,
                storeTypeEnum.type.toByte()
            )

            if (!storeReleaseList.isNullOrEmpty()) {
                val executorService = Executors.newFixedThreadPool(5)
                val results = mutableListOf<Future<Boolean>>()
                try {
                    list.forEach {
                        val future = executorService.submit<Boolean> {
                            try {
                                val storeRelease =
                                    storeReleaseList.find { storeRelease -> storeRelease.storeCode == it.code && storeRelease.storeType == storeTypeEnum.type.toByte() }
                                if (storeRelease != null && it.modifier != storeRelease.firstPubCreator) {
                                    txUserStorePublishersDao.updateComponentFirstPublisher(
                                        dslContext = dslContext,
                                        storeCode = it.code,
                                        storeType = storeTypeEnum.type.toByte(),
                                        firstPublisher = it.modifier,
                                        userId = userId
                                    )

                                }
                                true
                            } catch (e: Exception) {
                                logger.error("update${storeTypeEnum.name}FirstPublisher error:${e.message}")
                                false
                            }
                        }

                        results.add(future)

                    }

                    totalResults.addAll(results)
                    return results
                } catch (e: Exception) {
                    logger.error("update${storeTypeEnum.name}FirstPublisherIfNecessary error:${e.message}")
                    throw RuntimeException(e.message)
                } finally {
                    executorService.shutdown()
                }

            }

        }

        return null

    }


    private fun countCompoentPublisherByStoreType(storeTypeEnum: StoreTypeEnum): Int {
        when (storeTypeEnum) {
            StoreTypeEnum.ATOM -> {
                return txUserStorePublishersDao.countByAtomCode(dslContext)
            }

            StoreTypeEnum.TEMPLATE -> {
                return txUserStorePublishersDao.countByTemplateCode(dslContext)
            }

            StoreTypeEnum.IMAGE -> {
                return txUserStorePublishersDao.countByImageCode(dslContext)
            }

            StoreTypeEnum.IDE_ATOM -> {
                return txUserStorePublishersDao.countByIdeAtomCode(dslContext)
            }

            StoreTypeEnum.SERVICE -> {
                return txUserStorePublishersDao.countByExtService(dslContext)
            }

            else -> {
                return 0
            }

        }

    }


    private fun listCompoentByStoreType(
        storeTypeEnum: StoreTypeEnum,
        offset: Int,
    ): Result<Record2<String, String>>? {

        when (storeTypeEnum) {
            StoreTypeEnum.ATOM -> {
                return txUserStorePublishersDao.listByAtomCode(dslContext, offset, BATCH_SIZE)
            }

            StoreTypeEnum.TEMPLATE -> {
                return txUserStorePublishersDao.listByTemplateCode(dslContext, offset, BATCH_SIZE)
            }

            StoreTypeEnum.IMAGE -> {
                return txUserStorePublishersDao.listByImageCode(dslContext, offset, BATCH_SIZE)
            }

            StoreTypeEnum.IDE_ATOM -> {
                return txUserStorePublishersDao.listByIdeAtomCode(dslContext, offset, BATCH_SIZE)
            }

            StoreTypeEnum.SERVICE -> {
                return txUserStorePublishersDao.listExtServiceCode(dslContext, offset, BATCH_SIZE)
            }

            else -> {
                return null
            }

        }


    }




    class Component(
        val code: String,
        val modifier: String
    )

}





