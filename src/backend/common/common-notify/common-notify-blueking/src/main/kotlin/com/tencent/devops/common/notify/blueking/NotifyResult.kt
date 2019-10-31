package com.tencent.devops.common.notify.blueking

data class NotifyResult constructor(
        var Ret: Int,
        var ErrCode: Int,
        var ErrMsg: String,
        var StackTrace: String?,
        var data: Any?
) {
    constructor(errorMessage: String) : this(-1, 500, errorMessage, null, null)

    override fun toString(): String {
        return "Ret:$Ret, ErrCode:$ErrCode, ErrMsg:$ErrMsg, StackTrace:$String, data:$data"
    }
}