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

package com.tencent.devops.quality.dao.v2

import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.model.quality.tables.TQualityRuleOperation
import com.tencent.devops.model.quality.tables.records.TQualityRuleOperationRecord
import com.tencent.devops.quality.pojo.enum.RuleOperation
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class QualityRuleOperationDao {
    fun saveEndOperation(
        dslContext: DSLContext,
        ruleId: Long,
        notifyUserList: List<String>,
        notifyGroupList: List<Long>,
        notifyTypeList: List<NotifyType>
    ) {
        with(TQualityRuleOperation.T_QUALITY_RULE_OPERATION) {
            dslContext.insertInto(
                this,
                this.TYPE,
                this.NOTIFY_USER,
                this.NOTIFY_GROUP_ID,
                this.NOTIFY_TYPES,
                this.RULE_ID
            )
                .values(
                    RuleOperation.END.name,
                    notifyUserList.joinToString(","),
                    notifyGroupList.joinToString(","),
                    notifyTypeList.joinToString(","),
                    ruleId
                )
                .execute()
        }
    }

    fun saveAuditOperation(
        dslContext: DSLContext,
        ruleId: Long,
        auditUserList: List<String>,
        auditTimeoutMinutes: Int
    ) {
        with(TQualityRuleOperation.T_QUALITY_RULE_OPERATION) {
            dslContext.insertInto(
                this,
                this.TYPE,
                this.AUDIT_USER,
                this.AUDIT_TIMEOUT,
                this.RULE_ID
            )
                .values(
                    RuleOperation.AUDIT.name,
                    auditUserList.joinToString(","),
                    auditTimeoutMinutes,
                    ruleId
                )
                .execute()
        }
    }

    fun updateEndOperation(
        dslContext: DSLContext,
        ruleId: Long,
        notifyUserList: List<String>,
        notifyGroupList: List<Long>,
        notifyTypeList: List<NotifyType>
    ) {
        with(TQualityRuleOperation.T_QUALITY_RULE_OPERATION) {
            dslContext.update(this)
                .set(this.NOTIFY_USER, notifyUserList.joinToString(","))
                .set(this.NOTIFY_GROUP_ID, notifyGroupList.joinToString(","))
                .set(this.NOTIFY_TYPES, notifyTypeList.joinToString(","))
                .set(this.TYPE, RuleOperation.END.name)
                .where(RULE_ID.eq(ruleId))
                .execute()
        }
    }

    fun updateAuditOperation(
        dslContext: DSLContext,
        ruleId: Long,
        auditUserList: List<String>,
        auditTimeoutMinutes: Int
    ) {
        with(TQualityRuleOperation.T_QUALITY_RULE_OPERATION) {
            dslContext.update(this)
                .set(this.AUDIT_USER, auditUserList.joinToString(","))
                .set(this.AUDIT_TIMEOUT, auditTimeoutMinutes)
                .set(this.TYPE, RuleOperation.AUDIT.name)
                .where(RULE_ID.eq(ruleId))
                .execute()
        }
    }

    fun get(dslContext: DSLContext, ruleId: Long): TQualityRuleOperationRecord {
        return with(TQualityRuleOperation.T_QUALITY_RULE_OPERATION) {
            dslContext.selectFrom(this)
                .where(RULE_ID.eq(ruleId))
                .fetchOne()!!
        }
    }

    fun batchGet(dslContext: DSLContext, ruleIds: Collection<Long>): Result<TQualityRuleOperationRecord> {
        return with(TQualityRuleOperation.T_QUALITY_RULE_OPERATION) {
            dslContext.selectFrom(this)
                .where(RULE_ID.`in`(ruleIds))
                .fetch()
        }
    }
}
