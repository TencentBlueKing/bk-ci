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
        private const val BATCH_SIZE = 100
        private val logger = LoggerFactory.getLogger(TxUserStorePublishersServiceImpl::class.java)
        private val HAS_VERSION_LOG =
            setOf(StoreTypeEnum.SERVICE, StoreTypeEnum.ATOM, StoreTypeEnum.IMAGE, StoreTypeEnum.IDE_ATOM)
    }


    override fun updateComponentFirstPublisher(type: StoreTypeEnum): Boolean {
        var sucessFlag = true
        //矫正组件首次发布人数据
        val totalResults = modifyComponentFirstPublisher(type)

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


    private fun modifyComponentFirstPublisher(storeTypeEnum: StoreTypeEnum): List<Future<Boolean>> {
        val executorService = Executors.newFixedThreadPool(5)
        val count = countComponentPublisherByStoreType(storeTypeEnum)
        var offset = 0
        val totalResults = mutableListOf<Future<Boolean>>()
        try {
            while (offset < count) {


                val storeList = listComponentByStoreType(storeTypeEnum, offset)
                if (storeList.isNullOrEmpty()) {
                    break
                }

                offset += BATCH_SIZE
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

                if (!componentList.isNullOrEmpty()) {
                    val codes = componentList.map { it.code }
                    val storeReleaseList = txUserStorePublishersDao.selectStoreReleaseInfoByStoreCodes(
                        dslContext,
                        codes,
                        storeTypeEnum.type.toByte()
                    )

                    if (!storeReleaseList.isNullOrEmpty()) {
                        val results = mutableListOf<Future<Boolean>>()
                        try {
                            componentList.forEach {
                                val future = executorService.submit<Boolean> {
                                    try {
                                        val storeRelease =
                                            storeReleaseList.find { storeRelease ->
                                                storeRelease.storeCode == it.code
                                                        && storeRelease.storeType == storeTypeEnum.type.toByte()
                                            }
                                        if (storeRelease != null && it.modifier != storeRelease.firstPubCreator) {
                                            txUserStorePublishersDao.updateComponentFirstPublisher(
                                                dslContext = dslContext,
                                                storeCode = it.code,
                                                storeType = storeTypeEnum.type.toByte(),
                                                firstPublisher = it.modifier
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
                        } catch (e: Exception) {
                            logger.error("update${storeTypeEnum.name}FirstPublisherIfNecessary error:${e.message}")
                            throw RuntimeException(e.message)
                        }

                    }

                }


            }

        } catch (e: Exception) {
            logger.error("modify${storeTypeEnum.name}FirstPublisher error:${e.message}")
            throw RuntimeException(e.message)
        } finally {
            executorService.shutdown()
        }

        return totalResults

    }


    private fun countComponentPublisherByStoreType(storeTypeEnum: StoreTypeEnum): Int {
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


    private fun listComponentByStoreType(
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





