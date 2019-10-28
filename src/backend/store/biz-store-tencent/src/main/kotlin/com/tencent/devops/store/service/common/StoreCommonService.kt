package com.tencent.devops.store.service.common

import com.tencent.devops.common.api.constant.INIT_VERSION
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.dao.atom.MarketAtomDao
import com.tencent.devops.store.dao.template.MarketTemplateDao
import com.tencent.devops.store.pojo.atom.enums.ReleaseTypeEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * store公共
 * author: fayewang
 * since: 2019-07-23
 */
@Service
class StoreCommonService @Autowired constructor(
    private val dslContext: DSLContext,
    private val marketAtomDao: MarketAtomDao,
    private val templateDao: MarketTemplateDao

) {
    private val logger = LoggerFactory.getLogger(StoreCommonService::class.java)

    fun getStoreNameById(
        storeId: String,
        storeType: StoreTypeEnum
    ): String {
        logger.info("getStoreNameById: $storeId | $storeType")
        when (storeType.type) {
            StoreTypeEnum.ATOM.type -> {
                val atom = marketAtomDao.getAtomById(dslContext, storeId)
                return if (atom == null) {
                    ""
                } else {
                    atom["name"] as String
                }
            }
            StoreTypeEnum.TEMPLATE.type -> {
                val template = templateDao.getTemplate(dslContext, storeId)
                return if (template == null) {
                    ""
                } else {
                    template["TEMPLATE_NAME"] as String
                }
            }
            else -> return ""
        }
    }

    fun getRequireVersion(
        dbVersion: String,
        releaseType: ReleaseTypeEnum
    ): String {
        var requireVersion = INIT_VERSION
        val dbVersionParts = dbVersion.split(".")
        when (releaseType) {
            ReleaseTypeEnum.INCOMPATIBILITY_UPGRADE -> {
                requireVersion = "${dbVersionParts[0].toInt() + 1}.0.0"
            }
            ReleaseTypeEnum.COMPATIBILITY_UPGRADE -> {
                requireVersion = "${dbVersionParts[0]}.${dbVersionParts[1].toInt() + 1}.0"
            }
            ReleaseTypeEnum.COMPATIBILITY_FIX -> {
                requireVersion = "${dbVersionParts[0]}.${dbVersionParts[1]}.${dbVersionParts[2].toInt() + 1}"
            }
            ReleaseTypeEnum.CANCEL_RE_RELEASE -> {
                requireVersion = dbVersion
            }
            else -> {
            }
        }
        return requireVersion
    }
}
