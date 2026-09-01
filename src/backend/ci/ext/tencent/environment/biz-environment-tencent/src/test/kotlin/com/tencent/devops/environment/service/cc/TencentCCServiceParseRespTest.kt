/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.environment.service.cc

import com.tencent.devops.environment.exception.CCApiException
import io.mockk.every
import io.mockk.spyk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * 针对 [TencentCCService] 4 个对外方法 + 共享 helper [TencentCCService.parseCCResp]
 * 的端到端单测。
 *
 * 验证策略：MockK 的 [spyk] 包装真实 `TencentCCService` 实例，并通过私有方法 stub
 * 把 [TencentCCService.executePostRequest] / [TencentCCService.executeDeleteRequest]
 * 替换为返回预设的 JSON 字符串，不真正走网络。这样可以在不修改任何生产代码（连可见性
 * 都不动）的前提下，覆盖 parseCCResp 的所有失败分支。
 *
 * 重点关注：
 * 1. 蓝鲸 Open API HTTP 200 + body 中 `result=false` 的失败响应（限频是典型场景）
 *    必须被识别为失败并抛 [CCApiException]，而不是被当作"成功但 data 为空"。
 * 2. 4 个对外方法对应的 4 类 `CCResp<T>` 形态都能正确反序列化成功。
 * 3. 空响应体 / 非法 JSON 也被正确转化为 [CCApiException]。
 */
class TencentCCServiceParseRespTest {

    /**
     * 用 spy 包装一个真实 [TencentCCService] 实例，把私有 `executePostRequest`
     * stub 成返回预设 JSON 字符串。`recordPrivateCalls = true` 是访问私有方法
     * 的必要选项（参考 `PipelineViewGroupServiceTest` / `RefreshTokenGranterTest` 等
     * 既有 MockK 用法）。
     */
    private fun spyServiceForPost(resBody: String?): TencentCCService {
        val service = spyk(TencentCCService(), recordPrivateCalls = true)
        every {
            service["executePostRequest"](
                any() as Map<String, String>,
                any() as String,
                any() as Any
            )
        } returns resBody
        return service
    }

    private fun spyServiceForDelete(resBody: String?): TencentCCService {
        val service = spyk(TencentCCService(), recordPrivateCalls = true)
        every {
            service["executeDeleteRequest"](
                any() as Map<String, String>,
                any() as String,
                any()
            )
        } returns resBody
        return service
    }

    // ============================================================
    // 4 个对外方法的正常响应：result=true，能成功反序列化
    // ============================================================

    @Test
    fun `listHostsWithoutBiz returns parsed page data on success`() {
        val json = """
            {
              "code": 0,
              "result": true,
              "request_id": "req-list-1",
              "message": "success",
              "data": {
                "count": 2,
                "info": [
                  {
                    "bk_host_id": 100,
                    "bk_cloud_id": 0,
                    "bk_host_innerip": "10.0.0.1",
                    "svr_id": 1001,
                    "bk_os_type": "linux"
                  },
                  {
                    "bk_host_id": 101,
                    "bk_cloud_id": 0,
                    "bk_host_innerip": "10.0.0.2",
                    "svr_id": 1002,
                    "bk_os_type": "windows"
                  }
                ]
              }
            }
        """.trimIndent()

        val service = spyServiceForPost(json)

        val resp = service.listHostsWithoutBiz(
            fields = listOf("bk_host_id", "svr_id"),
            inValueList = setOf(1001L, 1002L),
            field = "svr_id"
        )

        assertThat(resp.result).isTrue
        assertThat(resp.code).isEqualTo(0)
        assertThat(resp.requestId).isEqualTo("req-list-1")
        assertThat(resp.data?.count).isEqualTo(2)
        assertThat(resp.data?.info).hasSize(2)
        assertThat(resp.data?.info?.get(0)?.svrId).isEqualTo(1001)
        assertThat(resp.data?.info?.get(1)?.osType).isEqualTo("windows")
    }

    @Test
    fun `addHostToCiBiz returns parsed bk_host_ids on success`() {
        val json = """
            {
              "code": 0,
              "result": true,
              "request_id": "req-add-1",
              "message": "ok",
              "data": {
                "bk_host_ids": [201, 202, 203]
              }
            }
        """.trimIndent()

        val service = spyServiceForPost(json)

        val resp = service.addHostToCiBiz(listOf(2001L, 2002L, 2003L))

        assertThat(resp.result).isTrue
        assertThat(resp.data?.bkHostIds).containsExactly(201L, 202L, 203L)
    }

    @Test
    fun `deleteHostFromCiBiz returns parsed Nothing response on success`() {
        // CCResp<Nothing>：data 必须是 null
        val json = """
            {
              "code": 0,
              "result": true,
              "request_id": "req-delete-1",
              "message": "",
              "data": null
            }
        """.trimIndent()

        val service = spyServiceForDelete(json)

        val resp = service.deleteHostFromCiBiz(setOf(100L, 101L))

        assertThat(resp.result).isTrue
    }

    @Test
    fun `queryCCFindHostBizRelations returns parsed list on success`() {
        val json = """
            {
              "code": 0,
              "result": true,
              "request_id": "req-rel-1",
              "message": "ok",
              "data": [
                {
                  "bk_biz_id": 1,
                  "bk_module_id": 10,
                  "bk_supplier_account": "0",
                  "bk_host_id": 100,
                  "bk_set_id": 5
                }
              ]
            }
        """.trimIndent()

        val service = spyServiceForPost(json)

        val resp = service.queryCCFindHostBizRelations(listOf(100))

        assertThat(resp.result).isTrue
        assertThat(resp.data).hasSize(1)
        assertThat(resp.data?.get(0)?.bkHostId).isEqualTo(100)
        assertThat(resp.data?.get(0)?.bkBizId).isEqualTo(1)
    }

    @Test
    fun `unknown fields in response json are silently ignored`() {
        // 模拟蓝鲸将来给响应或 CCHost 加新字段：FAIL_ON_UNKNOWN_PROPERTIES=false 应当兜住
        val json = """
            {
              "code": 0,
              "result": true,
              "request_id": "req-extra-1",
              "message": "ok",
              "future_field_at_top_level": "value_we_dont_know",
              "data": {
                "count": 1,
                "future_field_in_page_data": 42,
                "info": [
                  {
                    "bk_host_id": 100,
                    "bk_cloud_id": 0,
                    "bk_host_innerip": "10.0.0.1",
                    "svr_id": 1001,
                    "bk_os_type": "linux",
                    "future_field_in_host": "ignored"
                  }
                ]
              }
            }
        """.trimIndent()

        val service = spyServiceForPost(json)

        val resp = service.listHostsWithoutBiz(listOf("any"), setOf(1L), "svr_id")

        assertThat(resp.result).isTrue
        assertThat(resp.data?.info?.get(0)?.bkHostId).isEqualTo(100)
    }

    // ============================================================
    // 业务级失败：HTTP 200 但 result=false，必须抛 CCApiException
    // ============================================================

    /**
     * 本次修复的核心场景：蓝鲸网关返回限频。HTTP 状态码 200，body 是
     * `{"code":1642902,"result":false,"data":null,...}`。
     * 这种响应若未识别，调用方会把 `data=null` 当成"在 CC 中查无此机器"，
     * 把对应节点错误地置为 NOT_IN_CC。
     */
    @Test
    fun `rate limit response on queryCCFindHostBizRelations throws CCApiException with full context`() {
        val json = """
            {
              "code": 1642902,
              "message": "API rate limit exceeded by stage strategy",
              "result": false,
              "code_name": "RATE_LIMIT_RESTRICTION",
              "request_id": "req-rate-limit-xyz",
              "data": null
            }
        """.trimIndent()

        val service = spyServiceForPost(json)

        val ex = assertThrows<CCApiException> {
            service.queryCCFindHostBizRelations(listOf(100, 101, 102))
        }

        assertThat(ex.code).isEqualTo(1642902)
        assertThat(ex.codeName).isEqualTo("RATE_LIMIT_RESTRICTION")
        assertThat(ex.requestId).isEqualTo("req-rate-limit-xyz")
        // 异常 message 应当包含蓝鲸侧的原始错误描述，方便线上日志快速定位
        assertThat(ex.message)
            .contains("1642902")
            .contains("RATE_LIMIT_RESTRICTION")
            .contains("rate limit")
            .contains("req-rate-limit-xyz")
    }

    @Test
    fun `rate limit response on listHostsWithoutBiz also throws CCApiException`() {
        // 同样的限频 body 在 listHostsWithoutBiz 这条路径上也必须抛
        val json = """
            {
              "code": 1642902,
              "message": "API rate limit exceeded by stage strategy",
              "result": false,
              "code_name": "RATE_LIMIT_RESTRICTION",
              "request_id": "req-list-rate-limit",
              "data": null
            }
        """.trimIndent()

        val service = spyServiceForPost(json)

        val ex = assertThrows<CCApiException> {
            service.listHostsWithoutBiz(listOf("svr_id"), setOf(1L, 2L), "svr_id")
        }

        assertThat(ex.code).isEqualTo(1642902)
        assertThat(ex.codeName).isEqualTo("RATE_LIMIT_RESTRICTION")
    }

    @Test
    fun `non-rate-limit business failure also throws`() {
        val json = """
            {
              "code": 1199000,
              "message": "Invalid app code",
              "result": false,
              "code_name": "INVALID_PARAMS",
              "request_id": "req-bad-1",
              "data": null
            }
        """.trimIndent()

        val service = spyServiceForPost(json)

        val ex = assertThrows<CCApiException> {
            service.addHostToCiBiz(listOf(1L))
        }

        assertThat(ex.code).isEqualTo(1199000)
        assertThat(ex.codeName).isEqualTo("INVALID_PARAMS")
    }

    @Test
    fun `delete request with business failure throws`() {
        // 验证 DELETE 路径同样会校验 result
        val json = """
            {
              "code": 1199001,
              "message": "permission denied",
              "result": false,
              "code_name": "PERMISSION_DENIED",
              "request_id": "req-delete-fail",
              "data": null
            }
        """.trimIndent()

        val service = spyServiceForDelete(json)

        val ex = assertThrows<CCApiException> {
            service.deleteHostFromCiBiz(setOf(1L, 2L))
        }

        assertThat(ex.code).isEqualTo(1199001)
        assertThat(ex.codeName).isEqualTo("PERMISSION_DENIED")
    }

    @Test
    fun `result field missing is treated as failure`() {
        // 蓝鲸响应理论上不会缺 result，但出于防御性，CCResp.result 是 Boolean?
        // helper 用 `resp.result != true` 判定 — null 与 false 都视为失败
        val json = """
            {
              "code": 0,
              "message": "weird response missing result field",
              "request_id": "req-null-result",
              "data": null
            }
        """.trimIndent()

        val service = spyServiceForPost(json)

        assertThrows<CCApiException> {
            service.listHostsWithoutBiz(listOf("svr_id"), setOf(1L), "svr_id")
        }
    }

    @Test
    fun `business failure does not leak partial data to caller`() {
        // 即便失败响应中 data 非 null，也不应被当成"成功的部分数据"返回给上层
        val json = """
            {
              "code": 1642902,
              "message": "rate limit",
              "result": false,
              "code_name": "RATE_LIMIT_RESTRICTION",
              "request_id": "req-partial-data",
              "data": {
                "count": 999,
                "info": [
                  {
                    "bk_host_id": 999,
                    "bk_cloud_id": 0,
                    "bk_host_innerip": "1.2.3.4",
                    "svr_id": 999,
                    "bk_os_type": "linux"
                  }
                ]
              }
            }
        """.trimIndent()

        val service = spyServiceForPost(json)

        // 关键断言：抛出异常，不会把 partial data 当成成功响应返还
        assertThrows<CCApiException> {
            service.listHostsWithoutBiz(listOf("svr_id"), setOf(1L), "svr_id")
        }
    }

    // ============================================================
    // 异常输入：空响应体 / 非法 JSON
    // ============================================================

    @Test
    fun `null response body throws CCApiException`() {
        val service = spyServiceForPost(null)

        val ex = assertThrows<CCApiException> {
            service.listHostsWithoutBiz(listOf("svr_id"), setOf(1L), "svr_id")
        }

        assertThat(ex.message).contains("Response body is blank")
        assertThat(ex.code).isNull()
    }

    @Test
    fun `blank response body throws CCApiException`() {
        val service = spyServiceForPost("   \n\t  ")

        val ex = assertThrows<CCApiException> {
            service.queryCCFindHostBizRelations(listOf(1))
        }

        assertThat(ex.message).contains("Response body is blank")
    }

    @Test
    fun `malformed json throws CCApiException with original parse error as cause`() {
        val service = spyServiceForPost("""{ this is not valid json""")

        val ex = assertThrows<CCApiException> {
            service.listHostsWithoutBiz(listOf("svr_id"), setOf(1L), "svr_id")
        }

        assertThat(ex.message).contains("Deserialize CC response failed")
        // 链路保留：Jackson 抛的 JsonParseException 应作为 cause 暴露
        assertThat(ex.cause).isNotNull
    }

    @Test
    fun `html error page from gateway throws CCApiException`() {
        // 模拟网关挂掉返回 HTML 错误页（502 之类），不是合法 JSON
        val service = spyServiceForPost("<html><body><h1>502 Bad Gateway</h1></body></html>")

        val ex = assertThrows<CCApiException> {
            service.addHostToCiBiz(listOf(1L))
        }

        assertThat(ex.message).contains("Deserialize CC response failed")
    }
}
