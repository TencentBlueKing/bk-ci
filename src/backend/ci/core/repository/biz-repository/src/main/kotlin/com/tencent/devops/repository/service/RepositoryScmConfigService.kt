/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.repository.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.IdValue
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.auth.api.AuthPlatformApi
import com.tencent.devops.common.security.util.BkCryptoUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.repository.constant.RepositoryMessageCode
import com.tencent.devops.repository.constant.RepositoryMessageCode.CREDENTIAL_TYPE_PREFIX
import com.tencent.devops.repository.dao.RepositoryDao
import com.tencent.devops.repository.dao.RepositoryScmConfigDao
import com.tencent.devops.repository.dao.RepositoryScmProviderDao
import com.tencent.devops.repository.pojo.RepoCredentialTypeVo
import com.tencent.devops.repository.pojo.RepositoryConfigVisibility
import com.tencent.devops.repository.pojo.RepositoryConfigLogoInfo
import com.tencent.devops.repository.pojo.RepositoryScmConfig
import com.tencent.devops.repository.pojo.RepositoryScmConfigReq
import com.tencent.devops.repository.pojo.RepositoryScmConfigVo
import com.tencent.devops.repository.pojo.RepositoryScmProvider
import com.tencent.devops.repository.pojo.RepositoryScmProviderVo
import com.tencent.devops.repository.pojo.ScmConfigBaseInfo
import com.tencent.devops.repository.pojo.ScmConfigProps
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.RepoCredentialType
import com.tencent.devops.repository.pojo.enums.RepoCredentialType.TOKEN_USERNAME_PASSWORD
import com.tencent.devops.repository.pojo.enums.RepoCredentialType.USERNAME_PASSWORD
import com.tencent.devops.repository.pojo.enums.ScmConfigOauthType
import com.tencent.devops.repository.pojo.enums.ScmConfigStatus
import com.tencent.devops.scm.api.enums.EventAction
import com.tencent.devops.scm.api.enums.WebhookSecretType
import com.tencent.devops.scm.spring.properties.HttpClientProperties
import com.tencent.devops.scm.spring.properties.Oauth2ClientProperties
import com.tencent.devops.scm.spring.properties.ScmProviderProperties
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.file.Files
import javax.imageio.ImageIO

@Service
class RepositoryScmConfigService @Autowired constructor(
    private val dslContext: DSLContext,
    private val repositoryScmConfigDao: RepositoryScmConfigDao,
    private val repositoryScmProviderDao: RepositoryScmProviderDao,
    private val repositoryDao: RepositoryDao,
    private val uploadFileService: RepositoryUploadFileService,
    private val authPlatformApi: AuthPlatformApi,
    private val repositoryConfigVisibilityService: RepositoryConfigVisibilityService
) {
    @Value("\${aes.scm.props:#{null}}")
    private val aesKey: String = ""

    @Value("\${scm.oauth.callbackUrl:#{null}}")
    private val oauthCallbackUrl: String = ""

    @Value("\${logo.allowUploadLogoTypes:#{null}}")
    private lateinit var allowUploadLogoTypes: String

    @Value("\${logo.maxUploadLogoSize:#{null}}")
    private lateinit var maxUploadLogoSize: String

    @Value("\${logo.allowUploadLogoWidth:#{null}}")
    private lateinit var allowUploadLogoWidth: String

    @Value("\${logo.allowUploadLogoHeight:#{null}}")
    private lateinit var allowUploadLogoHeight: String

    fun create(userId: String, request: RepositoryScmConfigReq) {
        with(request) {
            validateUserPlatformPermission(userId = userId)
            logger.info("create scm config|userId:$userId|scmCode:scmCode|providerCode:$providerCode")
            val scmProvider = repositoryScmProviderDao.get(dslContext = dslContext, providerCode = providerCode)
                ?: throw ErrorCodeException(
                    errorCode = RepositoryMessageCode.ERROR_SCM_PROVIDER_NOT_FOUND,
                    params = arrayOf(providerCode)
                )
            if (!scmProvider.credentialTypeList.containsAll(request.credentialTypeList)) {
                val notSupportAuthType =
                    request.credentialTypeList.subtract(scmProvider.credentialTypeList.toSet())
                        .joinToString(",")
                throw ErrorCodeException(
                    errorCode = RepositoryMessageCode.ERROR_SCM_PROVIDER_NOT_SUPPORT_AUTH_TYPE,
                    params = arrayOf(notSupportAuthType)
                )
            }
            val providerProperties = createProviderProperties(scmProvider = scmProvider, request = request)
            val scmConfig = RepositoryScmConfig(
                scmCode = scmCode,
                name = name,
                providerCode = providerCode,
                scmType = scmType ?: scmProvider.scmType,
                hosts = hosts,
                logoUrl = logoUrl ?: scmProvider.logoUrl,
                credentialTypeList = request.credentialTypeList,
                oauthType = oauthType,
                oauthScmCode = oauthScmCode,
                status = ScmConfigStatus.SUCCESS,
                oauth2Enabled = request.credentialTypeList.contains(RepoCredentialType.OAUTH),
                mergeEnabled = mergeEnabled,
                pacEnabled = pacEnabled,
                webhookEnabled = webhookEnabled,
                providerProps = providerProperties,
                creator = userId,
                updater = userId
            )
            repositoryScmConfigDao.create(
                dslContext = dslContext,
                scmConfig = scmConfig
            )
        }
    }

    fun edit(userId: String, scmCode: String, request: RepositoryScmConfigReq) {
        with(request) {
            validateUserPlatformPermission(userId = userId)
            logger.info("update scm config|userId:$userId|scmCode:scmCode|providerCode:$providerCode")
            val scmProvider = repositoryScmProviderDao.get(dslContext = dslContext, providerCode = providerCode)
                ?: throw ErrorCodeException(
                    errorCode = RepositoryMessageCode.ERROR_SCM_PROVIDER_NOT_FOUND,
                    params = arrayOf(providerCode)
                )
            val scmConfig =
                repositoryScmConfigDao.get(dslContext = dslContext, scmCode = scmCode) ?: throw ErrorCodeException(
                    errorCode = RepositoryMessageCode.ERROR_SCM_CONFIG_NOT_FOUND,
                    params = arrayOf(scmCode)
                )
            val providerProperties = createProviderProperties(
                scmProvider = scmProvider,
                request = request,
                oldProviderProperties = scmConfig.providerProps
            )
            val newScmConfig = scmConfig.copy(
                scmCode = scmCode,
                name = name,
                providerCode = providerCode,
                scmType = scmConfig.scmType,
                hosts = hosts,
                logoUrl = logoUrl ?: scmProvider.logoUrl,
                credentialTypeList = request.credentialTypeList,
                oauthType = oauthType,
                oauthScmCode = oauthScmCode,
                mergeEnabled = mergeEnabled,
                pacEnabled = pacEnabled,
                webhookEnabled = webhookEnabled,
                providerProps = providerProperties,
                updater = userId
            )
            repositoryScmConfigDao.update(
                dslContext = dslContext,
                scmCode = scmCode,
                scmConfig = newScmConfig
            )
        }
    }

    fun listConfigBaseInfo(userId: String, scmType: ScmType?): List<ScmConfigBaseInfo> {
        val sqlLimit = PageUtil.convertPageSizeToSQLMAXLimit(PageUtil.DEFAULT_PAGE, PageUtil.MAX_PAGE_SIZE)
        val providerMap = repositoryScmProviderDao.list(dslContext = dslContext).associateBy { it.providerCode }
        val scmConfigs = repositoryScmConfigDao.list(
            dslContext = dslContext,
            excludeStatus = ScmConfigStatus.DISABLED,
            scmType = scmType,
            limit = sqlLimit.limit,
            offset = sqlLimit.offset
        )
        val scmCodes = scmConfigs.map { it.scmCode }
        val finalScmCodes = validateUserPermission(userId, scmCodes)
        return scmConfigs.filter {
            finalScmCodes.contains(it.scmCode)
        }.map {
            ScmConfigBaseInfo(
                scmCode = it.scmCode,
                name = it.name,
                status = it.status,
                scmType = it.scmType,
                hosts = it.hosts,
                logoUrl = it.logoUrl,
                docUrl = providerMap[it.providerCode]?.docUrl,
                credentialTypeList = getCredentialTypeVos(it.credentialTypeList, it.scmType),
                pacEnabled = it.pacEnabled
            )
        }
    }

    fun listConfigVo(
        userId: String,
        status: ScmConfigStatus?,
        excludeStatus: ScmConfigStatus? = null,
        oauth2Enabled: Boolean? = null,
        mergeEnabled: Boolean? = null,
        pacEnabled: Boolean? = null,
        offset: Int,
        limit: Int
    ): SQLPage<RepositoryScmConfigVo> {
        validateUserPlatformPermission(userId = userId)
        val providerMap = repositoryScmProviderDao.list(dslContext = dslContext).associateBy { it.providerCode }
        val count = repositoryScmConfigDao.count(
            dslContext = dslContext,
            status = status,
            excludeStatus = excludeStatus,
            oauth2Enabled = oauth2Enabled,
            mergeEnabled = mergeEnabled,
            pacEnabled = pacEnabled
        )
        val scmConfigs = repositoryScmConfigDao.list(
            dslContext = dslContext,
            status = status,
            excludeStatus = excludeStatus,
            oauth2Enabled = oauth2Enabled,
            mergeEnabled = mergeEnabled,
            pacEnabled = pacEnabled,
            limit = limit,
            offset = offset
        )
        val relatedCntMap =
            repositoryDao.countByScmCodes(dslContext = dslContext, scmCodes = scmConfigs.map { it.scmCode })

        val records = scmConfigs.map {
            convertScmConfigVo(scmConfig = it, providerMap = providerMap, relatedCntMap = relatedCntMap)
        }
        return SQLPage(count = count, records = records)
    }

    fun listConfig(
        userId: String,
        status: ScmConfigStatus? = null,
        excludeStatus: ScmConfigStatus? = null,
        oauth2Enabled: Boolean? = null,
        mergeEnabled: Boolean? = null,
        pacEnabled: Boolean? = null,
        offset: Int,
        limit: Int
    ): List<RepositoryScmConfig> {
        validateUserPlatformPermission(userId = userId)
        return repositoryScmConfigDao.list(
            dslContext = dslContext,
            status = status,
            excludeStatus = excludeStatus,
            oauth2Enabled = oauth2Enabled,
            mergeEnabled = mergeEnabled,
            pacEnabled = pacEnabled,
            limit = limit,
            offset = offset
        )
    }

    fun listProvider(userId: String): List<RepositoryScmProviderVo> {
        validateUserPlatformPermission(userId = userId)
        return repositoryScmProviderDao.list(dslContext = dslContext).map {
            RepositoryScmProviderVo(
                providerCode = it.providerCode,
                providerType = it.providerType,
                name = it.name,
                desc = it.desc,
                scmType = it.scmType,
                logoUrl = it.logoUrl,
                docUrl = it.docUrl,
                credentialTypeList = getCredentialTypeVos(it.credentialTypeList, it.scmType),
                api = it.api,
                merge = it.merge,
                webhook = it.webhook,
                pac = it.pac,
                webhookSecretType = it.webhookSecretType
            )
        }
    }

    fun getOrNull(scmCode: String): RepositoryScmConfig? {
        val scmConfig = repositoryScmConfigDao.get(
            dslContext = dslContext,
            scmCode = scmCode
        )
        scmConfig?.providerProps?.decrypt()
        return scmConfig
    }

    fun get(scmCode: String): RepositoryScmConfig {
        val scmConfig = repositoryScmConfigDao.get(
            dslContext = dslContext,
            scmCode = scmCode
        ) ?: throw ErrorCodeException(
            errorCode = RepositoryMessageCode.ERROR_SCM_CONFIG_NOT_FOUND,
            params = arrayOf(scmCode)
        )
        scmConfig.providerProps.decrypt()
        return scmConfig
    }

    fun getProps(scmCode: String): ScmProviderProperties {
        return get(scmCode = scmCode).providerProps
    }

    fun enable(userId: String, scmCode: String) {
        validateUserPlatformPermission(userId = userId)
        logger.info("enable scm config|userId:$userId|scmCode:$scmCode")
        repositoryScmConfigDao.updateStatus(
            dslContext = dslContext,
            scmCode = scmCode,
            status = ScmConfigStatus.SUCCESS
        )
    }

    fun disable(userId: String, scmCode: String) {
        validateUserPlatformPermission(userId = userId)
        logger.info("disable scm config|userId:$userId|scmCode:$scmCode")
        repositoryScmConfigDao.updateStatus(
            dslContext = dslContext,
            scmCode = scmCode,
            status = ScmConfigStatus.DISABLED
        )
    }

    fun delete(userId: String, scmCode: String) {
        validateUserPlatformPermission(userId = userId)
        logger.info("delete scm config|userId:$userId|scmCode:$scmCode")
        val relatedCnt = repositoryDao.countByScmCode(
            dslContext = dslContext,
            scmCode = scmCode
        )
        if (relatedCnt > 0) {
            throw ErrorCodeException(
                errorCode = RepositoryMessageCode.ERROR_SCM_CONFIG_IN_USED_CAN_NOT_DELETE,
                params = arrayOf(scmCode)
            )
        }
        repositoryScmConfigDao.delete(dslContext = dslContext, scmCode = scmCode)
    }

    @Suppress("NestedBlockDepth")
    fun uploadLogo(
        userId: String,
        contentLength: Long,
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    ): RepositoryConfigLogoInfo {
        validateUserPlatformPermission(userId = userId)
        val fileName = disposition.fileName
        logger.info("upload repository config logo file fileName is:$fileName,contentLength is:$contentLength")
        val index = fileName.lastIndexOf(".")
        val fileType = fileName.substring(index + 1).lowercase()
        // 校验文件类型是否满足上传文件类型的要求
        val allowUploadFileTypeList = allowUploadLogoTypes.split(",")
        if (!allowUploadFileTypeList.contains(fileType)) {
            throw OperationException(
                message = I18nUtil.getCodeLanMessage(
                    messageCode = CommonMessageCode.ERROR_LOGO_FORMAT_UNSUPPORTED,
                    params = arrayOf(fileType, allowUploadLogoTypes),
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }
        // 校验上传文件大小是否超出限制
        val maxFileSize = maxUploadLogoSize.toLong()
        if (contentLength > maxFileSize) {
            throw OperationException(
                message = I18nUtil.getCodeLanMessage(
                    messageCode = CommonMessageCode.ERROR_LOGO_FILE_SIZE_EXCEEDED,
                    params = arrayOf((maxFileSize / BYTES_PER_MB).toString() + "M"),
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }
        val file = Files.createTempFile(UUIDUtil.generate(), ".$fileType").toFile()
        val output = file.outputStream()
        // svg类型图片不做尺寸检查
        if ("svg" != fileType) {
            val img = ImageIO.read(inputStream)
            // 判断上传的logo是否为512x512规格
            val width = img.width
            val height = img.height
            if (width != height || width < allowUploadLogoWidth.toInt()) {
                throw OperationException(
                    message = I18nUtil.getCodeLanMessage(
                        CommonMessageCode.ERROR_LOGO_DIMENSION_REQUIREMENT,
                        params = arrayOf(allowUploadLogoWidth, allowUploadLogoHeight),
                        language = I18nUtil.getLanguage(userId)
                    )
                )
            }
            ImageIO.write(img, fileType, output)
        } else {
            val buffer = ByteArray(1024)
            var len: Int
            try {
                while (true) {
                    len = inputStream.read(buffer)
                    if (len > -1) {
                        output.write(buffer, 0, len)
                    } else {
                        break
                    }
                }
                output.flush()
            } catch (ignored: Throwable) {
                logger.error("BKSystemErrorMonitor|upload repository config logo|error=${ignored.message}", ignored)
                throw OperationException(
                    message = I18nUtil.getCodeLanMessage(
                        messageCode = CommonMessageCode.SYSTEM_ERROR,
                        language = I18nUtil.getLanguage(userId)
                    )
                )
            } finally {
                output.close()
            }
        }
        val filePath = "file/$fileType/${UUIDUtil.generate()}.$fileType"
        val logoUrl = uploadFileService.uploadFile(userId, file, filePath)
        logger.info("uploadStoreLogo logoUrl is:$logoUrl")
        return RepositoryConfigLogoInfo(logoUrl)
    }

    fun supportEvents(
        userId: String,
        scmCode: String
    ): List<IdValue> {
        return getProviderConfig(scmCode)?.let { provider ->
            provider.webhookProps?.let { props ->
                props.eventTypeList?.map { event ->
                    IdValue(event, eventDesc(provider.providerCode, event))
                }
            }
        } ?: listOf()
    }

    fun supportEventActions(
        userId: String,
        scmCode: String,
        eventType: String
    ): List<IdValue> {
        return getProviderConfig(scmCode)?.let { provider ->
            provider.webhookProps?.let { props ->
                props.eventTypeActionMap?.get(eventType)?.map { action ->
                    IdValue(
                        EventAction.valueOf(action).value,
                        eventActionDesc(provider.providerCode, eventType, action)
                    )
                }
            }
        } ?: listOf()
    }

    fun listDept(
        scmCode: String,
        userId: String,
        limit: Int,
        offset: Int
    ): SQLPage<RepositoryConfigVisibility> {
        validateUserPlatformPermission(userId)
        val record = repositoryConfigVisibilityService.listDept(
            scmCode = scmCode,
            limit = limit,
            offset = offset
        )
        val count = repositoryConfigVisibilityService.countDept(
            scmCode = scmCode
        ).toLong()
        return SQLPage(count = count, records = record)
    }

    fun addDept(
        scmCode: String,
        userId: String,
        checkPermission: Boolean = true,
        deptList: List<RepositoryConfigVisibility>
    ) {
        if (checkPermission) {
            validateUserPlatformPermission(userId)
        }
        if (deptList.isNotEmpty()) {
            repositoryConfigVisibilityService.createDept(
                scmCode = scmCode,
                userId = userId,
                deptList = deptList
            )
        }
    }

    fun deleteDept(
        scmCode: String,
        userId: String,
        checkPermission: Boolean = true,
        deptList: List<Int>
    ) {
        if (checkPermission) {
            validateUserPlatformPermission(userId)
        }
        if (deptList.isNotEmpty()) {
            repositoryConfigVisibilityService.deleteDept(
                scmCode = scmCode,
                deptList = deptList.toSet()
            )
        }
    }

    private fun getProviderConfig(scmCode: String): RepositoryScmProvider? {
        val scmConfig = repositoryScmConfigDao.get(dslContext, scmCode) ?: throw ErrorCodeException(
            errorCode = RepositoryMessageCode.ERROR_SCM_CONFIG_NOT_FOUND,
            params = arrayOf(scmCode)
        )
        return repositoryScmProviderDao.get(dslContext, scmConfig.providerCode)
    }

    private fun convertScmConfigVo(
        scmConfig: RepositoryScmConfig,
        providerMap: Map<String, RepositoryScmProvider>,
        relatedCntMap: Map<String, Long>
    ): RepositoryScmConfigVo {

        return with(scmConfig) {
            val props = convertProvideProps(providerProps)
            RepositoryScmConfigVo(
                scmCode = scmCode,
                name = name,
                providerCode = providerCode,
                scmType = scmType,
                hosts = hosts,
                logoUrl = logoUrl,
                docUrl = providerMap[providerCode]?.docUrl,
                credentialTypeList = getCredentialTypeVos(credentialTypeList, scmType),
                oauthType = oauthType,
                oauthScmCode = oauthScmCode,
                oauth2Enabled = oauth2Enabled,
                status = status,
                mergeEnabled = mergeEnabled,
                pacEnabled = pacEnabled,
                webhookEnabled = webhookEnabled,
                props = props,
                canDelete = relatedCntMap[scmCode]?.let { it == 0L } ?: true,
                creator = creator,
                updater = updater,
                createTime = createTime,
                updateTime = updateTime
            )
        }
    }

    private fun createProviderProperties(
        scmProvider: RepositoryScmProvider,
        request: RepositoryScmConfigReq,
        oldProviderProperties: ScmProviderProperties? = null
    ): ScmProviderProperties {
        with(request.props) {
            val providerPropertiesBuilder = ScmProviderProperties()
            if (scmProvider.api) {
                require(!apiUrl.isNullOrEmpty()) { "apiUrl can not empty" }
                providerPropertiesBuilder.proxyEnabled = proxyEnabled
                providerPropertiesBuilder.httpClientProperties = HttpClientProperties(
                    apiUrl = apiUrl
                )
            }
            providerPropertiesBuilder.providerCode = scmProvider.providerCode
            providerPropertiesBuilder.providerType = scmProvider.providerType.name
            providerPropertiesBuilder.oauth2Enabled = false
            if (request.credentialTypeList.contains(RepoCredentialType.OAUTH)) {
                when (request.oauthType) {
                    ScmConfigOauthType.NEW -> {
                        val oauth2ClientProperties = createOauth2ClientProperties(
                            oldProviderProperties = oldProviderProperties,
                            request = request
                        )
                        providerPropertiesBuilder.oauth2Enabled = true
                        providerPropertiesBuilder.oauth2ClientProperties = oauth2ClientProperties
                    }

                    ScmConfigOauthType.REUSE -> {
                        require(!request.oauthScmCode.isNullOrEmpty()) { "oauthScmCode can not empty" }
                    }

                    else -> {}
                }
            }
            if (request.webhookEnabled) {
                if (scmProvider.webhookSecretType == WebhookSecretType.APP.name && webhookSecret.isNullOrEmpty()
                ) {
                    throw IllegalArgumentException("webhookSecret can not empty")
                }
            }
            return providerPropertiesBuilder
        }
    }

    private fun ScmConfigProps.createOauth2ClientProperties(
        oldProviderProperties: ScmProviderProperties?,
        request: RepositoryScmConfigReq
    ): Oauth2ClientProperties? {
        require(!webUrl.isNullOrEmpty()) { "webUrl can not empty" }
        require(!clientId.isNullOrEmpty()) { "clientId can not empty" }
        require(!clientSecret.isNullOrEmpty()) { "clientSecret can not empty" }
        require(oauthCallbackUrl.isNotEmpty()) { "callbackUrl can not empty" }
        // 如果是更新,前端传过来的clientSecret可能是加码后的值
        val encryptClientSecret = if (clientSecret == SECRET_MIXER) {
            if (oldProviderProperties?.oauth2ClientProperties?.clientSecret.isNullOrEmpty()) {
                throw IllegalArgumentException("clientSecret can not empty")
            }
            oldProviderProperties?.oauth2ClientProperties?.clientSecret
        } else {
            BkCryptoUtil.encryptSm4ButAes(aesKey, clientSecret!!)
        }
        val redirectUri = String.format(oauthCallbackUrl, request.scmCode)
        return Oauth2ClientProperties(
            webUrl = webUrl,
            clientId = clientId,
            clientSecret = encryptClientSecret,
            redirectUri = redirectUri
        )
    }

    private fun convertProvideProps(providerProperties: ScmProviderProperties): ScmConfigProps {
        val apiUrl = providerProperties.httpClientProperties?.apiUrl
        val webUrl = providerProperties.oauth2ClientProperties?.webUrl
        val clientId = providerProperties.oauth2ClientProperties?.clientId
        val proxyEnabled = providerProperties.proxyEnabled ?: false
        return ScmConfigProps(
            apiUrl = apiUrl,
            webUrl = webUrl,
            clientId = clientId,
            proxyEnabled = proxyEnabled,
            clientSecret = SECRET_MIXER,
            webhookSecret = SECRET_MIXER
        )
    }

    private fun ScmProviderProperties.decrypt(): ScmProviderProperties {
        oauth2ClientProperties?.takeIf { oauth2Enabled == true }?.decrypt()
        return this
    }

    private fun Oauth2ClientProperties.decrypt() {
        clientSecret?.let {
            clientSecret = BkCryptoUtil.decryptSm4OrAes(aesKey, it)
        }
    }

    private fun getCredentialTypeVos(
        credentialTypeList: List<RepoCredentialType>,
        scmType: ScmType
    ): List<RepoCredentialTypeVo> {
        return credentialTypeList.map { credentialType ->
            RepoCredentialTypeVo(
                credentialType = credentialType.name,
                name = I18nUtil.getCodeLanMessage(
                    messageCode = "$CREDENTIAL_TYPE_PREFIX${credentialType.name}",
                    defaultMessage = credentialType.name
                ),
                authType = if (scmType == ScmType.CODE_SVN || scmType == ScmType.SCM_SVN) {
                    // SVN 授权类型，需要特殊处理
                    // 参考 com.tencent.devops.repository.pojo.ScmSvnRepository.SVN_TYPE_HTTP
                    when (credentialType) {
                        USERNAME_PASSWORD, TOKEN_USERNAME_PASSWORD -> RepoAuthType.HTTP
                        else -> RepoAuthType.SSH
                    }.name.toLowerCase()
                } else {
                    credentialType.authType.name
                }
            )
        }
    }

    /**
     * 校验平台管理权限
     */
    private fun validateUserPlatformPermission(userId: String) {
        if (!authPlatformApi.validateUserPlatformPermission(userId)) {
            throw OperationException(
                message = I18nUtil.getCodeLanMessage(
                    CommonMessageCode.ERROR_USER_NO_PLATFORM_ADMIN_PERMISSION
                )
            )
        }
    }

    private fun validateUserPermission(
        userId: String,
        scmCodes: List<String>
    ) = repositoryConfigVisibilityService.listScmCode(
        userId = userId,
        scmCodes = scmCodes
    )

    /**
     * 根据平台编码和事件类型获取事件描述
     */
    private fun eventDesc(providerCode: String, eventType: String) = I18nUtil.getCodeLanMessage(
        messageCode = "BK_${providerCode.uppercase()}_${eventType}_DESC",
        defaultMessage = eventType
    )

    /**
     * 根据平台编码和事件类型获取事件动作描述
     */
    private fun eventActionDesc(providerCode: String, eventType: String, action: String) = I18nUtil.getCodeLanMessage(
        messageCode = "BK_${providerCode.uppercase()}_${eventType}_ACTION_${action}_DESC",
        defaultMessage = action
    )

    companion object {
        const val SECRET_MIXER = "******"
        const val BYTES_PER_MB = 1024L * 1024L
        private val logger = LoggerFactory.getLogger(RepositoryScmConfigService::class.java)
    }
}
