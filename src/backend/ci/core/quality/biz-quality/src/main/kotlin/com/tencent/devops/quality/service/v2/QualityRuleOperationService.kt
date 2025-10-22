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

package com.tencent.devops.quality.service.v2

import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.model.quality.tables.records.TQualityRuleOperationRecord
import com.tencent.devops.quality.dao.v2.QualityRuleOperationDao
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class QualityRuleOperationService @Autowired constructor(
    private val dslContext: DSLContext,
    private val ruleOperationDao: QualityRuleOperationDao
) {
    fun serviceSaveEndOperation(
        ruleId: Long,
        notifyUserList: List<String>,
        notifyGroupList: List<Long>,
        notifyTypeList: List<NotifyType>
    ) {
        ruleOperationDao.saveEndOperation(
            dslContext = dslContext,
            ruleId = ruleId,
            notifyUserList = notifyUserList,
            notifyGroupList = notifyGroupList,
            notifyTypeList = notifyTypeList
        )
    }

    fun serviceSaveAuditOperation(ruleId: Long, auditUserList: List<String>, auditTimeoutMinutes: Int) {
        ruleOperationDao.saveAuditOperation(
            dslContext = dslContext,
            ruleId = ruleId,
            auditUserList = auditUserList,
            auditTimeoutMinutes = auditTimeoutMinutes
        )
    }

    fun serviceUpdateEndOperation(
        ruleId: Long,
        notifyUserList: List<String>,
        notifyGroupList: List<Long>,
        notifyTypeList: List<NotifyType>
    ) {
        ruleOperationDao.updateEndOperation(
            dslContext = dslContext,
            ruleId = ruleId,
            notifyUserList = notifyUserList,
            notifyGroupList = notifyGroupList,
            notifyTypeList = notifyTypeList
        )
    }

    fun serviceUpdateAuditOperation(ruleId: Long, auditUserList: List<String>, auditTimeoutMinutes: Int) {
        ruleOperationDao.updateAuditOperation(
            dslContext = dslContext,
            ruleId = ruleId,
            auditUserList = auditUserList,
            auditTimeoutMinutes = auditTimeoutMinutes
        )
    }

    fun serviceGet(dslContext: DSLContext, ruleId: Long): TQualityRuleOperationRecord {
        return ruleOperationDao.get(dslContext, ruleId)
    }

    fun serviceBatchGet(dslContext: DSLContext, ruleIds: Collection<Long>): List<TQualityRuleOperationRecord> {
        return ruleOperationDao.batchGet(dslContext, ruleIds)
    }
}
