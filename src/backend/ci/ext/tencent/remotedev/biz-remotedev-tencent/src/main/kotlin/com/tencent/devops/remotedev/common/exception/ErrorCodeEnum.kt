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
        formatErrorMessage = "Account [%s] does not exist, please contact DevOps-helper to register"
    ),
    OAUTH_ILLEGAL(
        errorType = ErrorType.USER,
        errorCode = "2132002",
        formatErrorMessage = "%s oauth invalid, need to re-authorize"
    ),
    WORKSPACE_NOT_FIND(
        errorType = ErrorType.USER,
        errorCode = "2132003",
        formatErrorMessage = "workspace %s not find"
    ),
    DEVFILE_ERROR(
        errorType = ErrorType.USER,
        errorCode = "2132004",
        formatErrorMessage = "load devfile error: %s"
    ),
    USERINFO_ERROR(
        errorType = ErrorType.USER,
        errorCode = "2132005",
        formatErrorMessage = "load user info error: %s"
    ),
    WORKSPACE_STATUS_CHANGE_FAIL(
        errorType = ErrorType.USER,
        errorCode = "2132006",
        formatErrorMessage = "workspace(%s) change failed: %s"
    ),
    WORKSPACE_SHARE_FAIL(
        errorType = ErrorType.USER,
        errorCode = "2132007",
        formatErrorMessage = "workspace share failed: %s"
    ),
    FORBIDDEN(
        errorType = ErrorType.USER,
        errorCode = "2132008",
        formatErrorMessage = "You do not have permission: %s"
    ),
    WORKSPACE_MAX_RUNNING(
        errorType = ErrorType.USER,
        errorCode = "2132009",
        formatErrorMessage = "The current workspace running count(%s) has reached the user limit(%s)"
    ),
    WORKSPACE_MAX_HAVING(
        errorType = ErrorType.USER,
        errorCode = "2132010",
        formatErrorMessage = "The number of created workspaces(%s) has reached the user limit(%s)"
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
        errorCode = "2130015",
        formatErrorMessage = "The errored workspace has been restored to [%s], please try again."
    ),
    WORKSPACE_NOT_RUNNING(
        errorType = ErrorType.USER,
        errorCode = "2130016",
        formatErrorMessage = "Workspace is not currently running."
    ),
    CHECK_USER_TICKET_FAIL(
        errorType = ErrorType.USER,
        errorCode = "2130017",
        formatErrorMessage = "check user login ticket fail."
    );

    fun getErrorMessage(): String {
        return I18nUtil.getCodeLanMessage(this.errorCode)
    }
}
