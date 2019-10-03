package com.tencent.devops.dispatch.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * {
 *   "code": 0,
 *   "data": {
 *     "cluster_id": "BCS-K8S-15018",
 *     "created_at": "2018-06-26T20:35:03.144638659+08:00",
 *     "creator": "pipeline",
 *     "description": "",
 *     "env_type": "dev",
 *     "id": 118,
 *     "name": "test-by-api",
 *     "project_id": "b3b58d228f244c13b83bef3af882155c",
 *     "status": "",
 *     "updated_at": "2018-06-26T20:35:03.144638659+08:00"
 *   },
 *   "message": "注册Namespace成功",
 *   "request_id": "73466ffa-fb7d-4306-8265-f1655afbde8d",
 *   "result": true
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class BCSCreateNamespaceResponse(
    val code: Int,
    val message: String,
    val request_id: String,
    val result: Boolean,
    val data: BCSCreateNamespaceData
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BCSCreateNamespaceData(
    val cluster_id: String,
    val created_at: String,
    val creator: String,
    val env_type: String,
    val id: Long,
    val name: String,
    val project_id: String
)