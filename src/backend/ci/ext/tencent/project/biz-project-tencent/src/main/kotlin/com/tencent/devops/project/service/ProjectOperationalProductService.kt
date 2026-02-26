package com.tencent.devops.project.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.devops.auth.pojo.ResponseDTO
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.project.dao.ProjectOperationalProductDao
import com.tencent.devops.project.pojo.CrosProductVO
import com.tencent.devops.project.pojo.ICosProductVO
import com.tencent.devops.project.pojo.ObsBaseDictDTO
import com.tencent.devops.project.pojo.ObsOperationalProductResponse
import com.tencent.devops.project.pojo.OperationalProductVO
import com.tencent.devops.project.pojo.ProjectCreateInfo
import com.tencent.devops.project.pojo.ProjectUpdateInfo
import com.tencent.devops.project.pojo.enums.ProjectProductDictType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.annotation.PostConstruct
import okhttp3.Request
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.Executors

@Service
class ProjectOperationalProductService(
    val dslContext: DSLContext,
    val projectOperationalProductDao: ProjectOperationalProductDao,
    val config: CommonConfig,
    val objectMapper: ObjectMapper
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ProjectOperationalProductService::class.java)
        private const val IEG_CHINESE_NAME = "IEG互动娱乐事业群"
        private const val TEG_CHINESE_NAME = "TEG技术工程事业群"
        private const val WXG_CHINESE_NAME = "WXG微信事业群"

        // bkCosts API 路径
        private const val BK_COSTS_GET_FULL_MAPPING_DATA = "/meta/get_full_mapping_data/"
        private const val BK_COSTS_GET_DEVOPS_LIST = "/api/v1/meta/get_devops_list?devops_id=bkdevops"
        private const val BK_COSTS_BATCH_ADD_DEVOPS_DATA = "/api/v1/meta/batch_add_devops_data/"
        private const val BK_COSTS_BATCH_UPDATE_DEVOPS_DATA = "/api/v1/meta/batch_update_devops_data"
    }

    private val executor = Executors.newSingleThreadExecutor()

    private val bgName2ProductList = mutableMapOf<String, MutableList<OperationalProductVO>>()

    private val productInfoList = mutableListOf<OperationalProductVO>()

    private val crosProductList = mutableListOf<CrosProductVO>()

    @Value("\${obs.url:#{null}}")
    private var obsUrl: String = ""

    @Value("\${obs.token:#{null}}")
    private var obsToken: String = ""

    @Value("\${esb.appCode:}")
    private val appCode = ""

    @Value("\${esb.appSecret:}")
    private val appSecret = ""

    @Value("\${bk.costs.url:}")
    private val bkCostsUrl = ""

    @Value("\${cros.url:}")
    private val crosUrl = ""

    // bkCosts 货币化相关配置（支持 YAML 列表格式）
    @Value("\${bk.costs.bgId:}")
    private val costsBgIds: List<String> = emptyList()

    @Value("\${bk.costs.businessLineId:}")
    private val costsBusinessLineIds: List<String> = emptyList()

    @Value("\${bk.costs.deptId:}")
    private val costsDeptIds: List<String> = emptyList()

    @Value("\${bk.costs.centerId:}")
    private val costsCenterIds: List<String> = emptyList()

    @Value("\${bk.costs.excludeDeptId:}")
    private val costsExcludeDeptIds: List<String> = emptyList()

    @PostConstruct
    fun syncOperationalProduct(): Boolean {
        logger.info("sync operational product start!")
        executor.execute {
            // 获取OBS运营产品
            val obsProductList = getOperationalProductsByDictType(
                dictType = ProjectProductDictType.OBS_PRODUCT
            )
            val planProductList = getOperationalProductsByDictType(
                dictType = ProjectProductDictType.PLAN_PRODUCT
            )
            val deptList = getOperationalProductsByDictType(
                dictType = ProjectProductDictType.DEPT
            )
            val bgList = getOperationalProductsByDictType(
                dictType = ProjectProductDictType.BG
            )
            // 获取映射关系
            val iCosProductVOs = getICosProduct()
            // 获取kpi信息
            val crosProductVOs = getCrosProduct()
            // 同步时，清空缓存，防止数据重复
            productInfoList.clear()
            bgName2ProductList.clear()
            crosProductList.clear()
            crosProductList.addAll(crosProductVOs)
            obsProductList.forEach { obsProductInfo ->
                val planProductInfo = planProductList.firstOrNull {
                    it.planProductId == obsProductInfo.planProductId
                }
                val deptInfo = deptList.firstOrNull {
                    it.deptId == planProductInfo?.deptId
                }
                val bgInfo = bgList.firstOrNull {
                    it.bgId == deptInfo?.bgId
                }
                val productId = obsProductInfo.productId!!.toInt()

                val iCosProductVO = iCosProductVOs.firstOrNull {
                    it.productId == productId && !it.iCosProductCode.isNullOrBlank()
                }
                val crosProductVO = iCosProductVO?.let { iCosProduct ->
                    crosProductVOs.firstOrNull {
                        iCosProduct.iCosProductCode == it.kpiCode
                    }
                }
                val crosCheck = crosProductVO?.crosCheck

                val operationalProductVO = OperationalProductVO(
                    productId = obsProductInfo.productId!!.toInt(),
                    productName = obsProductInfo.productName ?: "",
                    planProductName = planProductInfo?.planProductName ?: "",
                    deptName = deptInfo?.deptName ?: "",
                    bgName = bgInfo?.bgName ?: "",
                    iCosProductCode = iCosProductVO?.iCosProductCode,
                    iCosProductName = iCosProductVO?.iCosProductName,
                    crosCheck = crosCheck == 1
                )

                projectOperationalProductDao.createOrUpdate(
                    dslContext = dslContext,
                    operationalProductVO = operationalProductVO
                )
                productInfoList.add(operationalProductVO)
                val productListWithBgName = bgName2ProductList.getOrPut(
                    operationalProductVO.bgName!!
                ) { mutableListOf() }
                productListWithBgName.add(operationalProductVO)
            }
            bgName2ProductList["all"] = productInfoList
            logger.info("sync operational product finish!")
        }
        return true
    }

    fun listProductByBgName(bgName: String): List<OperationalProductVO>? {
        // 由于历史原因，用户注册OBS运营产品时的BG名称，有些已经和现在现存的BG对应不上了，所以需要进行特殊处理。
        return if (bgName == IEG_CHINESE_NAME || bgName == WXG_CHINESE_NAME || bgName == TEG_CHINESE_NAME) {
            bgName2ProductList[bgName]
        } else {
            bgName2ProductList["all"]
        }
    }

    fun listAllProducts(): List<OperationalProductVO> {
        return productInfoList
    }

    fun getProductByProductId(productId: Int): OperationalProductVO? {
        return productInfoList.firstOrNull { it.productId == productId }
    }

    private fun getOperationalProductsByDictType(dictType: ProjectProductDictType): List<OperationalProductVO> {
        return try {
            val obsBaseDictDTO = ObsBaseDictDTO(
                jsonrpc = "2.0",
                id = "0",
                method = "getObsBaseDict",
                params = mapOf(
                    "DeptId" to "2",
                    "StaffName" to "xx",
                    "DictType" to dictType.value.toString()
                )
            )
            val requestBody = JsonUtils.objectMapper.writeValueAsString(obsBaseDictDTO)
            OkhttpUtils.doPost(
                url = "${config.devopsWhiteProxy}$obsUrl",
                jsonParam = requestBody,
                headers = mapOf("Authorization" to "Bearer $obsToken")
            ).use {
                if (!it.isSuccessful) {
                    logger.warn("request obs products failed,response:($it)")
                    throw RemoteServiceException("request failed, response:($it)")
                }
                val responseStr = it.body!!.string()
                JsonUtils.objectMapper.readValue(responseStr, ObsOperationalProductResponse::class.java)
            }.result.data
        } catch (ignore: Exception) {
            logger.warn("get obs products fail!${ignore.message}")
            emptyList()
        }
    }

    // 获取KPI产品列表
    fun getCrosProduct(): List<CrosProductVO> {
        val request = Request.Builder().url(crosUrl).get().build()
        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                logger.warn("request failed, uri:($crosUrl)|response: ($it)")
                throw RemoteServiceException("request failed, response:($it)")
            }
            val responseStr = it.body!!.string()
            val responseDTO: CrosResponseDTO =
                objectMapper.readValue(responseStr, object : TypeReference<CrosResponseDTO>() {})
            if (responseDTO.ret != 0) {
                // 请求错误
                logger.warn("request failed, url:($bkCostsUrl)|response:($it)")
                throw RemoteServiceException("request failed, response:(${responseDTO.msg})")
            }
            if (logger.isDebugEnabled) {
                logger.debug("request response：${objectMapper.writeValueAsString(responseDTO.data)}")
            }
            return responseDTO.data
        }
    }

    private fun getICosProduct(): List<ICosProductVO> {
        val url = "$bkCostsUrl$BK_COSTS_GET_FULL_MAPPING_DATA"
        val responseDTO = doGetBkCostsRequest<List<ICosProductVO>>(url)
        return responseDTO.data ?: emptyList()
    }

    /**
     * 获取KPI产品列表
     * @param kpiName KPI产品名称（模糊搜索），为空时返回全部
     */
    fun getKpiProducts(kpiName: String? = null): List<CrosProductVO> {
        val products = if (crosProductList.isNotEmpty()) {
            crosProductList.toList()
        } else {
            try {
                getCrosProduct()
            } catch (e: Exception) {
                logger.warn("getKpiProducts failed: ${e.message}")
                emptyList()
            }
        }

        // 如果指定了 kpiName，则进行模糊搜索
        return if (kpiName.isNullOrBlank()) {
            products
        } else {
            products.filter {
                it.kpiName.contains(kpiName, ignoreCase = true) ||
                    it.kpiCode.contains(kpiName, ignoreCase = true)
            }.distinct()
        }
    }

    fun getBkCostsProjectInfo(
        devopsId: String,
        page: Int = 1,
        pageSize: Int = 100
    ): DevopsListResponseData {
        val requestBody = objectMapper.writeValueAsString(
            mapOf(
                "devops_id" to listOf(devopsId),
                "page" to page,
                "pagesize" to pageSize
            )
        )
        val url = "$bkCostsUrl$BK_COSTS_GET_DEVOPS_LIST"
        val responseDTO = doPostBkCostsRequest<DevopsListResponseData>(url, requestBody)
        return responseDTO.data ?: DevopsListResponseData(count = 0, list = emptyList())
    }

    /**
     * 批量添加蓝盾项目信息
     */
    fun batchAddDevopsData(addDevopsDataList: List<DevopsDataVO>): Boolean {
        val requestBody = objectMapper.writeValueAsString(
            mapOf("add_devops_data_list" to addDevopsDataList)
        )
        val url = "$bkCostsUrl$BK_COSTS_BATCH_ADD_DEVOPS_DATA"
        doPostBkCostsRequest<Any>(url, requestBody)
        return true
    }

    /**
     * 批量更新蓝盾项目信息
     */
    fun batchUpdateDevopsData(updateDevopsDataList: List<DevopsDataVO>): Boolean {
        val requestBody = objectMapper.writeValueAsString(
            mapOf("update_devops_data_list" to updateDevopsDataList)
        )
        val url = "$bkCostsUrl$BK_COSTS_BATCH_UPDATE_DEVOPS_DATA"
        doPostBkCostsRequest<Any>(url, requestBody)
        return true
    }

    /**
     * 同步 KPI 产品绑定到 bkCosts（项目创建时调用）
     * 如果指定了 kpiCode，则调用 batchAddDevopsData 将绑定关系同步到 bkCosts
     *
     * @param projectCreateInfo 项目创建信息
     */
    fun syncKpiProductOnCreate(projectCreateInfo: ProjectCreateInfo) {
        val kpiCode = projectCreateInfo.kpiCode
        val englishName = projectCreateInfo.englishName
        if (kpiCode.isNullOrBlank()) {
            logger.info("syncKpiProductOnCreate|$englishName|kpiCode is null, skip sync")
            return
        }

        try {
            val devopsData = DevopsDataVO(
                devopsId = englishName,
                devopsName = projectCreateInfo.projectName,
                obsProductId = projectCreateInfo.productId,
                obsProductName = projectCreateInfo.productName,
                icosProductCode = kpiCode,
                icosProductName = projectCreateInfo.kpiName,
                businessManager = null,
                isDeleted = false
            )
            batchAddDevopsData(listOf(devopsData))
            logger.info("syncKpiProductOnCreate|$englishName|success|kpiCode=$kpiCode")
        } catch (e: Exception) {
            // 同步失败不阻塞项目创建流程，仅记录警告日志
            logger.warn("syncKpiProductOnCreate|$englishName|failed|kpiCode=$kpiCode|error=${e.message}", e)
        }
    }

    /**
     * 同步 KPI 产品绑定到 bkCosts（项目更新时调用）
     * 如果指定了 kpiCode，则调用 batchUpdateDevopsData 将绑定关系同步到 bkCosts
     *
     * @param projectUpdateInfo 项目更新信息
     */
    fun syncKpiProductOnUpdate(projectUpdateInfo: ProjectUpdateInfo) {
        val kpiCode = projectUpdateInfo.kpiCode
        val englishName = projectUpdateInfo.englishName
        if (kpiCode.isNullOrBlank()) {
            logger.info("syncKpiProductOnUpdate|$englishName|kpiCode is null, skip sync")
            return
        }

        try {
            val devopsData = getBkCostsProjectInfo(englishName).list.firstOrNull()
            val devopsDataVo = DevopsDataVO(
                devopsId = englishName,
                devopsName = projectUpdateInfo.projectName,
                obsProductId = projectUpdateInfo.productId,
                obsProductName = projectUpdateInfo.productName,
                icosProductCode = kpiCode,
                icosProductName = projectUpdateInfo.kpiName,
                businessManager = null,
                isDeleted = false
            )
            if (devopsData != null) {
                batchUpdateDevopsData(listOf(devopsDataVo))
            } else {
                batchAddDevopsData(listOf(devopsDataVo))
            }
            logger.info("syncKpiProductOnUpdate|$englishName|success|kpiCode=$kpiCode")
        } catch (e: Exception) {
            // 同步失败不阻塞项目更新流程，仅记录警告日志
            logger.warn("syncKpiProductOnUpdate|$englishName|failed|kpiCode=$kpiCode|error=${e.message}", e)
        }
    }

    /**
     * 判断是否需要进行货币化
     * 根据配置文件中的部门ID列表，判断传入的组织信息是否在货币化范围内
     *
     * 判断逻辑：
     * 1. 首先检查是否在排除部门列表中（excludeDeptId），如果在则返回 false
     * 2. 然后依次检查 bgId、businessLineId、deptId、centerId 是否在配置的列表中
     * 3. 任意一个匹配则返回 true，否则返回 false
     *
     * @param bgId 事业群ID
     * @param businessLineId 业务线ID
     * @param deptId 部门ID
     * @param centerId 中心ID
     * @return 是否需要进行货币化
     */
    fun checkNeedMonetization(
        bgId: String?,
        businessLineId: String?,
        deptId: String?,
        centerId: String?
    ): Boolean {
        // 如果任意传入的ID在排除列表中，则不需要货币化
        val inputIds = listOfNotNull(bgId, businessLineId, deptId, centerId)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (inputIds.any { it in costsExcludeDeptIds }) {
            logger.info(
                "checkNeedMonetization|excluded|bgId=$bgId|businessLineId=$businessLineId|" +
                    "deptId=$deptId|centerId=$centerId"
            )
            return false
        }

        // 检查是否匹配配置的组织ID
        val bgMatch = !bgId.isNullOrBlank() && bgId.trim() in costsBgIds
        val businessLineMatch = !businessLineId.isNullOrBlank() &&
            businessLineId.trim() in costsBusinessLineIds
        val deptMatch = !deptId.isNullOrBlank() && deptId.trim() in costsDeptIds
        val centerMatch = !centerId.isNullOrBlank() && centerId.trim() in costsCenterIds

        val needMonetization = bgMatch || businessLineMatch || deptMatch || centerMatch

        logger.info(
            "checkNeedMonetization|result=$needMonetization|bgId=$bgId(match=$bgMatch)|" +
                "businessLineId=$businessLineId(match=$businessLineMatch)|" +
                "deptId=$deptId(match=$deptMatch)|centerId=$centerId(match=$centerMatch)"
        )

        return needMonetization
    }

    /**
     * 通用 bkCosts GET 请求
     */
    private inline fun <reified T> doGetBkCostsRequest(url: String): ResponseDTO<T> {
        val headerStr = objectMapper.writeValueAsString(
            mapOf("bk_app_code" to appCode, "bk_app_secret" to appSecret)
        ).replace("\\s".toRegex(), "")

        val request = Request.Builder()
            .url(url)
            .addHeader("x-bkapi-authorization", headerStr)
            .get()
            .build()

        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                logger.warn("request failed, url:($url)|response: ($it)")
                throw RemoteServiceException("request failed, response:($it)")
            }
            val responseStr = it.body!!.string()
            val responseDTO: ResponseDTO<T> =
                objectMapper.readValue(responseStr, object : TypeReference<ResponseDTO<T>>() {})
            if (responseDTO.code != 200L || !responseDTO.result) {
                logger.warn("request failed, url:($url)|response:($responseStr)")
                throw RemoteServiceException("request failed, response:(${responseDTO.message})")
            }
            if (logger.isDebugEnabled) {
                logger.debug("bkCosts GET response：${objectMapper.writeValueAsString(responseDTO.data)}")
            }
            return responseDTO
        }
    }

    /**
     * 通用 bkCosts POST 请求
     */
    private inline fun <reified T> doPostBkCostsRequest(url: String, requestBody: String): ResponseDTO<T> {
        val headerStr = objectMapper.writeValueAsString(
            mapOf("bk_app_code" to appCode, "bk_app_secret" to appSecret)
        ).replace("\\s".toRegex(), "")

        OkhttpUtils.doPost(
            url = url,
            jsonParam = requestBody,
            headers = mapOf(
                "Content-Type" to "application/json",
                "x-bkapi-authorization" to headerStr
            )
        ).use {
            if (!it.isSuccessful) {
                logger.warn("request failed, url:($url)|response: ($it)")
                throw RemoteServiceException("request failed, response:($it)")
            }
            val responseStr = it.body!!.string()
            val responseDTO: ResponseDTO<T> =
                objectMapper.readValue(responseStr, object : TypeReference<ResponseDTO<T>>() {})
            if (responseDTO.code != 200L || !responseDTO.result) {
                logger.warn("request failed, url:($url)|response:($responseStr)")
                throw RemoteServiceException("request failed, response:(${responseDTO.message})")
            }
            if (logger.isDebugEnabled) {
                logger.debug("bkCosts POST response：${objectMapper.writeValueAsString(responseDTO.data)}")
            }
            return responseDTO
        }
    }

    @Schema(title = "请求返回实体")
    data class CrosResponseDTO(
        @get:Schema(title = "返回码")
        val ret: Int,
        @get:Schema(title = "返回信息")
        val msg: String?,
        @get:Schema(title = "请求返回数据")
        val data: List<CrosProductVO>
    )

    @Schema(title = "蓝盾项目列表响应数据")
    data class DevopsListResponseData(
        @get:Schema(title = "总数")
        val count: Int,
        @get:Schema(title = "项目列表")
        val list: List<DevopsDataVO>
    )

    @Schema(title = "蓝盾项目数据")
    data class DevopsDataVO(
        @get:Schema(title = "蓝盾项目ID")
        @JsonProperty("devops_id")
        val devopsId: String,
        @get:Schema(title = "蓝盾项目名称")
        @JsonProperty("devops_name")
        val devopsName: String,
        @get:Schema(title = "OBS产品ID")
        @JsonProperty("obs_product_id")
        val obsProductId: Int?,
        @get:Schema(title = "OBS产品名称")
        @JsonProperty("obs_product_name")
        val obsProductName: String?,
        @get:Schema(title = "iCos产品编码")
        @JsonProperty("icos_product_code")
        val icosProductCode: String?,
        @get:Schema(title = "iCos产品名称")
        @JsonProperty("icos_product_name")
        val icosProductName: String?,
        @get:Schema(title = "业务负责人")
        @JsonProperty("business_manager")
        val businessManager: String?,
        @get:Schema(title = "是否删除")
        @JsonProperty("is_deleted")
        val isDeleted: Boolean? = false
    )
}
