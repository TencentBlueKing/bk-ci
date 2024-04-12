package com.tencent.devops.project.service

import com.tencent.bkrepo.common.api.util.JsonUtils
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.project.dao.ProjectOperationalProductDao
import com.tencent.devops.project.pojo.ObsBaseDictDTO
import com.tencent.devops.project.pojo.ObsOperationalProductResponse
import com.tencent.devops.project.pojo.OperationalProductVO
import com.tencent.devops.project.pojo.enums.ProjectProductDictType
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class ProjectOperationalProductService constructor(
    val dslContext: DSLContext,
    val projectOperationalProductDao: ProjectOperationalProductDao,
    val config: CommonConfig
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ProjectOperationalProductService::class.java)
    }

    private val bgName2ProductList = mutableMapOf<String, MutableList<OperationalProductVO>>()

    private val productInfoList = mutableListOf<OperationalProductVO>()

    @Value("\${obs.url:#{null}}")
    private var obsUrl: String = ""

    @Value("\${obs.token:#{null}}")
    private var obsToken: String = ""

    @PostConstruct
    fun syncOperationalProduct(): Boolean {
        logger.info("sync operational product start!")
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

            val operationalProductVO = OperationalProductVO(
                productId = obsProductInfo.productId!!.toInt(),
                productName = obsProductInfo.productName ?: "",
                planProductName = planProductInfo?.planProductName ?: "",
                deptName = deptInfo?.deptName ?: "",
                bgName = bgInfo?.bgName ?: ""
            )

            projectOperationalProductDao.createOrUpdate(
                dslContext = dslContext,
                operationalProductVO = operationalProductVO
            )
            productInfoList.add(operationalProductVO)
            val productListWithBgName = bgName2ProductList.getOrPut(operationalProductVO.bgName!!) { mutableListOf() }
            productListWithBgName.add(operationalProductVO)
        }
        bgName2ProductList["all"] = productInfoList
        logger.info("sync operational product finish!")
        return true
    }

    fun listProductByBgName(bgName: String): List<OperationalProductVO>? {
        return if (bgName2ProductList.containsKey(bgName)) {
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
                url = "${config.devopsHostGateway}$obsUrl",
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
}
