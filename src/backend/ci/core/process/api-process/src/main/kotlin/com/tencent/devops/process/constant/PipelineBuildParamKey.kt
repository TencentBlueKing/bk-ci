package com.tencent.devops.process.constant

/**
 * 流水线上下文变量Key
 */
object PipelineBuildParamKey {
    // GIT 触发公共参数
    const val CI_REPO_TYPE = "ci.repo_type"
    const val CI_REPO_URL = "ci.repo_url"
    const val CI_REPO = "ci.repo"
    const val CI_REPO_ID = "ci.repo_id"
    const val CI_REPO_GROUP = "ci.repo_group"
    const val CI_REPO_NAME = "ci.repo_name"
    const val CI_REPO_ALIAS_NAME = "ci.repo_alias_name"
    const val CI_EVENT = "ci.event"
    const val CI_EVENT_URL = "ci.event_url"
    const val CI_BRANCH = "ci.branch"
    const val CI_BUILD_MSG = "ci.build_msg"
    const val CI_COMMIT_MESSAGE = "ci.commit_message"
    const val CI_ACTION = "ci.action"
    const val CI_ACTOR = "ci.actor"

    // GIT PUSH参数
    const val CI_BEFORE_SHA = "ci.before_sha"
    const val CI_BEFORE_SHA_SHORT = "ci.before_sha_short"
    const val CI_SHA = "ci.sha"
    const val CI_SHA_SHORT = "ci.sha_short"
    const val CI_OPERATION_KIND = "ci.operation_kind"

    // GIT MR参数
    const val CI_MR_PROPOSER = "ci.mr_proposer"
    const val CI_HEAD_REPO_URL = "ci.head_repo_url"
    const val CI_BASE_REPO_URL = "ci.base_repo_url"
    const val CI_HEAD_REF = "ci.head_ref"
    const val CI_BASE_REF = "ci.base_ref"
    const val CI_MR_ID = "ci.mr_id"
    const val CI_MR_IID = "ci.mr_iid"
    const val CI_MR_DESC = "ci.mr_desc"
    const val CI_MR_TITLE = "ci.mr_title"
    const val CI_MR_URL = "ci.mr_url"
    const val CI_MR_REVIEWERS = "ci.mr_reviewers"
    const val CI_MILESTONE_NAME = "ci.milestone_name"
    const val CI_MILESTONE_ID = "ci.milestone_id"
    const val CI_TAPD_ISSUES = "ci.mr_tapd_issues"

    // GIT TAG参数
    const val CI_COMMIT_AUTHOR = "ci.commit_author"
    const val CI_TAG_FROM = "ci.tag_from"
    const val CI_TAG_DESC = "ci.tag_desc"

    // GIT TAG参数
    const val CI_ISSUE_TITLE = "ci.issue_title"
    const val CI_ISSUE_ID = "ci.issue_id"
    const val CI_ISSUE_IID = "ci.issue_iid"
    const val CI_ISSUE_DESCRIPTION = "ci.issue_description"
    const val CI_ISSUE_STATE = "ci.issue_state"
    const val CI_ISSUE_OWNER = "ci.issue_owner"
    const val CI_ISSUE_MILESTONE_ID = "ci.issue_milestone_id"

    // GIT NOTE参数
    const val CI_NOTE_COMMENT = "ci.note_comment"
    const val CI_NOTE_ID = "ci.note_id"
    const val CI_NOTE_TYPE = "ci.note_type"
    const val CI_NOTE_AUTHOR = "ci.note_author"
    const val CI_CREATE_TIME = "ci.create_time"
    const val CI_MODIFY_TIME = "ci.modify_time"

    // GIT REVIEW参数
    const val CI_REVIEW_ID = "ci.review_id"
    const val CI_REVIEW_IID = "ci.review_iid"
    const val CI_REVIEW_TYPE = "ci.review_type"
    const val CI_REVIEW_REVIEWERS = "ci.review_reviewers"
    const val CI_REVIEW_STATE = "ci.review_state"
    const val CI_REVIEW_OWNER = "ci.review_owner"

    // GIT REVIEW参数
    const val CI_CREATE_REF = "ci.create_ref"
    const val CI_CREATE_REF_TYPE = "ci.create_type"

    // 流水线基础变量
    const val CI_BUILD_NO = "ci.build-no"
    const val CI_BUILD_NUM = "ci.build_num"
    const val CI_PIPELINE_CREATOR = "ci.pipeline_creator"
    const val CI_PIPELINE_MODIFIER = "ci.pipeline_modifier"
    const val CI_PIPELINE_VERSION = "ci.pipeline_version"
    const val CI_PROJECT_ID = "ci.project_id"
    const val CI_PROJECT_NAME = "ci.project_name"
    const val CI_BUILD_START_TYPE = "ci.build_start_type"
    const val CI_PIPELINE_ID = "ci.pipeline_id"
    const val CI_BUILD_ID = "ci.build_id"
    const val CI_PIPELINE_NAME = "ci.pipeline_name"
    const val CI_WORKSPACE = "ci.workspace"
    const val CI_FAILED_TASKNAMES = "ci.failed_tasknames"
    const val CI_FAILED_TASKS = "ci.failed_tasks"
    const val CI_REMARK = "ci.remark"

    // 流水线Step变量
    const val STEP_NAME = "step.name"
    const val STEP_ID = "step.id"
    const val STEP_RETRY_COUNT_MANUAL = "step.retry-count-manual"
    const val STEP_RETRY_COUNT_AUTO = "step.retry-count-auto"
    const val STEP_STATUS_TEMPLATE = "steps.<step-id>.status"
    const val STEP_OUTCOME_TEMPLATE = "steps.<step-id>.outcome"

    // 流水线Job变量
    const val JOB_NAME = "job.name"
    const val JOB_ID = "job.id"
    const val JOB_OS = "job.os"
    const val JOB_CONTAINER_NETWORK = "job.container.network"
    const val JOB_CONTAINER_NODE_ALIAS = "job.container.node_alias"
    const val JOB_STAGE_ID = "job.stage_id"
    const val JOB_STAGE_NAME = "job.stage_name"
    const val JOB_INDEX = "job.index"
    const val JOB_STATUS_TEMPLATE = "jobs.<job-id>.status"
    const val JOB_OUTCOME_TEMPLATE = "jobs.<job-id>.outcome"
}
