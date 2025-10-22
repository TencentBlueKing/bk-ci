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
 *
 */

package com.tencent.devops.common.webhook.enums

object WebhookI18nConstants {
    // 工蜂事件描述
    const val TGIT_PUSH_EVENT_DESC = "bkTgitPushEventDesc"
    const val TGIT_ISSUE_CREATED_EVENT_DESC = "bkTgitIssueCreatedEventDesc"
    const val TGIT_ISSUE_UPDATED_EVENT_DESC = "bkTgitIssueUpdatedEventDesc"
    const val TGIT_ISSUE_CLOSED_EVENT_DESC = "bkTgitIssueClosedEventDesc"
    const val TGIT_ISSUE_REOPENED_EVENT_DESC = "bkTgitIssueReopenedEventDesc"
    const val TGIT_MR_CREATED_EVENT_DESC = "bkTgitMrCreatedEventDesc"
    const val TGIT_MR_UPDATED_EVENT_DESC = "bkTgitMrUpdatedEventDesc"
    const val TGIT_MR_CLOSED_EVENT_DESC = "bkTgitMrClosedEventDesc"
    const val TGIT_MR_REOPENED_EVENT_DESC = "bkTgitMrReopenedEventDesc"
    const val TGIT_MR_PUSH_UPDATED_EVENT_DESC = "bkTgitMrPushUpdatedEventDesc"
    const val TGIT_MR_MERGED_EVENT_DESC = "bkTgitMrMergedEventDesc"
    const val TGIT_NOTE_EVENT_DESC = "bkTgitNoteEventDesc"
    const val TGIT_REVIEW_APPROVED_EVENT_DESC = "bkTgitReviewApprovedEventDesc"
    const val TGIT_REVIEW_APPROVING_EVENT_DESC = "bkTgitReviewApprovingEventDesc"
    const val TGIT_REVIEW_CLOSED_EVENT_DESC = "bkTgitReviewClosedEventDesc"
    const val TGIT_REVIEW_CHANGE_DENIED_EVENT_DESC = "bkTgitReviewChangeDeniedEventDesc"
    const val TGIT_REVIEW_CHANGE_REQUIRED_EVENT_DESC = "bkTgitReviewChangeRequiredEventDesc"
    const val TGIT_REVIEW_CREATED_EVENT_DESC = "bkTgitReviewCreatedEventDesc"
    const val TGIT_TAG_PUSH_EVENT_DESC = "bkTgitTagPushEventDesc"
    const val TGIT_TAG_DELETE_EVENT_DESC = "bkTgitTagDeleteEventDesc"

    // Github事件描述
    const val GITHUB_PUSH_EVENT_DESC = "bkGithubPushEventDesc"
    const val GITHUB_CREATE_TAG_EVENT_DESC = "bkGithubCreateTagEventDesc"
    const val GITHUB_CREATE_BRANCH_EVENT_DESC = "bkGithubCreateBranchEventDesc"
    const val GITHUB_PR_EVENT_DESC = "bkGithubPrEventDesc"

    // P4事件描述
    const val P4_EVENT_DESC = "bkP4EventDesc"

    // SVN事件描述
    const val SVN_COMMIT_EVENT_DESC = "bkSvnCommitEventDesc"

    // 手动触发
    const val MANUAL_START_EVENT_DESC = "bkManualStartEventDesc"

    // 远程触发
    const val REMOTE_START_EVENT_DESC = "bkRemoteStartEventDesc"

    // openApi触发
    const val OPENAPI_START_EVENT_DESC = "bkServiceStartEventDesc"

    // 流水线触发
    const val PIPELINE_START_EVENT_DESC = "bkPipelineStartEventDesc"

    // 定时触发
    const val TIMING_START_EVENT_DESC = "bkTimingStartEventDesc"

    // 事件类型匹配
    const val EVENT_TYPE_MATCHED = "bkRepoTriggerEventTypeMatched"

    // 事件类型不匹配
    const val EVENT_TYPE_NOT_MATCH = "bkRepoTriggerEventTypeNotMatch"

    // 分支不匹配
    const val BRANCH_NOT_MATCH = "bkRepoTriggerBranchNotMatch"

    // 分支被排除
    const val BRANCH_IGNORED = "bkRepoTriggerBranchIgnored"

    // 目标分支不匹配
    const val TARGET_BRANCH_NOT_MATCH = "bkRepoTriggerTargetBranchNotMatch"

    // 目标分支被排除
    const val TARGET_BRANCH_IGNORED = "bkRepoTriggerTargetBranchIgnored"

    // 源分支不匹配
    const val SOURCE_BRANCH_NOT_MATCH = "bkRepoTriggerSourceBranchNotMatch"

    // 源分支被排除
    const val SOURCE_BRANCH_IGNORED = "bkRepoTriggerSourceBranchIgnored"

    // 用户不匹配
    const val USER_NOT_MATCH = "bkRepoTriggerUserNotMatch"

    // 用户被排除
    const val USER_IGNORED = "bkRepoTriggerUserIgnored"

    // 路径不匹配
    const val PATH_NOT_MATCH = "bkRepoTriggerPathNotMatch"

    // 路径被排除
    const val PATH_IGNORED = "bkRepoTriggerPathIgnored"

    // Tag名称不匹配
    const val TAG_NAME_NOT_MATCH = "bkRepoTriggerTagNameNotMatch"

    // Tag名称被排除
    const val TAG_NAME_IGNORED = "bkRepoTriggerTagNameIgnored"

    // Tag来源分支不匹配
    const val TAG_SOURCE_BRANCH_NOT_MATCH = "bkRepoTriggerTagSourceBranchNotMatch"

    // 评论内容不匹配
    const val NOTE_CONTENT_NOT_MATCH = "bkRepoTriggerNoteContentNotMatch"

    // 评论类型不匹配
    const val NOTE_ACTION_NOT_MATCH = "bkRepoTriggerNoteActionNotMatch"

    // 评审操作类型不匹配
    const val REVIEW_ACTION_NOT_MATCH = "bkRepoTriggerReviewActionNotMatch"

    // issues操作类型不匹配
    const val ISSUES_ACTION_NOT_MATCH = "bkRepoTriggerIssueActionNotMatch"

    // Github Pr操作类型不匹配
    const val PR_ACTION_NOT_MATCH = "bkRepoTriggerPrActionNotMatch"

    // Github Mr操作类型不匹配
    const val MR_ACTION_NOT_MATCH = "bkRepoTriggerMrActionNotMatch"

    // Github Push操作类型不匹配
    const val PUSH_ACTION_NOT_MATCH = "bkRepoTriggerPushActionNotMatch"

    // WIP阶段不触发
    const val MR_SKIP_WIP = "bkRepoTriggerSkipWipNotMatch"

    // 自定义触发控制不匹配
    const val THIRD_FILTER_NOT_MATCH = "bkRepoTriggerThirdFilterNotMatch"

    // 事件回放
    const val EVENT_REPLAY_DESC = "bkEventReplayDesc"

    // 触发条件不匹配默认文案
    const val TRIGGER_CONDITION_NOT_MATCH = "bkTriggerConditionNotMatch"

    // 代码库开启PAC事件描述
    const val ENABLE_PAC_EVENT_DESC = "bkRepoEnablePacEventDesc"
}
