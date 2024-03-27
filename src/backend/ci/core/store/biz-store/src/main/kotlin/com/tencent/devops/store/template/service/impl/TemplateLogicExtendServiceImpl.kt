package com.tencent.devops.store.template.service.impl

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.api.template.ServicePTemplateResource
import com.tencent.devops.process.pojo.template.MarketTemplateRequest
import com.tencent.devops.quality.api.v2.ServiceQualityRuleResource
import com.tencent.devops.quality.api.v2.pojo.request.CopyRuleRequest
import com.tencent.devops.store.common.service.StoreLogicExtendService
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_CODE
import com.tencent.devops.store.pojo.common.publication.StoreBaseDataPO
import com.tencent.devops.store.pojo.common.publication.StoreBaseFeatureDataPO
import com.tencent.devops.store.pojo.template.InstallProjectTemplateDTO
import com.tencent.devops.store.template.dao.TemplateCategoryRelDao
import com.tencent.devops.store.template.service.MarketTemplateService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("TEMPLATE_LOGIC_EXTEND_SERVICE")
class TemplateLogicExtendServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val marketTemplateService: MarketTemplateService,
    private val templateCategoryRelDao: TemplateCategoryRelDao
) : StoreLogicExtendService {

    companion object {
        private val logger = LoggerFactory.getLogger(TemplateLogicExtendServiceImpl::class.java)
    }

    override fun validateInstallExt(
        userId: String,
        storeCode: String,
        projectCodeList: ArrayList<String>
    ): Result<Boolean> {

        // 校验用户是否在模板下插件的可见范围之内和模板的可见范围是否都在其下面的插件可见范围之内
        val validateResult = marketTemplateService.validateUserTemplateComponentVisibleDept(
            userId = userId,
            templateCode = storeCode,
            projectCodeList = projectCodeList
        )
        if (validateResult.isNotOk()) {
            // 抛出错误提示
            return Result(validateResult.status, validateResult.message ?: "")
        }
        return Result(true)
    }

    override fun installComponentExt(
        userId: String,
        projectCodeList: ArrayList<String>,
        storeBaseDataPO: StoreBaseDataPO,
        storeBaseFeatureDataPO: StoreBaseFeatureDataPO?
    ): Result<Boolean> {

        val categoryRecords = templateCategoryRelDao.getCategorysByTemplateId(dslContext, storeBaseDataPO.id)
        val categoryCodeList = mutableListOf<String>()
        categoryRecords?.forEach {
            categoryCodeList.add(it[KEY_CATEGORY_CODE] as String)
        }
        val addMarketTemplateRequest = MarketTemplateRequest(
            projectCodeList = projectCodeList,
            templateCode = storeBaseDataPO.storeCode,
            templateName = storeBaseDataPO.name,
            logoUrl = storeBaseDataPO.logoUrl,
            categoryCodeList = categoryCodeList,
            publicFlag = storeBaseFeatureDataPO?.publicFlag ?: true,
            publisher = storeBaseDataPO.publisher
        )
        val addMarketTemplateResultKeys = mutableSetOf<String>()
        val projectTemplateMap = mutableMapOf<String, String>()
        projectCodeList.forEach { projectCode ->
            val addMarketTemplateResult = client.get(ServicePTemplateResource::class)
                .addMarketTemplate(
                    userId = userId,
                    projectId = projectCode,
                    addMarketTemplateRequest = addMarketTemplateRequest
                )
            logger.info("addMarketTemplateResult is $addMarketTemplateResult")
            if (addMarketTemplateResult.isNotOk()) {
                throw ErrorCodeException(
                    errorCode = StoreMessageCode.STORE_INSTALL_VALIDATE_FAIL,
                    params = arrayOf(storeBaseDataPO.storeCode, addMarketTemplateResult.message ?: "")
                )
            }
            addMarketTemplateResult.data?.keys?.let {
                addMarketTemplateResultKeys.addAll(it)
            }
            addMarketTemplateResult.data?.let {
                projectTemplateMap.putAll(it)
            }
        }
        val templateProjectInfos = client.get(ServicePTemplateResource::class)
            .getTemplateIdBySrcCode(
                srcTemplateId = storeBaseDataPO.storeCode,
                projectIds = projectCodeList
            ).data ?: emptyList()
        val installProjectTemplateDTO = templateProjectInfos.map { optionalTemplateInfo ->
            InstallProjectTemplateDTO(
                name = optionalTemplateInfo.name,
                templateId = optionalTemplateInfo.templateId,
                projectId = optionalTemplateInfo.projectId,
                version = optionalTemplateInfo.srcTemplateVersion,
                versionName = optionalTemplateInfo.versionName,
                templateType = optionalTemplateInfo.templateType,
                templateTypeDesc = optionalTemplateInfo.templateTypeDesc,
                category = optionalTemplateInfo.category,
                logoUrl = optionalTemplateInfo.logoUrl,
                stages = optionalTemplateInfo.stages,
                srcTemplateId = optionalTemplateInfo.srcTemplateId
            )
        }
        projectCodeList.removeAll(addMarketTemplateResultKeys)
        // 更新生成的模板的红线规则
        copyQualityRule(
            userId = userId,
            templateCode = storeBaseDataPO.storeCode,
            projectCodeList = addMarketTemplateResultKeys,
            projectTemplateMap = projectTemplateMap
        )
        if (projectCodeList.isNotEmpty()) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_INSTALL_TEMPLATE_CODE_IS_INVALID,
                params = arrayOf(
                    storeBaseDataPO.name,
                    projectCodeList.joinToString(",")
                ),
                data = false,
                language = I18nUtil.getLanguage(userId)
            )
        }
        return Result(true)
    }

    private fun copyQualityRule(
        userId: String,
        templateCode: String,
        projectCodeList: Collection<String>,
        projectTemplateMap: Map<String, String>
    ) {
        try {
            logger.info("start to copy the quality rule for template: $templateCode")
            val sourceTemplate = client.get(ServicePTemplateResource::class).listTemplateById(
                setOf(templateCode), null, null
            ).data?.templates!!.getValue(templateCode)
            projectCodeList.forEach { projectCode ->
                client.get(ServiceQualityRuleResource::class).copyRule(
                    CopyRuleRequest(
                        sourceTemplate.projectId,
                        templateCode,
                        projectCode,
                        projectTemplateMap[projectCode] ?: "",
                        userId
                    )
                )
            }
        } catch (e: Exception) {
            logger.error("fail to copy the quality rule for template: $templateCode", e)
        }
    }

}