/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.devops.auth.service

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.ManagerOrganizationDao
import com.tencent.devops.auth.dao.ManagerUserDao
import com.tencent.devops.auth.entity.ManagerChangeType
import com.tencent.devops.auth.entity.ManagerOrganizationInfo
import com.tencent.devops.auth.pojo.ManageOrganizationEntity
import com.tencent.devops.auth.pojo.dto.ManageOrganizationDTO
import com.tencent.devops.auth.refresh.dispatch.AuthRefreshDispatch
import com.tencent.devops.auth.refresh.event.ManagerOrganizationChangeEvent
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.service.utils.LogUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ManagerOrganizationService @Autowired constructor(
    val dslContext: DSLContext,
    val strategyService: StrategyService,
    val managerOrganizationDao: ManagerOrganizationDao,
    val managerOrganizationUserDao: ManagerUserDao,
    val organizationService: OrganizationService,
    val refreshDispatch: AuthRefreshDispatch
) {

    fun createManagerOrganization(userId: String, managerOrganization: ManageOrganizationDTO): Int {
        logger.info("createManagerOrganization |$userId | $managerOrganization")
        checkBeforeExecute(
            managerOrganization = managerOrganization,
            action = createAction,
            id = null
        )

        val id = managerOrganizationDao.create(
            dslContext = dslContext,
            userId = userId,
            managerOrganization = ManagerOrganizationInfo(
                organizationLevel = managerOrganization.level,
                organizationId = managerOrganization.organizationId,
                strategyId = managerOrganization.strategyId,
                name = managerOrganization.name,
                id = null
            )
        )
        logger.info("createManagerOrganization id{$id}")
        return id
    }

    fun updateManagerOrganization(userId: String, managerOrganization: ManageOrganizationDTO, managerId: Int): Boolean {
        logger.info("updateManagerOrganization $userId $managerId, $managerOrganization")
        checkBeforeExecute(
            managerOrganization = managerOrganization,
            action = updateAction,
            id = managerId
        )

        managerOrganizationDao.update(
            dslContext = dslContext,
            id = managerId,
            userId = userId,
            managerOrganization = ManagerOrganizationInfo(
                organizationLevel = managerOrganization.level,
                organizationId = managerOrganization.organizationId,
                strategyId = managerOrganization.strategyId,
                name = managerOrganization.name,
                id = null
            )
        )
        logger.info("updateManagerOrganization send update to mq： $userId | $managerId | $managerOrganization")
        refreshDispatch.dispatch(
            ManagerOrganizationChangeEvent(
                refreshType = "updateManagerOrganization",
                managerId = managerId,
                managerChangeType = ManagerChangeType.UPDATE
            )
        )
        return true
    }

    fun deleteManagerOrganization(userId: String, managerId: Int): Boolean {
        logger.info("deleteManagerOrganization $userId $managerId")
        managerOrganizationDao.delete(dslContext, managerId, userId)
        logger.info("deleteManagerOrganization send update to mq: $userId | $managerId ")
        refreshDispatch.dispatch(
            ManagerOrganizationChangeEvent(
                refreshType = "deleteManagerOrganization",
                managerId = managerId,
                managerChangeType = ManagerChangeType.DELETE
            )
        )
        return true
    }

    fun getManagerOrganization(managerId: Int): ManageOrganizationEntity? {
        val watcher = Watcher("getManagerOrganization")
        watcher.start("getManager")
        val record = managerOrganizationDao.get(dslContext, managerId) ?: return null
        watcher.start("getStrategyName")
        val strategyName = strategyService.getStrategyName(record.strategyid.toString()) ?: ""
        watcher.start("getParentOrganizationInfo")
        val parentOrganizationInfo = organizationService.getParentOrganizationInfo(
            organizationId = record.organizationId.toString(),
            level = record.level)
        val parentOrg = parentOrganizationInfo?.sortedBy { it.level }
        watcher.start("getOrganizationInfo")
        logger.info("list createTime: ${record.createTime}, ${DateTimeUtil.toDateTime(record.createTime)}")
        val organizationInfo = organizationService.getOrganizationInfo(record.organizationId.toString(), record!!.level)
        logger.info("get orgInfo ${record.organizationId}| ${record.level}| $organizationInfo")
        val entity = ManageOrganizationEntity(
            id = record.id,
            name = record.name,
            organizationId = record.organizationId,
            strategyId = record.strategyid,
            strategyName = strategyName,
            organizationLevel = record.level,
            createUser = record.createUser,
            createTime = DateTimeUtil.toDateTime(record.createTime),
            organizationName = organizationInfo?.organizationName ?: "",
            parentOrganizations = parentOrg
        )
        LogUtils.printCostTimeWE(watcher, warnThreshold = 200, errorThreshold = 1000)
        return entity
    }

    fun getManagerInfo(managerId: Int, needDeleteData: Boolean? = false): ManagerOrganizationInfo? {
        val record = if (needDeleteData!!) {
            managerOrganizationDao.getById(dslContext, managerId)
        } else {
            managerOrganizationDao.get(dslContext, managerId)
        }
        return ManagerOrganizationInfo(
            id = record!!.id,
            strategyId = record.strategyid,
            organizationLevel = record.level,
            organizationId = record.organizationId,
            name = record.name
        )
    }

    fun getManagerIdByStrategyId(strategyId: Int): List<String> {
        val managerOrganizationRecords = managerOrganizationDao.getByStrategyId(dslContext, strategyId)
        val managerIds = mutableListOf<String>()
        managerOrganizationRecords?.forEach {
            managerIds.add(it.id.toString())
        }
        return managerIds
    }

    fun listOrganization(): List<ManageOrganizationEntity>? {
        val records = managerOrganizationDao.list(dslContext)
        val entitys = mutableListOf<ManageOrganizationEntity>()
        records!!.forEach {
            val entity = getManagerOrganization(it.id)
            if (entity != null) {
                entity.userCount = managerOrganizationUserDao.count(dslContext, it.id)
                entitys.add(entity)
            }
        }
        return entitys
    }

    fun listManager(): List<ManagerOrganizationInfo>? {
        val records = managerOrganizationDao.list(dslContext)
        val entitys = mutableListOf<ManagerOrganizationInfo>()
        records!!.forEach {
            entitys.add(ManagerOrganizationInfo(
                id = it.id,
                organizationId = it.organizationId,
                organizationLevel = it.level,
                strategyId = it.strategyid,
                name = it.name
            ))
        }
        return entitys
    }

    fun countByOrg(orgId: Int): Int {
        return managerOrganizationDao.countByOrg(dslContext, orgId)
    }

    private fun checkBeforeExecute(managerOrganization: ManageOrganizationDTO, action: String, id: Int?) {
        checkOrgLevel(managerOrganization.level, managerOrganization.parentOrgId)
        logger.info("checkBeforeExecute level check success, $managerOrganization | $action| $id")
        val record = managerOrganizationDao.getByStrategyId(
            dslContext = dslContext,
            organizationId = managerOrganization.organizationId,
            strategyId = managerOrganization.strategyId
        )

        when (action) {
            createAction -> if (record != null && record.size > 0) {
                logger.warn("checkBeforeExecute fail, createAction:$record|" +
                    " ${managerOrganization.organizationId}| ${managerOrganization.strategyId} is exist")
                throw ErrorCodeException(
                    defaultMessage = "",
                    errorCode = AuthMessageCode.MANAGER_ORG_EXIST
                )
            }
            updateAction -> {
                if (record == null || record.size == 0) {
                    return
                }
                val ids = record.map { it.id }
                if (!ids.contains(id) || ids.size > 1) {
                    logger.warn("checkBeforeExecute fail, updateAction: $ids |$id |" +
                        " ${managerOrganization.organizationId}| ${managerOrganization.strategyId} is not exist")
                    throw ErrorCodeException(
                        defaultMessage = "",
                        errorCode = AuthMessageCode.MANAGER_ORG_EXIST
                    )
                }
            }
        }
    }

    private fun checkOrgLevel(level: Int, parentList: List<Int>) {
        if (level - 1 != parentList.size) {
            logger.warn("levelCheck fail, $level| ${parentList.size}")
            throw ErrorCodeException(
                defaultMessage = "",
                errorCode = AuthMessageCode.MANAGER_ORG_CHECKOUT_FAIL
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ManagerOrganizationService::class.java)
        const val createAction = "create"
        const val updateAction = "update"
    }
}
