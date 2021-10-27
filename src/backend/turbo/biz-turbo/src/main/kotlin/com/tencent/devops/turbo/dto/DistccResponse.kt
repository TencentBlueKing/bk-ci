package com.tencent.devops.turbo.dto

data class DistccResponse<T>(
    val message: String,
    val result: Boolean,
    val length: Int,
    val code: Int,
    val data: T
) {
    companion object {
        private const val SUCCESS_CODE: Int = 0
    }

    fun isSuccessful(): Boolean {
        return result && code == SUCCESS_CODE
    }
}
