package com.tencent.devops.project.service

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
    }

    private val executor = Executors.newSingleThreadExecutor()

    private val bgName2ProductList = mutableMapOf<String, MutableList<OperationalProductVO>>()

    private val productInfoList = mutableListOf<OperationalProductVO>()

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

    @PostConstruct
    fun syncOperationalProduct(): Boolean {
        logger.info("sync operational product start!")
        executor.execute {
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
            val iCosProductVOs = getICosProduct()
            val crosProductVOs = getCrosProduct()
            // 同步时，清空缓存，防止数据重复
            productInfoList.clear()
            bgName2ProductList.clear()
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
                    it.productId == productId
                }
                val crosCheck = iCosProductVO?.let { iCosProduct ->
                    crosProductVOs.firstOrNull {
                        iCosProduct.iCosProductCode == it.iCosProductCode
                    }?.crosCheck
                }

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

    private fun getICosProduct(): List<ICosProductVO> {
        val headerStr = objectMapper.writeValueAsString(
            mapOf("bk_app_code" to appCode, "bk_app_secret" to appSecret)
        ).replace("\\s".toRegex(), "")

        val request = Request.Builder()
            .url(bkCostsUrl)
            .addHeader("x-bkapi-authorization", headerStr)
            .get()
            .build()
        OkhttpUtils.doHttp(request).use {
            if (!it.isSuccessful) {
                logger.warn("request failed, uri:($bkCostsUrl)|response: ($it)")
                throw RemoteServiceException("request failed, response:($it)")
            }
            val responseStr = it.body!!.string()
            val responseDTO: ResponseDTO<List<ICosProductVO>> =
                objectMapper.readValue(responseStr, object : TypeReference<ResponseDTO<List<ICosProductVO>>>() {})
            if (responseDTO.code != 200L || !responseDTO.result) {
                // 请求错误
                logger.warn("request failed, url:($bkCostsUrl)|response:($it)")
                throw RemoteServiceException("request failed, response:(${responseDTO.message})")
            }
            if (logger.isDebugEnabled) {
                logger.debug("request response：${objectMapper.writeValueAsString(responseDTO.data)}")
            }
            return responseDTO.data ?: emptyList()
        }
    }

    private fun getCrosProduct(): List<CrosProductVO> {
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

    @Schema(title = "请求返回实体")
    data class CrosResponseDTO(
        @get:Schema(title = "返回码")
        val ret: Int,
        @get:Schema(title = "返回信息")
        val msg: String?,
        @get:Schema(title = "请求返回数据")
        val data: List<CrosProductVO>
    )
}
