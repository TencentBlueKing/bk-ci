package com.tencent.devops.dispatch.pojo

/**
 * {
 *   "code": 0,
 *   "message": "OK",
 *   "data": {
 *     "version_id": 5621,
 *     "template_id": 805,
 *     "inst_id_list": [
 *       2318
 *     ]
 *   }
 * }
 */
data class BCSCreateInstanceResponse(
    val code: Int,
    val message: String,
    val data: BCSCreateInstanceData
)

data class BCSCreateInstanceData(
    val version_id: Int,
    val template_id: Int,
    val inst_id_list: List<Long>
)