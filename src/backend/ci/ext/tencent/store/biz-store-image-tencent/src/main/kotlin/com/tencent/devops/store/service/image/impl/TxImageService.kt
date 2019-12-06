package com.tencent.devops.store.service.image.impl

import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreVisibleDeptService
import com.tencent.devops.store.service.image.ImageService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TxImageService : ImageService() {
    @Autowired
    lateinit var storeVisibleDeptService: StoreVisibleDeptService

    private val logger = LoggerFactory.getLogger(TxImageService::class.java)
    override fun generateInstallFlag(defaultFlag: Boolean, members: MutableList<String>?, userId: String, visibleList: MutableList<Int>?, userDeptList: List<Int>): Boolean {
        return if (defaultFlag || (members != null && members.contains(userId))) {
            true
        } else {
            visibleList != null && (visibleList.contains(0) || visibleList.intersect(userDeptList).count() > 0)
        }
    }

    override fun batchGetVisibleDept(imageCodeList: List<String>, image: StoreTypeEnum): HashMap<String, MutableList<Int>>? {
        return storeVisibleDeptService.batchGetVisibleDept(imageCodeList, StoreTypeEnum.IMAGE).data
    }
}
