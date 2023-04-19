package com.tencent.devops.remotedev.service

import com.tencent.devops.common.api.constant.HTTP_401
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OauthForbiddenException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.remotedev.RemoteDevDispatcher
import com.tencent.devops.common.service.Profile
import com.tencent.devops.common.test.BkCiAbstractTest
import com.tencent.devops.common.websocket.dispatch.WebSocketDispatcher
import com.tencent.devops.remotedev.config.CommonConfig
import com.tencent.devops.remotedev.dao.RemoteDevBillingDao
import com.tencent.devops.remotedev.dao.RemoteDevSettingDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceOpHistoryDao
import com.tencent.devops.remotedev.dao.WorkspaceSharedDao
import com.tencent.devops.remotedev.pojo.RemoteDevGitType
import com.tencent.devops.remotedev.service.redis.RedisCacheService
import com.tencent.devops.remotedev.service.redis.RedisHeartBeat
import com.tencent.devops.remotedev.service.transfer.RemoteDevGitTransfer
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.format.DateTimeFormatter

internal class WorkspaceServiceTest : BkCiAbstractTest() {
    private val redisOperation: RedisOperation = mockk()
    private val workspaceDao: WorkspaceDao = mockk()
    private val workspaceHistoryDao: WorkspaceHistoryDao = mockk()
    private val workspaceOpHistoryDao: WorkspaceOpHistoryDao = mockk()
    private val workspaceSharedDao: WorkspaceSharedDao = mockk()
    private val remoteDevGitTransfer: RemoteDevGitTransfer = mockk()
    private val permissionService: PermissionService = mockk()
    private val sshService: SshPublicKeysService = mockk()
    private val dispatcher: RemoteDevDispatcher = mockk()
    private var formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val remoteDevSettingDao: RemoteDevSettingDao = mockk()
    private val redisHeartBeat: RedisHeartBeat = mockk()
    private val webSocketDispatcher: WebSocketDispatcher = mockk()
    private val remoteDevBillingDao: RemoteDevBillingDao = mockk()
    private val commonService: CommonService = mockk()
    private val profile: Profile = mockk()
    private val redisCache: RedisCacheService = mockk()
    private val commonConfig: CommonConfig = mockk()
    private val self: WorkspaceService = spyk(
        WorkspaceService(
            dslContext = dslContext,
            redisOperation = redisOperation,
            workspaceDao = workspaceDao,
            workspaceHistoryDao = workspaceHistoryDao,
            workspaceOpHistoryDao = workspaceOpHistoryDao,
            workspaceSharedDao = workspaceSharedDao,
            remoteDevGitTransfer = remoteDevGitTransfer,
            permissionService = permissionService,
            sshService = sshService,
            client = client,
            dispatcher = dispatcher,
            remoteDevSettingDao = remoteDevSettingDao,
            webSocketDispatcher = webSocketDispatcher,
            redisHeartBeat = redisHeartBeat,
            remoteDevBillingDao = remoteDevBillingDao,
            commonService = commonService,
            redisCache = redisCache,
            profile = profile,
            commonConfig = commonConfig
        ),
        recordPrivateCalls = true
    )

    @Nested
    inner class ThrowableTest {
        @Test
        fun getAuthorizedGitRepository_1() {
            every {
                remoteDevGitTransfer.load(RemoteDevGitType.GIT).getProjectList(
                    any(), any(), any(), any(), any(), any()
                )
            } throws RemoteServiceException(
                errorMessage = "ops! it's error!下游服务抛出了异常，并且返回了401", httpStatus = HTTP_401
            )
            Assertions.assertThrows(ErrorCodeException::class.java) {
                self.getAuthorizedGitRepository(
                    userId = "user00", search = null, page = null, pageSize = null, gitType = RemoteDevGitType.GIT
                )
            }
        }

        @Test
        fun getAuthorizedGitRepository_2() {
            every {
                remoteDevGitTransfer.load(RemoteDevGitType.GIT).getProjectList(
                    any(), any(), any(), any(), any(), any()
                )
            } throws OauthForbiddenException("用户[user00]尚未进行OAUTH授权，请先授权。")
            Assertions.assertThrows(ErrorCodeException::class.java) {
                self.getAuthorizedGitRepository(
                    userId = "user00", search = null, page = null, pageSize = null, gitType = RemoteDevGitType.GIT
                )
            }
        }
    }
}
