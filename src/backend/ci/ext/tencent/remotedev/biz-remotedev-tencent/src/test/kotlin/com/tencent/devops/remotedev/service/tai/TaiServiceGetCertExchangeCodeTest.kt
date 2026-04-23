package com.tencent.devops.remotedev.service.tai

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.remotedev.pojo.tai.CertExchangeCodeData
import com.tencent.devops.remotedev.pojo.tai.CertExchangeCodeReq
import com.tencent.devops.remotedev.pojo.tai.CertExchangeCodeResp
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import com.tencent.devops.common.api.util.OkhttpUtils
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.lang.reflect.Field

class TaiServiceGetCertExchangeCodeTest {

    private lateinit var taiService: TaiService

    @BeforeEach
    fun setUp() {
        taiService = TaiService()
        setField(taiService, "taiUrl", "https://test.tai.example.com")
        setField(taiService, "taiPassid", "test-paasid")
        setField(taiService, "taiToken", "test-token")

        mockkStatic(OkhttpUtils::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getCertExchangeCode should return code on success`() {
        val expectedResp = CertExchangeCodeResp(
            code = 0,
            msg = "get code success",
            data = CertExchangeCodeData(code = "382916")
        )
        val responseBody = jacksonObjectMapper().writeValueAsString(expectedResp)

        every { OkhttpUtils.doHttp(any()) } returns buildOkResponse(responseBody)

        val result = taiService.getCertExchangeCode(
            userId = "testuser",
            req = CertExchangeCodeReq(username = "testuser")
        )

        assertEquals(0, result.code)
        assertEquals("get code success", result.msg)
        assertNotNull(result.data)
        assertEquals("382916", result.data!!.code)
    }

    @Test
    fun `getCertExchangeCode should throw on biz error code`() {
        val errorResp = CertExchangeCodeResp(
            code = 1100,
            msg = "internal error",
            data = null
        )
        val responseBody = jacksonObjectMapper().writeValueAsString(errorResp)

        every { OkhttpUtils.doHttp(any()) } returns buildOkResponse(responseBody)

        assertThrows(RemoteServiceException::class.java) {
            taiService.getCertExchangeCode(
                userId = "testuser",
                req = CertExchangeCodeReq(username = "testuser")
            )
        }
    }

    @Test
    fun `getCertExchangeCode should throw on http error`() {
        every { OkhttpUtils.doHttp(any()) } returns buildErrorResponse(500, "Internal Server Error")

        assertThrows(RemoteServiceException::class.java) {
            taiService.getCertExchangeCode(
                userId = "testuser",
                req = CertExchangeCodeReq(username = "testuser")
            )
        }
    }

    private fun buildOkResponse(body: String): Response {
        return Response.Builder()
            .code(200)
            .message("OK")
            .protocol(Protocol.HTTP_1_1)
            .request(Request.Builder().url("https://test.tai.example.com/qrcode/backend/get").build())
            .body(body.toResponseBody(null))
            .build()
    }

    private fun buildErrorResponse(code: Int, message: String): Response {
        return Response.Builder()
            .code(code)
            .message(message)
            .protocol(Protocol.HTTP_1_1)
            .request(Request.Builder().url("https://test.tai.example.com/qrcode/backend/get").build())
            .body("".toResponseBody(null))
            .build()
    }

    private fun setField(target: Any, fieldName: String, value: Any) {
        val field: Field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }
}
