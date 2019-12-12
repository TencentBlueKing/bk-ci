package com.tencent.devops.store.service.image.impl

import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.image.ImageService
import org.springframework.stereotype.Service

@Service
class SampleImageService : ImageService() {
    override fun batchGetVisibleDept(imageCodeList: List<String>, image: StoreTypeEnum): HashMap<String, MutableList<Int>>? {
        // 开源版本不设置可见范围
        return null
    }

    override fun generateInstallFlag(defaultFlag: Boolean, members: MutableList<String>?, userId: String, visibleList: MutableList<Int>?, userDeptList: List<Int>): Boolean {
        // 开源版镜像默认所有用户都有权限安装
        return true
    }
}
