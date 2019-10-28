package com.tencent.devops.store.service.ideatom

import com.tencent.devops.store.dao.ideatom.IdeAtomDao
import com.tencent.devops.store.service.common.AbstractClassifyService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("IDE_ATOM_CLASSIFY_SERVICE")
class MarketIdeAtomClassifyService : AbstractClassifyService() {

    private val logger = LoggerFactory.getLogger(MarketIdeAtomClassifyService::class.java)

    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var ideAtomDao: IdeAtomDao

    override fun getDeleteClassifyFlag(classifyId: String): Boolean {
        // 允许删除分类是条件：1、该分类下的原子插件都不处于上架状态
        var flag = false
        val releaseAtomNum = ideAtomDao.countReleaseAtomNumByClassifyId(dslContext, classifyId)
        logger.info("the releaseAtomNum is :$releaseAtomNum")
        if (releaseAtomNum == 0) {
            flag = true
        }
        return flag
    }
}
