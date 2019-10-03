package com.tencent.devops.dispatch.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * {
 *   "code": 0,
 *   "data": {
 *     "deleted_id": "118"
 *   },
 *   "message": "Namespace delete success!",
 *   "request_id": "d36867af-e8e8-441f-9fde-ddb56cf11b12",
 *   "result": true
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class BCSDeleteNamespaceResponse(
    val code: Int,
    val message: String,
    val request_id: String,
    val result: Boolean
)