package com.tencent.devops.store.constant

object StoreCode {
    const val BK_OTHER = "BkOther"//其他
    const val BK_PIPELINED_JOB = "BkPipelinedJob"//流水线Job
    const val BK_IMAGE_STORE_ONLINE = "BkImageStoreOnline"//容器镜像商店上线，历史镜像数据自动生成
    const val BK_OLD_VERSION_BUILD_IMAGE = "BkOldVersionBuildImage"//旧版的构建镜像，通过拷贝为构建镜像入口生成
    const val BK_AUTOMATICALLY_CONVERTED = "BkAutomaticallyConverted"//已自动转换为容器镜像商店数据，请项目管理员在研发商店工作台进行管理。
    const val BK_COPY_FOR_BUILD_IMAGE = "BkCopyForBuildImage"//旧版的构建镜像，通过蓝盾版本仓库“拷贝为构建镜像”入口生成。
    const val BK_AFTER_IMAGE_STORE_ONLINE = "BkAfterImageStoreOnline"//容器镜像商店上线后，旧版入口已下线。因历史原因，此类镜像没有办法对应到实际的镜像推送人，暂时先挂到项目管理员名下。
    const val BK_PROJECT_MANAGER_CAN_OPERATION = "BkProjectManagerCanOperation"//项目管理员可在研发商店工作台进行上架/升级/下架等操作，或者交接给实际负责人进行管理。
    const val BK_HISTORYDATA_DATA = "BkHistorydataData"//historyData数据迁移自动通过
    const val BK_WORKER_BEE_PROJECT_NOT_EXIST = "BkWorkerBeeProjectNotExist"//工蜂项目信息不存在，请检查链接
    const val BK_WORKER_BEE_PROJECT_NOT_STREAM_ENABLED = "BkWorkerBeeProjectNotStreamEnabled"//工蜂项目未开启Stream，请前往仓库的CI/CD进行配置

}