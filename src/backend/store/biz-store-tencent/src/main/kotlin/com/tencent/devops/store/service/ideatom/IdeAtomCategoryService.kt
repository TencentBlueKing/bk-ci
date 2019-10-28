package com.tencent.devops.store.service.ideatom

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.store.dao.ideatom.IdeAtomCategoryRelDao
import com.tencent.devops.store.pojo.common.Category
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class IdeAtomCategoryService @Autowired constructor(
    private val dslContext: DSLContext,
    private val ideAtomCategoryRelDao: IdeAtomCategoryRelDao
) {
    private val logger = LoggerFactory.getLogger(IdeAtomCategoryService::class.java)

    /**
     * 查找IDE插件范畴
     */
    fun getCategorysByAtomId(atomId: String): Result<List<Category>?> {
        logger.info("getCategorysByAtomCode atomId is :$atomId")
        val ideAtomCategoryList = mutableListOf<Category>()
        val ideAtomCategoryRecords = ideAtomCategoryRelDao.getCategorysByIdeAtomId(dslContext, atomId) // 查询IDE插件范畴信息
        ideAtomCategoryRecords?.forEach {
            ideAtomCategoryList.add(
                Category(
                    id = it["id"] as String,
                    categoryCode = it["categoryCode"] as String,
                    categoryName = it["categoryName"] as String,
                    iconUrl = it["iconUrl"] as? String,
                    categoryType = StoreTypeEnum.getStoreType((it["categoryType"] as Byte).toInt()),
                    createTime = (it["createTime"] as LocalDateTime).timestampmilli(),
                    updateTime = (it["updateTime"] as LocalDateTime).timestampmilli()
                )
            )
        }
        return Result(ideAtomCategoryList)
    }
}
