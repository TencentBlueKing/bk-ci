package com.tencent.devops.remotedev.common.exception

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nTranslateTypeEnum
import com.tencent.devops.common.api.pojo.ErrorType

enum class ErrorCodeEnum(
    @BkFieldI18n
    val errorType: ErrorType,
    val errorCode: String,
    @BkFieldI18n(translateType = I18nTranslateTypeEnum.VALUE, reusePrefixFlag = false)
    val formatErrorMessage: String
) {

    USER_NOT_EXISTS(
        errorType = ErrorType.USER,
        errorCode = "2132001",
        formatErrorMessage = "2132001"
    ),//Account [%s] does not exist, please contact DevOps-helper to register
    OAUTH_ILLEGAL(
        errorType = ErrorType.USER,
        errorCode = "2132002",
        formatErrorMessage = "2132002"
    ),//%s oauth invalid, need to re-authorize
    WORKSPACE_NOT_FIND(
        errorType = ErrorType.USER,
        errorCode = "2132003",
        formatErrorMessage = "2132003"
    ),//workspace %s not find
    DEVFILE_ERROR(
        errorType = ErrorType.USER,
        errorCode = "2132004",
        formatErrorMessage = "2132004"
    ),//load devfile error: %s
    USERINFO_ERROR(
        errorType = ErrorType.USER,
        errorCode = "2132005",
        formatErrorMessage = "2132005"
    ),//load user info error: %s
    WORKSPACE_STATUS_CHANGE_FAIL(
        errorType = ErrorType.USER,
        errorCode = "2132006",
        formatErrorMessage = "2132006"
    ),//workspace(%s) change failed: %s
    WORKSPACE_SHARE_FAIL(
        errorType = ErrorType.USER,
        errorCode = "2132007",
        formatErrorMessage = "2132007"
    ),//workspace share failed: %s
    FORBIDDEN(
        errorType = ErrorType.USER,
        errorCode = "2132008",
        formatErrorMessage = "2132008"
    ),//You do not have permission: %s
    WORKSPACE_MAX_RUNNING(
        errorType = ErrorType.USER,
        errorCode = "2132009",
        formatErrorMessage = "2132009"
    ),//The current workspace running count(%s) has reached the user limit(%s)
    WORKSPACE_MAX_HAVING(
        errorType = ErrorType.USER,
        errorCode = "2132010",
        formatErrorMessage = "2132010"
    ),//The number of created workspaces(%s) has reached the user limit(%s)
    UPDATE_BK_TICKET_FAIL(
        errorType = ErrorType.USER,
        errorCode = "2132011",
        formatErrorMessage = "2132011"
    ),// update BkTicket fail.
    DENIAL_OF_SERVICE(
        errorType = ErrorType.USER,
        errorCode = "2132012",
        formatErrorMessage = "2132012"
    ),// Repeat request! Please try again later.
    REPEAT_REQUEST(
        errorType = ErrorType.USER,
        errorCode = "2132013",
        formatErrorMessage = "2132013"
    ),// The workspace has error and cannot be repaired, please choose to destroy.
    WORKSPACE_ERROR(
        errorType = ErrorType.USER,
        errorCode = "2132014",
        formatErrorMessage = "2132014"
    ),// The errored workspace has been restored to [%s], please try again.
    WORKSPACE_ERROR_FIX(
        errorType = ErrorType.USER,
        errorCode = "2132015",
        formatErrorMessage = "2132015"
    )
}
