/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.common.service.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.store.common.dao.StoreBaseEnvExtManageDao
import com.tencent.devops.store.common.dao.StoreBaseEnvManageDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.dao.StoreMemberDao
import com.tencent.devops.store.common.service.StoreArchiveService
import com.tencent.devops.store.common.service.StoreCommonService
import com.tencent.devops.store.common.utils.StoreReleaseUtils
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.StoreBaseEnvDataPO
import com.tencent.devops.store.pojo.common.publication.StoreBaseEnvExtDataPO
import com.tencent.devops.store.pojo.common.publication.StorePkgInfoUpdateRequest
import com.tencent.devops.store.pojo.common.version.VersionModel
import org.apache.commons.codec.digest.DigestUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StoreArchiveServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeMemberDao: StoreMemberDao,
    private val storeBaseQueryDao: StoreBaseQueryDao,
    private val storeBaseEnvManageDao: StoreBaseEnvManageDao,
    private val storeBaseEnvExtManageDao: StoreBaseEnvExtManageDao,
    private val storeCommonService: StoreCommonService
) : StoreArchiveService {

    override fun verifyComponentPackage(
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum,
        version: String,
        releaseType: ReleaseTypeEnum?
    ): Boolean {
        // 校验用户是否是该组件的开发成员
        val flag = storeMemberDao.isStoreMember(
            dslContext = dslContext,
            userId = userId,
            storeCode = storeCode,
            storeType = storeType.type.toByte()
        )
        if (!flag) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PERMISSION_DENIED,
                params = arrayOf(storeCode)
            )
        }
        val storeRecord = storeBaseQueryDao.getComponent(
            dslContext = dslContext,
            storeCode = storeCode,
            version = version,
            storeType = storeType
        )
        var storeName = storeRecord?.name
        if (storeName == null) {
            val storeNewestRecord = storeBaseQueryDao.getNewestComponentByCode(dslContext, storeCode, storeType)
                ?: throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                    params = arrayOf(storeCode)
                )
            storeName = storeNewestRecord.name
        }
        // 不是重新上传的包才需要校验版本号
        if (null != releaseType) {
            storeCommonService.validateStoreVersion(
                storeCode = storeCode,
                storeType = storeType,
                versionInfo = VersionModel(
                    releaseType = releaseType,
                    version = version
                ),
                name = storeName ?: ""
            )
        }
        // 判断是否有资格上传包
        if (storeRecord != null && storeRecord.status !in listOf(
                StoreStatusEnum.INIT.name,
                StoreStatusEnum.GROUNDING_SUSPENSION.name
            )
        ) {
            throw ErrorCodeException(errorCode = StoreMessageCode.STORE_RELEASE_STEPS_ERROR)
        }
        return true
    }

    override fun updateComponentPkgInfo(
        userId: String,
        storePkgInfoUpdateRequest: StorePkgInfoUpdateRequest
    ): Boolean {
        val storeCode = storePkgInfoUpdateRequest.storeCode
        val version = storePkgInfoUpdateRequest.version
        val storeType = storePkgInfoUpdateRequest.storeType
        val storeId = with(storeBaseQueryDao) {
            when (storePkgInfoUpdateRequest.releaseType) {
                ReleaseTypeEnum.NEW -> getFirstComponent(
                    dslContext = dslContext,
                    storeCode = storeCode,
                    storeType = storeType
                )?.id
                else -> getComponentId(
                    dslContext = dslContext,
                    storeCode = storeCode,
                    version = version,
                    storeType = storeType
                )
            }
        } ?: DigestUtils.md5Hex("$storeType-$storeCode-$version")
        val storePkgEnvRequests = storePkgInfoUpdateRequest.storePkgEnvInfos
        val storeBaseEnvDataPOs: MutableList<StoreBaseEnvDataPO> = mutableListOf()
        var storeBaseEnvExtDataPOs: MutableList<StoreBaseEnvExtDataPO>? = null
        storePkgEnvRequests.forEach { storePkgEnvRequest ->
            val envId = UUIDUtil.generate()
            // 生成环境基本信息
            val storeBaseEnvDataPO = StoreBaseEnvDataPO(
                id = envId,
                storeId = storeId,
                language = storePkgEnvRequest.language,
                pkgName = storePkgEnvRequest.pkgName,
                pkgPath = storePkgEnvRequest.pkgRepoPath,
                target = storePkgEnvRequest.target,
                shaContent = storePkgEnvRequest.shaContent,
                preCmd = storePkgEnvRequest.preCmd,
                osName = storePkgEnvRequest.osName,
                osArch = storePkgEnvRequest.osArch,
                runtimeVersion = storePkgEnvRequest.runtimeVersion,
                defaultFlag = storePkgEnvRequest.defaultFlag,
                creator = userId,
                modifier = userId
            )
            storeBaseEnvDataPOs.add(storeBaseEnvDataPO)
            // 生成环境扩展信息
            storeBaseEnvExtDataPOs = StoreReleaseUtils.generateStoreBaseEnvExtPO(
                envId = envId,
                storeId = storeId,
                userId = userId,
                extBaseEnvInfo = storePkgEnvRequest.extEnvInfo
            )
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            storeBaseEnvManageDao.deleteStoreEnvInfo(context, storeId)
            storeBaseEnvManageDao.batchSave(context, storeBaseEnvDataPOs)
            storeBaseEnvExtManageDao.deleteStoreEnvExtInfo(context, storeId)
            if (!storeBaseEnvExtDataPOs.isNullOrEmpty()) {
                storeBaseEnvExtManageDao.batchSave(context, storeBaseEnvExtDataPOs!!)
            }
        }
        return true
    }
}
