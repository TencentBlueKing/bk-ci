package com.tencent.devops.store.common.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.store.pojo.common.InstallStoreReq
import com.tencent.devops.store.pojo.common.InstalledPkgFileShaContentRequest
import com.tencent.devops.store.pojo.common.StoreBaseInfo
import com.tencent.devops.store.pojo.common.StoreBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.common.UnInstallReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.StoreDeleteRequest

interface StoreComponentManageService {

    /**
     * 更新组件基本信息
     */
    fun updateComponentBaseInfo(
        userId: String,
        storeType: String,
        storeCode: String,
        storeBaseInfoUpdateRequest: StoreBaseInfoUpdateRequest,
        checkPermissionFlag: Boolean = true
    ): Result<Boolean>

    /**
     * 安裝组件
     */
    fun installComponent(
        userId: String,
        channelCode: ChannelCode,
        installStoreReq: InstallStoreReq
    ): Result<Boolean>

    /**
     * 卸載项目组件
     */
    fun uninstallComponent(
        userId: String,
        projectCode: String,
        storeType: String,
        storeCode: String,
        unInstallReq: UnInstallReq
    ): Result<Boolean>

    /**
     * 删除组件
     */
    fun deleteComponent(userId: String, handlerRequest: StoreDeleteRequest): Result<Boolean>

    /**
     * 校验下载组件的权限
     */
    fun validateComponentDownloadPermission(
        storeCode: String,
        storeType: StoreTypeEnum,
        version: String,
        projectCode: String,
        userId: String,
        instanceId: String? = null
    ): Result<StoreBaseInfo?>

    /**
     * 更新组件已安装包sha1摘要值
     */
    fun updateComponentInstalledPkgShaContent(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String,
        installedPkgFileShaContentRequest: InstalledPkgFileShaContentRequest
    ): Result<Boolean>

    /**
     * 更改组件授权人信息
     */
    fun updateStoreRepositoryAuthorizer(
        userId: String,
        storeType: StoreTypeEnum,
        storeCode: String
    ): Result<Boolean>
}
