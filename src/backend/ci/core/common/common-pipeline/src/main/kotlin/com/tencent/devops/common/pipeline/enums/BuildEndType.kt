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

package com.tencent.devops.common.pipeline.enums

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nTranslateTypeEnum

/**
 * 构建终态子类型，覆盖取消/失败/超时/成功等所有结束场景。
 * 命名规范: {大类}_{子类型}，如 CANCEL_USER、FAIL_EXEC、TIMEOUT_JOB。
 *
 * 新增子类型只需在此追加枚举项并补充 buildEndType.{displayName} 国际化词条，
 * 前端可直接按 category 归类展示，无需感知具体子类型。
 */
enum class BuildEndType(
    @BkFieldI18n(
        translateType = I18nTranslateTypeEnum.VALUE,
        keyPrefixName = "buildEndType",
        reusePrefixFlag = false
    )
    val displayName: String,
    val category: BuildEndCategory
) {
    // ---- 取消类 ----
    CANCEL_USER("cancelUser", BuildEndCategory.CANCEL),
    // 系统自动取消，含服务重启、排队满、并发互斥、构建机失联等，具体成因见 reason
    CANCEL_SYSTEM("cancelSystem", BuildEndCategory.CANCEL),
    CANCEL_PARENT_PIPELINE("cancelParentPipeline", BuildEndCategory.CANCEL),

    // ---- 失败类 ----
    // 插件执行失败等常规失败
    FAIL_EXEC("failExec", BuildEndCategory.FAIL),
    // 质量红线拦截未达标
    FAIL_QUALITY("failQuality", BuildEndCategory.FAIL),
    // 人工审核驳回
    FAIL_REVIEW("failReview", BuildEndCategory.FAIL),
    // 子流水线执行失败导致父构建失败
    FAIL_SUB_PIPELINE("failSubPipeline", BuildEndCategory.FAIL),
    // 同阶段其他Job失败且开启了FastKill，本位置被提前终止（连带影响，非独立失败原因）
    FAIL_FAST_KILL("failFastKill", BuildEndCategory.FAIL),
    // 同一次构建中同时存在多种失败子类型
    FAIL_MULTIPLE("failMultiple", BuildEndCategory.FAIL),

    // ---- 超时类 ----
    // Job 超过执行时限，其下步骤被终止。仅位置级：构建以终止链路收尾，构建级归取消/失败类
    TIMEOUT_JOB("timeoutJob", BuildEndCategory.TIMEOUT),
    // 单个步骤超过自身执行时限。仅位置级：构建级归失败类
    TIMEOUT_STEP("timeoutStep", BuildEndCategory.TIMEOUT),
    // 构建排队超时。唯一的构建级超时子类型（构建状态本身即 QUEUE_TIMEOUT）
    TIMEOUT_QUEUE("timeoutQueue", BuildEndCategory.TIMEOUT),

    // ---- 成功类 ----
    SUCCESS("success", BuildEndCategory.SUCCESS),
    // 阶段准入被驳回，后续阶段不再执行，构建按成功结束
    SUCCESS_STAGE_ABORT("successStageAbort", BuildEndCategory.SUCCESS),
    // 阶段准入等待人工审核中，构建暂以阶段成功挂起，审核通过后会继续运行
    SUCCESS_STAGE_REVIEWING("successStageReviewing", BuildEndCategory.SUCCESS);

    companion object {
        fun parse(name: String?): BuildEndType? {
            return try {
                if (name == null) null else valueOf(name)
            } catch (ignored: Exception) {
                null
            }
        }
    }
}

/**
 * 构建终态大类，供前端按类别渲染不同样式的详情卡片（失败卡片/超时卡片/取消卡片/成功卡片）。
 *
 * 大类必须与所在层级的状态同类：构建级跟随构建最终状态（`ModelRecord.status`），
 * 位置级跟随该位置的 statusAtEnd。因此 Job执行超时、Agent心跳超时这类走 TERMINATE 事件链路、
 * 以取消/终止/失败收尾的场景，构建级不能归入 TIMEOUT——具体成因由 reason 与位置级子类型表达。
 */
enum class BuildEndCategory {
    CANCEL,
    FAIL,
    TIMEOUT,
    SUCCESS
}
