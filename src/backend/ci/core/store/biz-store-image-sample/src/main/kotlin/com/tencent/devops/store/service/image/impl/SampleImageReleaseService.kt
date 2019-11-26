package com.tencent.devops.store.service.image.impl

import com.tencent.devops.common.api.constant.BEGIN
import com.tencent.devops.common.api.constant.CHECK
import com.tencent.devops.common.api.constant.COMMIT
import com.tencent.devops.common.api.constant.DOING
import com.tencent.devops.common.api.constant.END
import com.tencent.devops.common.api.constant.FAIL
import com.tencent.devops.common.api.constant.NUM_FIVE
import com.tencent.devops.common.api.constant.NUM_FOUR
import com.tencent.devops.common.api.constant.NUM_ONE
import com.tencent.devops.common.api.constant.NUM_THREE
import com.tencent.devops.common.api.constant.NUM_TWO
import com.tencent.devops.common.api.constant.SUCCESS
import com.tencent.devops.common.api.constant.TEST
import com.tencent.devops.common.api.constant.UNDO
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.pojo.common.ReleaseProcessItem
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import com.tencent.devops.store.service.image.ImageReleaseService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SampleImageReleaseService @Autowired constructor() : ImageReleaseService() {
    override fun getPassTestStatus(isNormalUpgrade: Boolean): Byte {
        //开源版不审核直接发布
        return ImageStatusEnum.RELEASED.status.toByte()
    }

    override fun handleProcessInfo(isNormalUpgrade: Boolean, status: Int): List<ReleaseProcessItem> {
        val processInfo = initProcessInfo()
        val totalStep = NUM_FIVE
        when (status) {
            ImageStatusEnum.INIT.status, ImageStatusEnum.COMMITTING.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_TWO, DOING)
            }
            ImageStatusEnum.CHECKING.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_THREE, DOING)
            }
            ImageStatusEnum.CHECK_FAIL.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_THREE, FAIL)
            }
            ImageStatusEnum.TESTING.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_FOUR, DOING)
            }
            ImageStatusEnum.RELEASED.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_FIVE, SUCCESS)
            }
        }
        return processInfo
    }

    /**
     * 初始化镜像版本进度
     */
    private fun initProcessInfo(): List<ReleaseProcessItem> {
        val processInfo = mutableListOf<ReleaseProcessItem>()
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(BEGIN), BEGIN, NUM_ONE, SUCCESS))
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(COMMIT), COMMIT, NUM_TWO, UNDO))
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(CHECK), CHECK, NUM_THREE, UNDO))
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(TEST), TEST, NUM_FOUR, UNDO))
        processInfo.add(ReleaseProcessItem(MessageCodeUtil.getCodeLanMessage(END), END, NUM_FIVE, UNDO))
        return processInfo
    }
}
