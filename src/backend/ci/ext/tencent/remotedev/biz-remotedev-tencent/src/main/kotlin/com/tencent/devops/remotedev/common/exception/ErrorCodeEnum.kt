package com.tencent.devops.remotedev.common.exception

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.web.utils.I18nUtil

enum class ErrorCodeEnum(
    @BkFieldI18n
    val errorType: ErrorType,
    val errorCode: String,
    val formatErrorMessage: String
) {

    USER_NOT_EXISTS(
        errorType = ErrorType.USER,
        errorCode = "2132001",
        formatErrorMessage = "Account [{0}] does not exist, please contact DevOps-helper to register"
    ),
    OAUTH_ILLEGAL(
        errorType = ErrorType.USER,
        errorCode = "2132002",
        formatErrorMessage = "{0} oauth invalid, need to re-authorize"
    ),
    WORKSPACE_NOT_FIND(
        errorType = ErrorType.USER,
        errorCode = "2132003",
        formatErrorMessage = "workspace {0} not find"
    ),
    DEVFILE_ERROR(
        errorType = ErrorType.USER,
        errorCode = "2132004",
        formatErrorMessage = "load devfile error: {0}"
    ),
    USERINFO_ERROR(
        errorType = ErrorType.USER,
        errorCode = "2132005",
        formatErrorMessage = "load user info error: {0}"
    ),
    WORKSPACE_STATUS_CHANGE_FAIL(
        errorType = ErrorType.USER,
        errorCode = "2132006",
        formatErrorMessage = "workspace({0}) change failed: {1}"
    ),
    WORKSPACE_SHARE_FAIL(
        errorType = ErrorType.USER,
        errorCode = "2132007",
        formatErrorMessage = "workspace share failed: {0}"
    ),
    FORBIDDEN(
        errorType = ErrorType.USER,
        errorCode = "2132008",
        formatErrorMessage = "You do not have permission: {0}"
    ),
    WORKSPACE_MAX_RUNNING(
        errorType = ErrorType.USER,
        errorCode = "2132009",
        formatErrorMessage = "The current workspace running count({0}) has reached the user limit({1})"
    ),
    WORKSPACE_MAX_HAVING(
        errorType = ErrorType.USER,
        errorCode = "2132010",
        formatErrorMessage = "The number of created workspaces({0}) has reached the user limit({1})"
    ),
    UPDATE_BK_TICKET_FAIL(
        errorType = ErrorType.USER,
        errorCode = "2132011",
        formatErrorMessage = "update BkTicket fail,please check hostName exists."
    ),
    DENIAL_OF_SERVICE(
        errorType = ErrorType.USER,
        errorCode = "2132012",
        formatErrorMessage = "Sorry, you are not authorized to access this resource."
    ),
    REPEAT_REQUEST(
        errorType = ErrorType.USER,
        errorCode = "2132013",
        formatErrorMessage = "Repeat request! Please try again later."
    ),
    WORKSPACE_ERROR(
        errorType = ErrorType.USER,
        errorCode = "2132014",
        formatErrorMessage = "The workspace has error and cannot be repaired, please choose to destroy."
    ),
    WORKSPACE_ERROR_FIX(
        errorType = ErrorType.USER,
        errorCode = "2132015",
        formatErrorMessage = "The errored workspace has been restored to [{0}], please try again."
    ),
    WORKSPACE_NOT_RUNNING(
        errorType = ErrorType.USER,
        errorCode = "2132016",
        formatErrorMessage = "Workspace [{0}] is not currently running."
    ),
    CHECK_USER_TICKET_FAIL(
        errorType = ErrorType.USER,
        errorCode = "2132017",
        formatErrorMessage = "check user login ticket fail."
    ),
    GET_WATERMARK_FAIL(
        errorType = ErrorType.USER,
        errorCode = "2132018",
        formatErrorMessage = "get watermark fail."
    ),
    WORKSPACE_UNAVAILABLE(
        errorType = ErrorType.USER,
        errorCode = "2132020",
        formatErrorMessage = "The usage time of the current workspace [{0}] has reached the user limit [{1}h]"
    );

    fun getErrorMessage(): String {
        return I18nUtil.getCodeLanMessage(this.errorCode)
    }
}
