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

package com.tencent.devops.environment.service.job

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.environment.pojo.job.jobcloudres.JobCloudQueryExecuteObjectLogsResult
import jakarta.ws.rs.core.Response.Status
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ApigwJobCloudApiV4ResTest {

    private val api = ApigwJobCloudApi()

    @Test
    fun `script step logs are parsed and container field is ignored`() {
        val json = """
            {
              "data": {
                "job_instance_id": 100,
                "step_instance_id": 200,
                "log_type": 1,
                "script_step_execute_object_logs": [
                  {
                    "execute_object": {
                      "type": 1,
                      "resource_id": "1",
                      "host": {"bk_host_id": 1, "ip": "127.0.0.1", "bk_cloud_id": 0},
                      "container": {"id": 1001, "container_id": "abc", "name": "c1"}
                    },
                    "log_content": "job_start\n"
                  }
                ]
              }
            }
        """.trimIndent()

        val result = parse(buildResponse(200, json, requestId = "req-1"))

        assertThat(result.result).isTrue
        assertThat(result.jobRequestId).isEqualTo("req-1")
        val data = result.data!!
        assertThat(data.jobInstanceId).isEqualTo(100L)
        assertThat(data.logType).isEqualTo(1)
        val log = data.scriptStepExecuteObjectLogs!!.single()
        assertThat(log.executeObject?.host?.bkHostId).isEqualTo(1L)
        assertThat(log.executeObject?.host?.ip).isEqualTo("127.0.0.1")
        assertThat(log.logContent).isEqualTo("job_start\n")
    }

    @Test
    fun `file step logs are parsed`() {
        val json = """
            {
              "data": {
                "job_instance_id": 100,
                "step_instance_id": 200,
                "log_type": 2,
                "file_step_execute_object_logs": [
                  {
                    "execute_object": {"type": 1, "resource_id": "1", "host": {"bk_host_id": 1}},
                    "download_log": [
                      {
                        "mode": 1,
                        "src_execute_object": {"type": 1, "resource_id": "2", "host": {"bk_host_id": 2}},
                        "src_path": "/data/1.log",
                        "dest_execute_object": {"type": 1, "resource_id": "1", "host": {"bk_host_id": 1}},
                        "dest_path": "/tmp/1.log",
                        "status": 4,
                        "log_content": "success",
                        "size": "1.0 Bytes",
                        "speed": "0 KB/s",
                        "process": "100%"
                      }
                    ]
                  }
                ]
              }
            }
        """.trimIndent()

        val data = parse(buildResponse(200, json)).data!!

        val fileLog = data.fileStepExecuteObjectLogs!!.single()
        assertThat(fileLog.uploadLog).isNull()
        val download = fileLog.downloadLog!!.single()
        assertThat(download.srcExecuteObject?.host?.bkHostId).isEqualTo(2L)
        assertThat(download.destPath).isEqualTo("/tmp/1.log")
        assertThat(download.status).isEqualTo(4)
    }

    @Test
    fun `4xx response is mapped to BAD_REQUEST with job error message`() {
        val json = """{"error": {"code": "INVALID_ARGUMENT", "message": "invalid bk_scope_type"}}"""

        val ex = assertThrows<CustomException> { parse(buildResponse(400, json, requestId = "req-400")) }

        assertThat(ex.status).isEqualTo(Status.BAD_REQUEST)
        assertThat(ex.message).contains("INVALID_ARGUMENT").contains("invalid bk_scope_type").contains("req-400")
    }

    @Test
    fun `403 response is mapped to BAD_REQUEST`() {
        val json = """
            {"error": {"code": "NO_PERMISSION", "message": "no permission", "data": {"system_id": "bk_job"}}}
        """.trimIndent()

        val ex = assertThrows<CustomException> { parse(buildResponse(403, json)) }

        assertThat(ex.status).isEqualTo(Status.BAD_REQUEST)
        assertThat(ex.message).contains("NO_PERMISSION")
    }

    @Test
    fun `5xx response with non-json body is mapped to INTERNAL_SERVER_ERROR`() {
        val ex = assertThrows<CustomException> {
            parse(buildResponse(502, "<html>502 Bad Gateway</html>"))
        }

        assertThat(ex.status).isEqualTo(Status.INTERNAL_SERVER_ERROR)
        assertThat(ex.message).contains("502")
    }

    @Test
    fun `empty success body throws INTERNAL_SERVER_ERROR`() {
        val ex = assertThrows<CustomException> { parse(buildResponse(200, "")) }

        assertThat(ex.status).isEqualTo(Status.INTERNAL_SERVER_ERROR)
    }

    private fun parse(response: Response) =
        api.getResultFromV4Res(response, JobCloudQueryExecuteObjectLogsResult::class.java)

    private fun buildResponse(code: Int, body: String, requestId: String? = null): Response {
        val builder = Response.Builder()
            .request(Request.Builder().url("http://job.test/api/v4/batch_get_job_instance_execute_object_log").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("mock")
            .body(body.toResponseBody())
        requestId?.let { builder.header("X-Bkapi-Request-Id", it) }
        return builder.build()
    }
}
