package com.tencent.devops.store.service.image

import com.tencent.devops.store.dao.image.MarketImageDao
import com.tencent.devops.store.service.common.impl.StoreMemberServiceImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("imageMemberService")
class ImageMemberService : StoreMemberServiceImpl() {

    @Autowired
    lateinit var marketImageDao: MarketImageDao

    override fun getStoreName(storeCode: String): String {
        return marketImageDao.getLatestImageByCode(dslContext, storeCode)?.imageName ?: ""
    }
}