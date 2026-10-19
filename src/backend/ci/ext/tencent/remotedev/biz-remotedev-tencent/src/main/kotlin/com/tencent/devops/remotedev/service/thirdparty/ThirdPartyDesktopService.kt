package com.tencent.devops.remotedev.service.thirdparty

import com.tencent.devops.common.api.constant.CommonMessageCode.PARAMETER_ILLEGAL_ERROR
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.remotedev.dao.ProjectStartAppLinkDao
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.dao.WorkspaceJoinDao
import com.tencent.devops.remotedev.dispatch.kubernetes.interfaces.ServiceStartCloudInterface
import com.tencent.devops.remotedev.pojo.remotedev.FetchWinPoolData
import com.tencent.devops.remotedev.dispatch.kubernetes.service.factory.RemoteDevServiceFactory
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceOwnerType
import com.tencent.devops.remotedev.pojo.WorkspaceStatus
import com.tencent.devops.remotedev.pojo.expert.WorkspaceTaskStatus
import com.tencent.devops.remotedev.pojo.op.OpProjectWorkspaceAssignData
import com.tencent.devops.remotedev.pojo.remotedev.CreateCvmResp
import com.tencent.devops.remotedev.pojo.remotedev.RegisterVmData
import com.tencent.devops.remotedev.pojo.thirdparty.ThirdPartyDesktopBinding
import com.tencent.devops.remotedev.pojo.thirdparty.ThirdPartyDesktopCdsKeyResp
import com.tencent.devops.remotedev.pojo.thirdparty.ThirdPartyDesktopInstallCommandResp
import com.tencent.devops.remotedev.pojo.thirdparty.ThirdPartyDesktopTokenInfo
import com.tencent.devops.remotedev.pojo.thirdparty.ThirdPartyDesktopTokenResp
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.client.StartCloudClient
import com.tencent.devops.remotedev.service.redis.ConfigCacheService
import com.tencent.devops.remotedev.service.workspace.CreateControl
import jakarta.ws.rs.core.Response
import java.security.SecureRandom
import java.util.Base64
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * 一个 token 的注册记录，存 Redis
 * 后续 taskStatus / cdsKey / assign 接口只允许操作这里记录的资源
 */
data class ThirdPartyDesktopRegisterRecord(
    val ip: String,
    /* 重装 token 没有注册任务，为 null */
    val taskId: String?,
    val envId: String?,
    /* 用于重复调用 register 时原样返回 */
    val taskUid: String? = null
)

/**
 * 第三方机器注册为云桌面相关能力
 *
 * token 为 32 位随机串（24 字节 SecureRandom，base64url），本身不携带任何信息
 * userId/os/zoneId/projectId 存在 Redis 中，key 使用 token 的 sha256，Redis 中不落 token 明文
 * 一个 token 只能注册一台机器，后续接口的入参必须属于该 token 的注册记录
 *
 * 重装场景：根据已有 cdsId 签发 token，签发时直接写入注册记录（envId = cdsId），不能注册新机器；
 * 机器未分配时允许分配，已分配则拒绝（避免重复加项目配额）
 *
 * 注册失败重试：BCS 调用失败直接释放占位；BCS 任务失败(failed/abort)后，同一个 token 可以再注册一次
 */
@Service
class ThirdPartyDesktopService @Autowired constructor(
    private val remoteDevServiceFactory: RemoteDevServiceFactory,
    private val startCloudClient: StartCloudClient,
    private val createControl: CreateControl,
    private val redisOperation: RedisOperation,
    private val permissionService: PermissionService,
    private val dslContext: DSLContext,
    private val workspaceJoinDao: WorkspaceJoinDao,
    private val workspaceDao: WorkspaceDao,
    private val configCacheService: ConfigCacheService,
    private val projectStartAppLinkDao: ProjectStartAppLinkDao
) {

    /* 第三方云桌面 token 有效期（秒），默认 1 小时 */
    @Value("\${remoteDev.thirdPartyDesktop.expireSeconds:3600}")
    private val expireSeconds: Long = DEFAULT_EXPIRE_SECONDS

    /* cdsId 与注册人的绑定有效期（天），用于未分配机器的重装，默认 30 天 */
    @Value("\${remoteDev.thirdPartyDesktop.bindingExpireDays:30}")
    private val bindingExpireDays: Long = DEFAULT_BINDING_EXPIRE_DAYS

    /* 安装脚本下载地址，必须配置，可被 Redis key [SCRIPT_URL_KEY] 覆盖 */
    @Value("\${remoteDev.thirdPartyDesktop.scriptUrl:}")
    private val scriptUrl: String = ""

    /* 脚本在用户机器上回调的 remotedev 地址，按环境配置，可被 Redis key [API_HOST_KEY] 覆盖 */
    @Value("\${remoteDev.thirdPartyDesktop.apiHost:}")
    private val apiHost: String = ""

    private val secureRandom = SecureRandom()

    /**
     * 校验通过的 token：id 为 token 的 sha256，用于拼接各个 Redis key
     */
    private data class VerifiedToken(
        val id: String,
        val info: ThirdPartyDesktopTokenInfo
    )

    /**
     * 生成第三方机器注册云桌面的安装命令：签发一次性 token 后拼进对应系统的命令模板
     * 目前只有 WINDOWS 有脚本，其他系统返回空命令且不签发 token
     * gameId 取项目在 T_PROJECT_START_APP_LINK 中映射的 START appid，查不到报错
     */
    fun genInstallCommand(
        userId: String,
        projectId: String,
        zoneId: String,
        os: OS
    ): ThirdPartyDesktopInstallCommandResp {
        if (os != OS.WINDOWS) {
            logger.info("genInstallCommand|os not supported, return empty|$userId|$projectId|$zoneId|$os")
            return ThirdPartyDesktopInstallCommandResp(command = "", expireTime = 0L)
        }
        // 参数会拼进 PowerShell 命令，必须严格校验，避免命令注入
        val zone = zoneId.trim()
        if (!ZONE_REGEX.matches(zone)) {
            throw illegal("zoneId $zoneId is illegal, must end with digits, e.g. GZ11")
        }
        val gameId = projectStartAppLinkDao.getAppId(dslContext, projectId)?.toString()
        if (gameId.isNullOrBlank() || !GAME_ID_REGEX.matches(gameId)) {
            logger.warn("genInstallCommand|gameId not found|$userId|$projectId|$gameId")
            throw notFound("project $projectId has no gameId mapping")
        }
        val url = resolveUrlConfig(SCRIPT_URL_KEY, scriptUrl, "scriptUrl")
        val host = resolveUrlConfig(API_HOST_KEY, apiHost, "apiHost").trimEnd('/')
        val tokenResp = genToken(userId = userId, projectId = projectId, os = os.name, zoneId = zone)
        val command = buildWindowsInstallCommand(
            token = tokenResp.token,
            zoneId = zone,
            apiHost = host,
            gameIdList = gameId,
            scriptUrl = url
        )
        logger.info("genInstallCommand|$userId|$projectId|$zone|$os|$gameId|$host|$url")
        return ThirdPartyDesktopInstallCommandResp(command = command, expireTime = tokenResp.expireTime)
    }

    /**
     * 配置优先级：Redis(含 T_CONFIG 表，ConfigCacheService 本地缓存 1 分钟) > 配置文件
     * 只接受 http(s) 地址，且不能包含引号、空白等会破坏 PowerShell 命令的字符
     */
    private fun resolveUrlConfig(redisKey: String, configValue: String, name: String): String {
        val override = configCacheService.get(redisKey)?.trim()?.ifBlank { null }
        val value = override ?: configValue.trim()
        if (value.isBlank()) {
            logger.error("genInstallCommand|[remoteDev.thirdPartyDesktop.$name] is not configured")
            throw ErrorCodeException(
                statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                errorCode = PARAMETER_ILLEGAL_ERROR,
                defaultMessage = "[remoteDev.thirdPartyDesktop.$name] is not configured",
                params = arrayOf("config", "[remoteDev.thirdPartyDesktop.$name] is not configured")
            )
        }
        if (!SAFE_URL_REGEX.matches(value)) {
            logger.error("genInstallCommand|$name is illegal|$value|fromRedis=${override != null}")
            throw ErrorCodeException(
                statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                errorCode = PARAMETER_ILLEGAL_ERROR,
                defaultMessage = "thirdPartyDesktop $name is illegal",
                params = arrayOf("config", "thirdPartyDesktop $name is illegal")
            )
        }
        return value
    }

    /**
     * 命令模板见 CDS《第三方云桌面注册 —— 后台对接说明》，token 走环境变量，不出现在子进程命令行
     */
    private fun buildWindowsInstallCommand(
        token: String,
        zoneId: String,
        apiHost: String,
        gameIdList: String,
        scriptUrl: String
    ): String = listOf(
        "\$env:CDS_TOKEN = ${psQuote(token)}",
        "\$env:CDS_ZONE  = ${psQuote(zoneId)}",
        "\$env:CDS_HOST  = ${psQuote(apiHost)}",
        "\$env:CDS_GAME_ID_LIST = ${psQuote(gameIdList)}",
        "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12",
        "\$f = Join-Path \$env:TEMP 'cds_register_thirdparty.ps1'",
        "Invoke-WebRequest -UseBasicParsing -Uri ${psQuote(scriptUrl)} -OutFile \$f",
        "if (-not (Select-String -Path \$f -Pattern 'Requires -Version' -Quiet)) {",
        "    Write-Host '脚本下载异常（内容不符合预期），请检查网络或联系管理员' -ForegroundColor Red",
        "} else {",
        "    powershell -NoProfile -ExecutionPolicy Bypass -File \$f",
        "    Write-Host \"退出码: \$LASTEXITCODE\"",
        "}",
        "Remove-Item Env:CDS_TOKEN, Env:CDS_ZONE, Env:CDS_HOST, Env:CDS_GAME_ID_LIST -ErrorAction SilentlyContinue",
        "Remove-Item \$f -ErrorAction SilentlyContinue"
    ).joinToString("\n")

    /* PowerShell 单引号字符串：内部单引号写两次，单引号内 $ 不会被展开 */
    private fun psQuote(value: String) = "'" + value.replace("'", "''") + "'"

    fun genToken(
        userId: String,
        projectId: String,
        os: String,
        zoneId: String
    ): ThirdPartyDesktopTokenResp {
        if (userId.isBlank() || projectId.isBlank() || zoneId.isBlank()) {
            throw illegal("userId/projectId/zoneId can not be blank")
        }
        val osEnum = OS.parse(os.uppercase()) ?: throw illegal("os $os is not supported")
        // token 后续可以直接交付机器、增加项目配额，签发前必须确认是项目成员
        if (!permissionService.checkUserVisitPermission(userId = userId, projectCode = projectId)) {
            logger.warn("genToken|$userId is not member of project $projectId")
            throw forbidden("user $userId is not member of project $projectId")
        }
        val expireTime = System.currentTimeMillis() + expireSeconds * 1000
        val info = ThirdPartyDesktopTokenInfo(
            userId = userId,
            os = osEnum.name,
            zoneId = zoneId,
            projectId = projectId,
            expireTime = expireTime
        )
        val (token, tokenId) = issueToken(info)
        logger.info("genToken|$userId|$projectId|${osEnum.name}|$zoneId|$expireTime|${tokenId.take(LOG_ID_LENGTH)}")
        return ThirdPartyDesktopTokenResp(token = token, expireTime = expireTime)
    }

    /**
     * 重装场景：根据已注册的 cdsId 签发 token
     * 已分配：按工作空间记录校验，用户需是拥有者或项目管理员
     * 未分配：还没有工作空间记录，按注册时写入的 cdsId 绑定校验，用户需是注册人或项目管理员
     * 签发时直接写入注册记录 envId = cdsId，后续 cdskey / assign 接口复用原有归属校验
     */
    fun genReinstallToken(
        userId: String,
        cdsId: String
    ): ThirdPartyDesktopTokenResp {
        if (userId.isBlank() || cdsId.isBlank()) {
            throw illegal("userId/cdsId can not be blank")
        }
        // T_WORKSPACE_WINDOWS.HOST_IP 存的就是 cgsId(即 cdsId)
        val ws = workspaceJoinDao.fetchWindowsWorkspacesSimple(
            dslContext = dslContext,
            ip = cdsId,
            notStatus = listOf(WorkspaceStatus.DELETED, WorkspaceStatus.DELIVERING_FAILED)
        ).maxByOrNull { it.createTime }
        val expireTime = System.currentTimeMillis() + expireSeconds * 1000
        val (info, ip) = if (ws != null) {
            if (!permissionService.hasManagerOrOwnerPermission(
                    userId = userId,
                    projectId = ws.projectId,
                    workspaceName = ws.workspaceName,
                    ownerType = ws.ownerType
                )
            ) {
                logger.warn("genReinstallToken|no permission|$userId|$cdsId|${ws.workspaceName}|${ws.projectId}")
                throw forbidden("user $userId is not owner of $cdsId or manager of project ${ws.projectId}")
            }
            ThirdPartyDesktopTokenInfo(
                userId = userId,
                os = ws.os?.name ?: "",
                zoneId = ws.zoneId?.ifBlank { null } ?: cdsId.substringBefore("."),
                projectId = ws.projectId,
                expireTime = expireTime,
                reinstall = true
            ) to (ws.ip ?: "")
        } else {
            val binding = getBinding(cdsId)
            if (binding == null) {
                logger.warn("genReinstallToken|workspace and binding not found|$userId|$cdsId")
                throw notFound("no machine found by cdsId $cdsId")
            }
            if (binding.userId != userId && !permissionService.hasUserManager(userId, binding.projectId)) {
                logger.warn("genReinstallToken|no permission|$userId|$cdsId|${binding.userId}|${binding.projectId}")
                throw forbidden("user $userId is not register of $cdsId or manager of project ${binding.projectId}")
            }
            ThirdPartyDesktopTokenInfo(
                userId = userId,
                os = binding.os,
                zoneId = binding.zoneId,
                projectId = binding.projectId,
                expireTime = expireTime,
                reinstall = true,
                // 未分配的机器后续分配时，拥有者仍是注册人
                assignOwner = binding.userId
            ) to binding.ip
        }
        val (token, tokenId) = issueToken(info)
        redisOperation.set(
            key = registerKey(tokenId),
            value = JsonUtil.toJson(
                ThirdPartyDesktopRegisterRecord(ip = ip, taskId = null, envId = cdsId),
                formatted = false
            ),
            expiredInSecond = expireSeconds
        )
        logger.info(
            "genReinstallToken|$userId|${info.projectId}|${ws?.workspaceName}|$cdsId|$expireTime|" +
                tokenId.take(LOG_ID_LENGTH)
        )
        return ThirdPartyDesktopTokenResp(token = token, expireTime = expireTime)
    }

    /**
     * 生成随机 token 并写入 Redis，返回 token 明文和 tokenId(sha256)
     */
    private fun issueToken(info: ThirdPartyDesktopTokenInfo): Pair<String, String> {
        val token = randomToken()
        val tokenId = ShaUtils.sha256(token)
        // 碰撞概率可忽略，用 setIfAbsent 兜底，保证不会覆盖已有 token
        if (!redisOperation.setIfAbsent(tokenKey(tokenId), JsonUtil.toJson(info, formatted = false), expireSeconds)) {
            logger.error("issueToken|token conflict|${info.userId}|${info.projectId}")
            throw ErrorCodeException(
                statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode,
                errorCode = PARAMETER_ILLEGAL_ERROR,
                defaultMessage = "genToken failed, please retry",
                params = arrayOf("token", "genToken failed, please retry")
            )
        }
        return token to tokenId
    }

    /**
     * 校验 token：格式合法且 Redis 中存在未过期的记录，否则抛 401
     */
    private fun verifyToken(token: String?): VerifiedToken {
        if (token.isNullOrBlank()) {
            throw unauthorized("token is empty")
        }
        if (!TOKEN_REGEX.matches(token)) {
            throw unauthorized("token illegal")
        }
        val tokenId = ShaUtils.sha256(token)
        val value = redisOperation.get(tokenKey(tokenId))
        if (value.isNullOrBlank()) {
            logger.warn("verifyToken|token not found or expired|${tokenId.take(LOG_ID_LENGTH)}")
            throw unauthorized("token illegal or expired")
        }
        val info = try {
            JsonUtil.to(value, ThirdPartyDesktopTokenInfo::class.java)
        } catch (e: Throwable) {
            logger.warn("verifyToken|parse failed|${tokenId.take(LOG_ID_LENGTH)}|${e.message}")
            throw unauthorized("token illegal")
        }
        if (info.expireTime < System.currentTimeMillis()) {
            logger.warn("verifyToken|token expired|${info.userId}|${info.expireTime}")
            throw unauthorized("token expired")
        }
        return VerifiedToken(id = tokenId, info = info)
    }

    /**
     * 一个 token 只能注册一台机器：先用 setIfAbsent 抢占，成功后记录 taskId/envId
     * 已注册（有 taskId/envId）时不查任务状态，直接返回已有结果，不再调用 BCS；
     * 仅作用于第三方机器注册，不影响其他机器类型
     * BCS 调用报错或返回空会释放占位，允许同一个 token 重试；BCS 任务执行失败需重新签发 token 再注册
     */
    fun registerVm(token: String?, ip: String): CreateCvmResp? {
        val verified = verifyToken(token)
        val info = verified.info
        if (info.reinstall) {
            logger.warn("registerVm|reinstall token can not register|${info.userId}|$ip")
            throw forbidden("reinstall token can not register a new machine")
        }
        if (ip.isBlank()) {
            throw illegal("ip can not be blank")
        }
        val recordKey = registerKey(verified.id)
        val ttl = remainSeconds(info)
        if (!redisOperation.setIfAbsent(recordKey, REGISTERING, ttl)) {
            return existedRegister(verified, ip)
        }
        logger.info("registerVm|${info.userId}|${info.projectId}|${info.os}|${info.zoneId}|$ip")
        val resp = try {
            remoteDevServiceFactory.loadRemoteDevService(WorkspaceMountType.BCS).registerVm(
                RegisterVmData(
                    internal = true,
                    ip = ip,
                    os = info.os,
                    machineType = DEFAULT_MACHINE_TYPE,
                    zoneId = info.zoneId,
                    skipInit = true
                )
            )
        } catch (e: Throwable) {
            redisOperation.delete(recordKey)
            throw e
        }
        if (resp == null) {
            redisOperation.delete(recordKey)
            logger.warn("registerVm|bcs return null|${info.userId}|$ip")
            return null
        }
        if (resp.environmentUid.isNullOrBlank()) {
            logger.warn("registerVm|bcs return empty environmentUid|${info.userId}|$ip|${resp.taskID}")
        } else {
            // 长期记录 cdsId 归属，未分配就重装时用于签发重装 token
            redisOperation.set(
                key = bindingKey(resp.environmentUid!!),
                value = JsonUtil.toJson(
                    ThirdPartyDesktopBinding(
                        userId = info.userId,
                        projectId = info.projectId,
                        os = info.os,
                        zoneId = info.zoneId,
                        ip = ip
                    ),
                    formatted = false
                ),
                expiredInSecond = bindingExpireDays * SECONDS_PER_DAY
            )
        }
        redisOperation.set(
            key = recordKey,
            value = JsonUtil.toJson(
                ThirdPartyDesktopRegisterRecord(
                    ip = ip,
                    taskId = resp.taskID,
                    envId = resp.environmentUid,
                    taskUid = resp.taskUid
                ),
                formatted = false
            ),
            expiredInSecond = ttl
        )
        return resp
    }

    /**
     * 已有注册记录：不查任务状态，有 taskId 或 envId 就直接返回已有结果
     * 注册中（getRecord 抛 403 稍后重试）或记录里两者都为空时拒绝
     */
    private fun existedRegister(verified: VerifiedToken, ip: String): CreateCvmResp {
        val info = verified.info
        val record = getRecord(verified)
        val envId = record.envId?.ifBlank { null }
        val taskId = record.taskId?.ifBlank { null }
        if (envId == null && taskId == null) {
            throw forbidden("token has already been used to register a machine")
        }
        logger.info(
            "registerVm|already registered, return existed|${info.userId}|$envId|$taskId|" +
                "recordIp=${record.ip}|reqIp=$ip"
        )
        return CreateCvmResp(environmentUid = envId, taskID = taskId ?: "", taskUid = record.taskUid ?: "")
    }

    fun getTaskStatus(token: String?, taskId: String): WorkspaceTaskStatus? {
        val verified = verifyToken(token)
        if (taskId.isBlank()) {
            throw illegal("taskId can not be blank")
        }
        val record = getRecord(verified)
        if (record.taskId != taskId) {
            logger.warn("getTaskStatus|taskId not belong to token|${verified.info.userId}|$taskId|${record.taskId}")
            throw forbidden("taskId $taskId does not belong to this token")
        }
        logger.info("getTaskStatus|${verified.info.userId}|$taskId")
        return remoteDevServiceFactory.loadRemoteDevService(WorkspaceMountType.BCS).taskStatus(taskId)
    }

    /**
     * envId 必须是本 token 注册出来的；CDS apply 会把 vmId 与 envId(cdsId) 绑定，
     * 已绑定到其他 cdsId 的 vmId 会被 CDS 拒绝，同一组 vmId + envId 重复调用返回同一个 key
     */
    fun getCdsKey(token: String?, vmId: String, envId: String): ThirdPartyDesktopCdsKeyResp {
        val verified = verifyToken(token)
        if (vmId.isBlank() || envId.isBlank()) {
            throw illegal("vmId/envId can not be blank")
        }
        checkEnvBelong(verified, envId, "getCdsKey")
        logger.info("getCdsKey|${verified.info.userId}|$vmId|$envId")
        return ThirdPartyDesktopCdsKeyResp(cdsKey = startCloudClient.applyCdsKey(envId = envId, vmId = vmId))
    }

    /**
     * envId 必须是本 token 注册（或重装 token 绑定）的，且 cds 未分配
     * 一个 token 只能分配一次（避免重复增加项目配额）
     */
    fun assignWorkspace(token: String?, envId: String): Boolean {
        val verified = verifyToken(token)
        val info = verified.info
        if (envId.isBlank()) {
            throw illegal("envId can not be blank")
        }
        checkEnvBelong(verified, envId, "assignWorkspace")
        // 注册接口返回的是 BCS environmentUid，而 createControl.assignWorkspace 按 listcgs 的 cgsId(zoneId.ip) 匹配
        // 这里先在 listcgs 中按 envId 找到对应的 cgsId（重装 token 绑定的就是 cgsId，同样兼容）
        val cgsId = resolveCgsId(envId)
        // createControl.assignWorkspace 会先给项目加配额再跳过已分配的 cgsId，必须在这里提前拦截
        if (workspaceDao.getAvailableCgsWorkspace(dslContext = dslContext, cgsId = cgsId) > 0) {
            logger.warn("assignWorkspace|cds already assigned|${info.userId}|$envId|$cgsId|reinstall=${info.reinstall}")
            throw forbidden("env $envId has already been assigned")
        }
        val assignKey = assignKey(verified.id)
        if (!redisOperation.setIfAbsent(assignKey, envId, remainSeconds(info))) {
            logger.warn("assignWorkspace|already assigned|${info.userId}|$envId")
            throw forbidden("env $envId has already been assigned by this token")
        }
        logger.info("assignWorkspace|${info.userId}|${info.projectId}|${info.os}|$envId|$cgsId")
        try {
            createControl.assignWorkspace(
                userId = info.userId,
                data = OpProjectWorkspaceAssignData(
                    projectId = info.projectId,
                    owner = null,
                    cgsIds = listOf(cgsId),
                    ips = null,
                    repoId = null,
                    localDriver = null,
                    type = WorkspaceOwnerType.PROJECT,
                    os = OS.parse(info.os)
                ),
                assignOwner = info.assignOwner ?: info.userId
            )
        } catch (e: Throwable) {
            // 分配失败释放占位，允许重试
            redisOperation.delete(assignKey)
            throw e
        }
        return true
    }

    /**
     * 分配时 cdsId 有两种形态混用：
     * cgsId：zoneId.ip（如 CQ12.28.164.247.187），重装 token 取自 T_WORKSPACE_WINDOWS.HOST_IP
     * envId：BCS environmentUid（如 56385x8zru7lrvnv-1792409866669546025），注册接口返回
     * 按格式判断后走不同匹配，统一解析成 listcgs 中的 cgsId；找不到则不分配，避免白加项目配额
     */
    private fun resolveCgsId(cdsId: String): String {
        val byCgsId = CGS_ID_REGEX.matches(cdsId)
        val startCloud = SpringContextUtil.getBean(ServiceStartCloudInterface::class.java)
        val cgs = if (byCgsId) {
            startCloud.getCgsData(FetchWinPoolData(cgsIds = listOf(cdsId), ips = null)).data?.firstOrNull()
        } else {
            startCloud.realtimeStartCloudResourceList().data?.firstOrNull { it.envId == cdsId }
        }
        if (cgs == null) {
            logger.warn("assignWorkspace|not found in listcgs|$cdsId|byCgsId=$byCgsId")
            throw notFound("env $cdsId not found in resource pool, register may not be finished")
        }
        logger.info("assignWorkspace|resolveCgsId|$cdsId|byCgsId=$byCgsId|${cgs.cgsId}|${cgs.envId}|${cgs.status}")
        return cgs.cgsId
    }

    private fun checkEnvBelong(verified: VerifiedToken, envId: String, action: String) {
        val record = getRecord(verified)
        if (record.envId.isNullOrBlank() || record.envId != envId) {
            logger.warn("$action|envId not belong to token|${verified.info.userId}|$envId|${record.envId}")
            throw forbidden("env $envId does not belong to this token")
        }
    }

    private fun getRecord(verified: VerifiedToken): ThirdPartyDesktopRegisterRecord {
        val value = redisOperation.get(registerKey(verified.id))
        if (value.isNullOrBlank()) {
            throw forbidden("no machine registered by this token")
        }
        if (value == REGISTERING) {
            throw forbidden("machine is registering, please retry later")
        }
        return try {
            JsonUtil.to(value, ThirdPartyDesktopRegisterRecord::class.java)
        } catch (e: Throwable) {
            logger.warn("getRecord|parse failed|${verified.info.userId}|${e.message}")
            throw forbidden("no machine registered by this token")
        }
    }

    private fun randomToken(): String {
        val bytes = ByteArray(TOKEN_BYTES)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    /* 注册/分配记录与 token 同生命周期，至少保留 1 分钟 */
    private fun remainSeconds(info: ThirdPartyDesktopTokenInfo): Long {
        return maxOf((info.expireTime - System.currentTimeMillis()) / 1000 + 1, MIN_RECORD_SECONDS)
    }

    private fun getBinding(cdsId: String): ThirdPartyDesktopBinding? {
        val value = redisOperation.get(bindingKey(cdsId))?.ifBlank { null } ?: return null
        return try {
            JsonUtil.to(value, ThirdPartyDesktopBinding::class.java)
        } catch (e: Throwable) {
            logger.warn("getBinding|parse failed|$cdsId|${e.message}")
            null
        }
    }

    private fun tokenKey(tokenId: String) = "$REDIS_KEY_PREFIX:token:$tokenId"

    private fun registerKey(tokenId: String) = "$REDIS_KEY_PREFIX:register:$tokenId"

    private fun assignKey(tokenId: String) = "$REDIS_KEY_PREFIX:assign:$tokenId"

    private fun bindingKey(cdsId: String) = "$REDIS_KEY_PREFIX:binding:$cdsId"

    private fun unauthorized(msg: String) = ErrorCodeException(
        statusCode = Response.Status.UNAUTHORIZED.statusCode,
        errorCode = PARAMETER_ILLEGAL_ERROR,
        defaultMessage = msg,
        params = arrayOf("token", msg)
    )

    private fun forbidden(msg: String) = ErrorCodeException(
        statusCode = Response.Status.FORBIDDEN.statusCode,
        errorCode = PARAMETER_ILLEGAL_ERROR,
        defaultMessage = msg,
        params = arrayOf("permission", msg)
    )

    private fun notFound(msg: String) = ErrorCodeException(
        statusCode = Response.Status.NOT_FOUND.statusCode,
        errorCode = PARAMETER_ILLEGAL_ERROR,
        defaultMessage = msg,
        params = arrayOf("cdsId", msg)
    )

    private fun illegal(msg: String) = ErrorCodeException(
        statusCode = Response.Status.BAD_REQUEST.statusCode,
        errorCode = PARAMETER_ILLEGAL_ERROR,
        defaultMessage = msg,
        params = arrayOf("param", msg)
    )

    companion object {
        private val logger = LoggerFactory.getLogger(ThirdPartyDesktopService::class.java)
        private const val DEFAULT_EXPIRE_SECONDS = 3600L
        /* Redis 覆盖配置的 key，值为完整地址，删除 key 即回落到配置文件 */
        const val SCRIPT_URL_KEY = "remotedev:thirdPartyDesktop:scriptUrl"
        const val API_HOST_KEY = "remotedev:thirdPartyDesktop:apiHost"
        private val ZONE_REGEX = Regex("^[A-Za-z]*\\d+$")
        /* cgsId = zoneId.ipv4，如 CQ12.28.164.247.187 */
        private val CGS_ID_REGEX = Regex("^[A-Za-z0-9_-]+\\.(\\d{1,3}\\.){3}\\d{1,3}$")
        private val GAME_ID_REGEX = Regex("^\\d{1,20}$")
        private val SAFE_URL_REGEX = Regex("^https?://[A-Za-z0-9._~:/?#\\[\\]@!&()*+,;=%-]+$")
        private const val DEFAULT_BINDING_EXPIRE_DAYS = 30L
        private const val SECONDS_PER_DAY = 24 * 3600L
        private const val MIN_RECORD_SECONDS = 60L
        private const val DEFAULT_MACHINE_TYPE = "XL2"
        private const val REDIS_KEY_PREFIX = "remotedev:third_party_desktop"
        private const val REGISTERING = "REGISTERING"

        /* 24 字节随机数，base64url 无填充后固定 32 位 */
        private const val TOKEN_BYTES = 24
        private val TOKEN_REGEX = Regex("^[A-Za-z0-9_-]{32}$")
        private const val LOG_ID_LENGTH = 8
    }
}
