package com.tencent.devops.store.common.service.impl

import com.tencent.devops.store.atom.dao.MarketAtomDao
import com.tencent.devops.store.common.dao.StoreReleaseDao
import com.tencent.devops.store.common.service.StoreComponentDataCorrectionService
import com.tencent.devops.store.image.dao.MarketImageDao
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.template.dao.MarketTemplateDao
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service


@Service
class StoreCoreComponentDataCorrectionServiceImpl : StoreComponentDataCorrectionService() {
    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var atomDao: MarketAtomDao

    @Autowired
    lateinit var templateDao: MarketTemplateDao

    @Autowired
    lateinit var imageDao: MarketImageDao


    @Autowired
    lateinit var storeReleaseDao: StoreReleaseDao







    override fun updateComponentFirstPublisher(userId: String?) {

        try {
            val atomList = atomDao.listByAtomCode(dslContext)?.map { Component(it.atomCode, it.modifier) }
            updateFirstPublisherIfNecessary(StoreTypeEnum.ATOM, atomList, storeReleaseDao, dslContext,userId)

            val templateList = templateDao.listByTemplateCode(dslContext)?.map { Component(it.templateCode, it.modifier) }
            updateFirstPublisherIfNecessary(StoreTypeEnum.TEMPLATE, templateList, storeReleaseDao, dslContext,userId)

            val imageList = imageDao.listByImageCode(dslContext)?.map { Component(it.imageCode, it.modifier) }
            updateFirstPublisherIfNecessary(StoreTypeEnum.IMAGE, imageList, storeReleaseDao, dslContext,userId)
        }catch (e: Exception){
            logger.info("updateComponentFirstPublisher error:${e.message}")
            throw RuntimeException("updateComponentFirstPublisher error")
        }

    }


    fun updateFirstPublisherIfNecessary(
        storeTypeEnum: StoreTypeEnum,
        list: List<Component>?,
        dao: StoreReleaseDao,
        dslContext: DSLContext,
        userId: String?
    ) {

        if (!list.isNullOrEmpty()) {
            val codes = list.map { it.code }
            val storeReleaseList = storeReleaseDao.selectStoreReleaseInfo(
                dslContext,
                codes,
                storeTypeEnum.type.toByte()
            )

            if (!storeReleaseList.isNullOrEmpty()) {
                list.forEach {
                    val storeRelease = storeReleaseList.find { storeRelease -> storeRelease.storeCode == it.code && storeRelease.storeType == storeTypeEnum.type.toByte()}
                    if (storeRelease != null && it.modifier != storeRelease.firstPubCreator) {
                        dslContext.transaction { configuration ->
                            val transactionContext = DSL.using(configuration)
                            dao.updateComponentFirstPublisher(
                                dslContext = transactionContext,
                                storeCode = it.code,
                                storeType = storeTypeEnum.type.toByte(),
                                firstPublisher = it.modifier,
                                userId = userId
                            )
                        }

                    }
                }

            }

        }


    }


    class Component(
        val code: String,
        val modifier: String
    )


    companion object {
        private val logger = LoggerFactory.getLogger(StoreCoreComponentDataCorrectionServiceImpl::class.java)
    }

}



