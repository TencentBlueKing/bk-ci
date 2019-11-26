package com.tencent.devops.process.engine.atom.vm.parser

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.DockerVersion
import com.tencent.devops.common.pipeline.type.DispatchType
import com.tencent.devops.common.pipeline.type.StoreDispatchType
import com.tencent.devops.common.pipeline.type.devcloud.PublicDevCloudDispathcType
import com.tencent.devops.common.pipeline.type.docker.DockerDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.pipeline.type.idc.IDCDispatchType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

/**
 * @Description
 * @Date 2019/11/17
 * @Version 1.0
 */
@Component
@Primary
class DispatchTypeParserTxImpl @Autowired constructor(
    @Qualifier(value = "commonDispatchTypeParser")
    private val commonDispatchTypeParser: DispatchTypeParser
) : DispatchTypeParser {

    private val logger = LoggerFactory.getLogger(DispatchTypeParserTxImpl::class.java)

    override fun parse(userId: String, projectId: String, dispatchType: DispatchType) {
        if (dispatchType is StoreDispatchType) {
            if (dispatchType.imageType == ImageType.BKSTORE) {
                //一般性处理
                commonDispatchTypeParser.parse(userId, projectId, dispatchType)
                //腾讯内部版专有处理
                if (dispatchType.imageType == ImageType.BKDEVOPS) {
                    if (dispatchType is DockerDispatchType) {
                        dispatchType.dockerBuildVersion = dispatchType.value.removePrefix("paas/")
                    } else if (dispatchType is PublicDevCloudDispathcType) {
                        dispatchType.image = dispatchType.value.removePrefix("devcloud/")
                    } else if (dispatchType is IDCDispatchType) {
                        dispatchType.image = dispatchType.value.removePrefix("paas/")
                    }
                } else {
                    //第三方镜像
                    if (dispatchType is PublicDevCloudDispathcType) {
                        dispatchType.image = dispatchType.value
                    } else if (dispatchType is IDCDispatchType) {
                        dispatchType.image = dispatchType.value
                    } else {
                        dispatchType.dockerBuildVersion = dispatchType.value
                    }
                }
            } else if (dispatchType.imageType == ImageType.BKDEVOPS) {
                //针对非商店的旧数据处理
                if (dispatchType.value != DockerVersion.TLINUX1_2.value && dispatchType.value != DockerVersion.TLINUX2_2.value) {
                    dispatchType.dockerBuildVersion = "bkdevops/" + dispatchType.value
                    dispatchType.value = "bkdevops/" + dispatchType.value
                } else {
                    //TLINUX1.2/2.2需要后续做特殊映射
                }
            }
            logger.info("DispatchTypeParserTxImpl:AfterTransfer:dispatchType=(${JsonUtil.toJson(dispatchType)})")
        } else {
            logger.info("DispatchTypeParserTxImpl:not StoreDispatchType, no transfer")
        }
    }
}