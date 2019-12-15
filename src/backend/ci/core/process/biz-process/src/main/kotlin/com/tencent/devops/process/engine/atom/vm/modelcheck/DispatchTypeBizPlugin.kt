package com.tencent.devops.process.engine.atom.vm.modelcheck

import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.type.StoreDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.process.plugin.ContainerBizPlugin
import com.tencent.devops.process.plugin.annotation.ContainerBiz

/**
 * @Description
 * @Date 2019/12/15
 * @Version 1.0
 */
@ContainerBiz
class DispatchTypeBizPlugin : ContainerBizPlugin<VMBuildContainer> {
    override fun containerClass(): Class<VMBuildContainer> {
        return VMBuildContainer::class.java
    }

    override fun afterCreate(container: VMBuildContainer, projectId: String, pipelineId: String, pipelineName: String, userId: String, channelCode: ChannelCode) {

    }

    override fun beforeDelete(container: VMBuildContainer, userId: String, pipelineId: String?) {

    }

    override fun check(container: VMBuildContainer, appearedCnt: Int) {
        val dispatchType = container.dispatchType
        if (dispatchType is StoreDispatchType) {
            if (dispatchType.imageType == ImageType.BKSTORE) {
                //BKSTORE的镜像确保code与version不为空
                if (dispatchType.imageCode.isNullOrBlank()) {
                    throw IllegalArgumentException("从研发商店选择的镜像code不可为空")
                }
                if (dispatchType.imageVersion.isNullOrBlank()) {
                    throw IllegalArgumentException("从研发商店选择的镜像version不可为空")
                }
            } else {
                //其余类型的镜像确保value不为空
                if (dispatchType.value.isBlank()) {
                    throw IllegalArgumentException("非商店蓝盾源/第三方源的镜像value不可为空")
                }
            }
        }
    }
}