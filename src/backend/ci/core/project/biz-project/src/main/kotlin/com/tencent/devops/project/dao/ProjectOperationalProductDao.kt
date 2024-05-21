package com.tencent.devops.project.dao

import com.tencent.devops.model.project.tables.TOperationalProduct
import com.tencent.devops.model.project.tables.records.TOperationalProductRecord
import com.tencent.devops.project.pojo.OperationalProductVO
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class ProjectOperationalProductDao {
    fun createOrUpdate(
        dslContext: DSLContext,
        operationalProductVO: OperationalProductVO
    ) {
        with(TOperationalProduct.T_OPERATIONAL_PRODUCT) {
            dslContext.insertInto(
                this,
                PRODUCT_ID,
                PRODUCT_NAME,
                PLAN_PRODUCT_NAME,
                DEPT_NAME,
                BG_NAME
            ).values(
                operationalProductVO.productId,
                operationalProductVO.productName,
                operationalProductVO.planProductName,
                operationalProductVO.deptName,
                operationalProductVO.bgName
            ).onDuplicateKeyUpdate()
                .set(PRODUCT_NAME, operationalProductVO.productName)
                .set(PLAN_PRODUCT_NAME, operationalProductVO.planProductName)
                .set(DEPT_NAME, operationalProductVO.deptName)
                .set(BG_NAME, operationalProductVO.bgName)
                .execute()
        }
    }

    fun listByBgName(
        dslContext: DSLContext,
        bgName: String
    ): List<OperationalProductVO> {
        return with(TOperationalProduct.T_OPERATIONAL_PRODUCT) {
            dslContext.selectFrom(this).where(BG_NAME.eq(bgName)).fetch()
        }.map {
            it.convert()
        }
    }

    fun TOperationalProductRecord.convert(): OperationalProductVO {
        return OperationalProductVO(
            productId = productId,
            productName = productName,
            planProductName = planProductName,
            deptName = deptName,
            bgName = bgName
        )
    }
}
