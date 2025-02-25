package com.tencent.devops.store.common.service.impl

import com.tencent.devops.model.store.tables.records.TStoreReleaseRecord
import com.tencent.devops.store.common.dao.TxUserStorePublishersDao
import com.tencent.devops.store.common.service.TxUserStorePublishersService
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.jooq.Record2
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.ExecutorService
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
        private var successUpdateCount = 0
    }


    override fun updateComponentFirstPublisher(type: StoreTypeEnum) {
        val startTime = System.currentTimeMillis()
        logger.info("start to update component first publisher,time:${startTime}")
        successUpdateCount = 0
        modifyComponentFirstPublisher(type)
        val endTime = System.currentTimeMillis()
        logger.info("update component first publisher end,time:${endTime}")
        logger.info("update total cost ${endTime - startTime}")
        logger.info("successUpdateCount: $successUpdateCount")

    }


    private fun modifyComponentFirstPublisher(storeTypeEnum: StoreTypeEnum) {

        var offset = 0
        try {
            val count = countComponentPublisherByStoreType(storeTypeEnum)

            while (offset < count) {


                val storeList = listComponentByStoreType(storeTypeEnum, offset)
                if (storeList.isNullOrEmpty()) {
                    break
                }

                offset += BATCH_SIZE
                val componentList = getComponentList(storeTypeEnum, storeList)
                updateFirstPublisherIfNecessary(
                    storeTypeEnum = storeTypeEnum,
                    list = componentList
                )

            }

        } catch (ignored: Throwable) {
            logger.error("modify${storeTypeEnum.name}FirstPublisher error:${ignored},offset:${offset},has been modified $successUpdateCount")
            throw RuntimeException("modify${storeTypeEnum.name}FirstPublisher error:${ignored}")
        }


    }


    fun updateFirstPublisherIfNecessary(
        storeTypeEnum: StoreTypeEnum,
        list: List<Component>?
    ) {
        if (!list.isNullOrEmpty()) {

            val codes = list.map { it.code }
            val storeReleaseList = txUserStorePublishersDao.selectStoreReleaseInfoByStoreCodes(
                dslContext,
                codes,
                storeTypeEnum.type.toByte()
            )

            if (!storeReleaseList.isNullOrEmpty()) {

                updatePublishers(
                    storeTypeEnum = storeTypeEnum,
                    componentList = list,
                    storeReleaseList = storeReleaseList
                )
            }
        }
    }


    private fun updatePublishers(
        storeTypeEnum: StoreTypeEnum,
        componentList: List<Component>,
        storeReleaseList: List<TStoreReleaseRecord>
    ) {
        for (component in componentList) {


            val storeRelease =
                storeReleaseList.find {
                    it.storeCode == component.code &&
                            it.storeType == storeTypeEnum.type.toByte()
                }
            if (storeRelease != null && component.modifier != storeRelease.firstPubCreator) {
                txUserStorePublishersDao.updateComponentFirstPublisher(
                    dslContext = dslContext,
                    storeCode = component.code,
                    storeType = storeTypeEnum.type.toByte(),
                    firstPublisher = component.modifier
                )
                successUpdateCount += 1
            }


        }


    }


    private fun getComponentList(
        storeTypeEnum: StoreTypeEnum,
        storeList: List<Record2<String, String>>
    ): List<Component> {

        val componentIds = storeList.map { it.value1() }
        val versionLogList = txUserStorePublishersDao.queryModifierByAtomId(dslContext, componentIds, storeTypeEnum)

        return versionLogList?.takeIf { it.isNotEmpty }?.let { logs ->
            storeList.mapNotNull { store ->
                logs.find { it.value2() == store.value1() }?.let { versionLog ->
                    Component(store.value2(), versionLog.value1())
                }
            }
        } ?: emptyList()

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





