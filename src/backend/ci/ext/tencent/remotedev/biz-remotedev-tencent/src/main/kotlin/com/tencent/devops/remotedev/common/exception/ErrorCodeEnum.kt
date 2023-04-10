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
        errorCode = "2130001",
        formatErrorMessage = "2130001"
    ),//Account [%s] does not exist, please contact DevOps-helper to register
    OAUTH_ILLEGAL(
        errorType = ErrorType.USER,
        errorCode = "2130002",
        formatErrorMessage = "2130002"
    ),//%s oauth invalid, need to re-authorize
    WORKSPACE_NOT_FIND(
        errorType = ErrorType.USER,
        errorCode = "2130003",
        formatErrorMessage = "2130003"
    ),//workspace %s not find
    DEVFILE_ERROR(
        errorType = ErrorType.USER,
        errorCode = "2130004",
        formatErrorMessage = "2130004"
    ),//load devfile error: %s
    USERINFO_ERROR(
        errorType = ErrorType.USER,
        errorCode = "2130005",
        formatErrorMessage = "2130005"
    ),//load user info error: %s
    WORKSPACE_STATUS_CHANGE_FAIL(
        errorType = ErrorType.USER,
        errorCode = "2130006",
        formatErrorMessage = "2130006"
    ),//workspace(%s) change failed: %s
    WORKSPACE_SHARE_FAIL(
        errorType = ErrorType.USER,
        errorCode = "2130007",
        formatErrorMessage = "2130007"
    ),//workspace share failed: %s
    FORBIDDEN(
        errorType = ErrorType.USER,
        errorCode = "2130008",
        formatErrorMessage = "2130008"
    ),//You do not have permission: %s
    WORKSPACE_MAX_RUNNING(
        errorType = ErrorType.USER,
        errorCode = "2130009",
        formatErrorMessage = "2130009"
    ),//The current workspace running count(%s) has reached the user limit(%s)
    WORKSPACE_MAX_HAVING(
        errorType = ErrorType.USER,
        errorCode = "2130010",
        formatErrorMessage = "2130010"
    ),//The number of created workspaces(%s) has reached the user limit(%s)
    UPDATE_BK_TICKET_FAIL(
        errorType = ErrorType.USER,
        errorCode = "2130011",
        formatErrorMessage = "2130011"
    ),//update BkTicket fail,please check hostName exists.
    DENIAL_OF_SERVICE(
        errorType = ErrorType.USER,
        errorCode = "2130012",
        formatErrorMessage = "2130012"
    ),//Sorry, you are not authorized to access this resource.
    REPEAT_REQUEST(
        errorType = ErrorType.USER,
        errorCode = "2130013",
        formatErrorMessage = "2130013"
    )//Repeat request! Please try again later.
}
