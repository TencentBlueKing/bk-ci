package com.tencent.devops.remotedev.filter.impl

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.remotedev.service.redis.ConfigCacheService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.core.MultivaluedHashMap
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriInfo
import java.net.URI
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WhitelistCoffeeAIFilterTest {

    private lateinit var cacheService: ConfigCacheService
    private lateinit var client: Client
    private lateinit var serviceTxUserResource: ServiceTxUserResource
    private lateinit var filter: WhitelistCoffeeAIFilter

    @BeforeEach
    fun setUp() {
        cacheService = mockk(relaxed = true)
        client = mockk(relaxed = true)
        serviceTxUserResource = mockk(relaxed = true)

        every { client.get(ServiceTxUserResource::class) } returns serviceTxUserResource

        filter = WhitelistCoffeeAIFilter(
            cacheService = cacheService,
            client = client
        )
    }

    @Test
    fun `non coffee ai path passes directly`() {
        val ctx = buildRequestContext(path = "/api/user/remotedev/settings")

        assertTrue(filter.verify(ctx))

        verify(exactly = 0) { cacheService.checkApiCoffeeAIWhiteList(any()) }
        verify(exactly = 0) { client.get(ServiceTxUserResource::class) }
    }

    @Test
    fun `user in coffee ai whitelist passes directly`() {
        every { cacheService.checkApiCoffeeAIWhiteList(USER_ID) } returns true

        val ctx = buildRequestContext()

        assertTrue(filter.verify(ctx))

        verify(exactly = 1) { cacheService.checkApiCoffeeAIWhiteList(USER_ID) }
        verify(exactly = 0) { client.get(ServiceTxUserResource::class) }
    }

    @Test
    fun `user bg in coffee ai whitelist passes`() {
        every { cacheService.checkApiCoffeeAIWhiteList(USER_ID) } returns false
        every { serviceTxUserResource.get(USER_ID) } returns Result(buildUserDeptDetail(bgName = BG_NAME))
        every { cacheService.checkApiCoffeeAIWhiteList(BG_NAME) } returns true

        val ctx = buildRequestContext()

        assertTrue(filter.verify(ctx))

        verify(exactly = 1) { serviceTxUserResource.get(USER_ID) }
        verify(exactly = 1) { cacheService.checkApiCoffeeAIWhiteList(BG_NAME) }
    }

    @Test
    fun `user and bg not in coffee ai whitelist are blocked`() {
        every { cacheService.checkApiCoffeeAIWhiteList(USER_ID) } returns false
        every { serviceTxUserResource.get(USER_ID) } returns Result(buildUserDeptDetail(bgName = BG_NAME))
        every { cacheService.checkApiCoffeeAIWhiteList(BG_NAME) } returns false

        val ctx = buildRequestContext()

        assertFalse(filter.verify(ctx))
    }

    @Test
    fun `user info lookup failure is blocked`() {
        every { cacheService.checkApiCoffeeAIWhiteList(USER_ID) } returns false
        every { serviceTxUserResource.get(USER_ID) } throws RuntimeException("lookup failed")

        val ctx = buildRequestContext()

        assertFalse(filter.verify(ctx))
    }

    @Test
    fun `missing user header aborts request and skips whitelist check`() {
        val ctx = buildRequestContext(userId = null)

        assertTrue(filter.verify(ctx))

        verify(exactly = 1) { ctx.abortWith(any<Response>()) }
        verify(exactly = 0) { cacheService.checkApiCoffeeAIWhiteList(any()) }
    }

    private fun buildRequestContext(
        userId: String? = USER_ID,
        path: String = "/api/user/coffee_ai/token"
    ): ContainerRequestContext {
        val ctx = mockk<ContainerRequestContext>(relaxed = true)
        val uriInfo = mockk<UriInfo>()
        every { uriInfo.requestUri } returns URI.create(path)
        every { ctx.uriInfo } returns uriInfo

        val headers = MultivaluedHashMap<String, String>()
        userId?.let { headers.putSingle(AUTH_HEADER_DEVOPS_USER_ID, it) }
        every { ctx.headers } returns headers

        return ctx
    }

    private fun buildUserDeptDetail(bgName: String): UserDeptDetail {
        return UserDeptDetail(
            bgName = bgName,
            bgId = "bg-id",
            businessLineName = "business-line",
            businessLineId = "business-line-id",
            deptName = "dept",
            deptId = "dept-id",
            centerName = "center",
            centerId = "center-id",
            groupId = "group-id",
            groupName = "group",
            userId = USER_ID,
            name = "Test User"
        )
    }

    companion object {
        private const val USER_ID = "test-user"
        private const val BG_NAME = "IEG"
    }
}
