package com.tencent.devops.project.pojo.tof

/**
 * deng
 * 2019-01-14
 * {
 *   "message": "",
 *   "code": "00",
 *   "data": [
 *     {
 *       "DisplayName": "蓝盾"
 *     }
 *   ],
 *   "result": true,
 *   "request_id": "e334a02f715f4ae4b5fa0b3b152398e1"
 * }
 */
data class CCAppNameResponse(
    val DisplayName: String,
    val isNotImportant: String? // TODO 不知道为啥只有一个field json 解析都失败，加上这个先
)
