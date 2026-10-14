/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 */
package com.tencent.devops.notify.pojo.wework

/**
 * 场景 E：审核完成后把原卡片更新为 text_notice。
 * 仅用于已发送的审核模板卡片，不改变文本/邮件/MyOA。
 */
object WeworkReviewResultCard {

    const val RESULT_APPROVED = "已通过"
    const val RESULT_REJECTED = "已驳回"
    const val RESULT_DONE = "已处理"

    fun build(
        taskId: String,
        resultTitle: String,
        reviewer: String,
        reviewedAt: String,
        projectName: String = "",
        pipelineName: String = "",
        buildNum: String = "",
        reviewStage: String = "",
        rejectReason: String? = null
    ): WeworkTemplateCard {
        val subtitle = listOf(pipelineName, buildNum.takeIf { it.isNotBlank() }?.let { "#$it" })
            .filter { !it.isNullOrBlank() }
            .joinToString(" · ")
        val contents = mutableListOf<WeworkTemplateCardHorizontalContent>()
        if (projectName.isNotBlank()) {
            contents.add(WeworkTemplateCardHorizontalContent("项目", projectName))
        }
        if (pipelineName.isNotBlank()) {
            contents.add(WeworkTemplateCardHorizontalContent("流水线", pipelineName))
        }
        if (buildNum.isNotBlank()) {
            contents.add(WeworkTemplateCardHorizontalContent("构建号", "#$buildNum"))
        }
        if (reviewStage.isNotBlank() && contents.size < WeworkReviewCardConst.MAX_HLIST) {
            contents.add(WeworkTemplateCardHorizontalContent("审核阶段", reviewStage))
        }
        val quote = rejectReason?.takeIf { it.isNotBlank() }?.let {
            WeworkTemplateCardQuoteArea(title = "驳回原因", quoteText = it.take(200))
        }
        return WeworkTemplateCard(
            cardType = WeworkReviewCardConst.CARD_TYPE_TEXT_NOTICE,
            source = WeworkTemplateCardSource(desc = "蓝盾流水线", descColor = 3),
            mainTitle = WeworkTemplateCardMainTitle(
                title = "流水线人工审核",
                desc = subtitle.takeIf { it.isNotBlank() }?.take(64)
            ),
            emphasisContent = WeworkTemplateCardEmphasis(
                title = resultTitle,
                desc = "审核人: $reviewer · $reviewedAt"
            ),
            quoteArea = quote,
            horizontalContentList = contents.takeIf { it.isNotEmpty() },
            taskId = taskId
        )
    }

    fun resultTitle(action: String, alreadyDone: Boolean): String {
        return when {
            alreadyDone -> RESULT_DONE
            action == WeworkReviewCardConst.ACTION_REJECT -> RESULT_REJECTED
            else -> RESULT_APPROVED
        }
    }
}
