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

    BASE_ERROR(
        errorType = ErrorType.USER,
        errorCode = "2132001",
        formatErrorMessage = "error: {0}"
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
        formatErrorMessage = "The workspace has error , please try again later or contact 8000 helper."
    ),
    WORKSPACE_ERROR_FIX(
        errorType = ErrorType.USER,
        errorCode = "2132015",
        formatErrorMessage = "The errored workspace has been restored to [{0}], please try again later."
    ),
    WORKSPACE_NOT_RUNNING(
        errorType = ErrorType.USER,
        errorCode = "2132016",
        formatErrorMessage = "Workspace [{0}] is not currently running," +
            "please try again later or contact 8000 helper."
    ),
    GET_WATERMARK_FAIL(
        errorType = ErrorType.USER,
        errorCode = "2132018",
        formatErrorMessage = "get watermark fail."
    ),
    NOT_ALLOWED_ENVIRONMENT(
        errorType = ErrorType.USER,
        errorCode = "2132019",
        formatErrorMessage = "Not allowed to create in the current environment."
    ),
    WINDOWS_CONFIG_NOT_FIND(
        errorType = ErrorType.USER,
        errorCode = "2132021",
        formatErrorMessage = "windows config {0} not find"
    ),
    WINDOWS_RESOURCE_NOT_AVAILABLE(
        errorType = ErrorType.USER,
        errorCode = "2132022",
        formatErrorMessage = "windows resource {0} not available"
    ),
    INSTALL_SOFTWARE_FAIL(
        errorType = ErrorType.USER,
        errorCode = "2132024",
        formatErrorMessage = "install software fail."
    ),
    PROJECT_DESKTOP_RESOURCES_INSUFFICIENT(
        errorType = ErrorType.USER,
        errorCode = "2132027",
        formatErrorMessage = "The cloud desktop resources under the current project are insufficient, currently {0}"
    ),
    CLIENT_NEED_UPDATED(
        errorType = ErrorType.USER,
        errorCode = "2132028",
        formatErrorMessage = "Please install the latest version of the client."
    ),
    DELIVERING_FAILED(
        errorType = ErrorType.THIRD_PARTY,
        errorCode = "2132029",
        formatErrorMessage = "delivering failed"
    ),
    REAPPLY_EXPERT_SUPPORT_ERROR(
        errorType = ErrorType.USER,
        errorCode = "2132031",
        formatErrorMessage = "Please do not submit duplicate issue requests within 1 hour[{0}]."
    ),
    ZONE_VM_RESOURCE_NOT_ENOUGH(
        errorType = ErrorType.THIRD_PARTY,
        errorCode = "2132032",
        formatErrorMessage = "zone {0} machine {1} free {2} less than the {3} units that currently need to be produced."
    ),
    PROJECT_ACCESS_DEVICE_PERMISSION(
        errorType = ErrorType.THIRD_PARTY,
        errorCode = "2132033",
        formatErrorMessage = "req project_access_device_permissions error {0}"
    ),
    PROJECT_DESKTOP_SPEC_RESOURCES_INSUFFICIENT(
        errorType = ErrorType.USER,
        errorCode = "2132035",
        formatErrorMessage = "{0} is a special model, project quota is insufficient.Quota is {1}, and {2} are available"
    ),
    NO_TGIT_OAUTH_ERROR(
        errorType = ErrorType.USER,
        errorCode = "2132036",
        formatErrorMessage = "{0} No oauth authorization for {1}"
    ),
    CREATE_ITSM_TICKET_ERROR(
        errorType = ErrorType.THIRD_PARTY,
        errorCode = "2132037",
        formatErrorMessage = "{0} {1} create itsm ticket error"
    ),
    NO_TGIT_PREMISSION(
        errorType = ErrorType.USER,
        errorCode = "2132038",
        formatErrorMessage = "User{0}does not have sufficient permissions" +
            "(root directory approval permissions of SVN or master permissions of GIT)"
    ),
    REQ_DEVCLOUD_ERROR(
        errorType = ErrorType.THIRD_PARTY,
        errorCode = "2132039",
        formatErrorMessage = "request devcloud {0} error {1}"
    ),
    REQ_BKVISION_ERROR(
        errorType = ErrorType.THIRD_PARTY,
        errorCode = "2132040",
        formatErrorMessage = "request bkvision {0} error {1}"
    ),
    REMOTEDEV_JOB_ERROR(
        errorType = ErrorType.SYSTEM,
        errorCode = "2132041",
        formatErrorMessage = "do remotedev job error {1}"
    ),
    REQ_TGIT_API_ERROR(
        errorType = ErrorType.THIRD_PARTY,
        errorCode = "2132042",
        formatErrorMessage = "request tgit api {0} error {1}"
    ),
    REMOTEDEV_CLIENT_IP_DUPLICATE_ERROR(
        errorType = ErrorType.THIRD_PARTY,
        errorCode = "2132043",
        formatErrorMessage = "client ip {0} request error: duplicate ip"
    ),
    REMOTEDEV_CLIENT_IP_NO_PERM_ERROR(
        errorType = ErrorType.THIRD_PARTY,
        errorCode = "2132044",
        formatErrorMessage = "client ip {0} request error: ip no permission to perform the operation {1}"
    ),
    MOA_VERIRY(
        errorType = ErrorType.THIRD_PARTY,
        errorCode = "2132045",
        formatErrorMessage = "req wesec moa_verify error {0}"
    );

    fun getErrorMessage(): String {
        return I18nUtil.getCodeLanMessage(this.errorCode)
    }
}
