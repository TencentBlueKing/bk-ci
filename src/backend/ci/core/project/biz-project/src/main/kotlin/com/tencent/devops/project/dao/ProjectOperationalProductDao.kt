package com.tencent.devops.project.dao

import com.tencent.devops.model.project.tables.TOperationalProduct
import com.tencent.devops.project.pojo.OperationalProductInfo
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class ProjectOperationalProductDao {
    fun createOrUpdate(
        dslContext: DSLContext,
        operationalProductInfo: OperationalProductInfo
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
                operationalProductInfo.productId,
                operationalProductInfo.productName,
                operationalProductInfo.planProductName,
                operationalProductInfo.deptName,
                operationalProductInfo.bgName
            ).onDuplicateKeyUpdate()
                .set(PRODUCT_NAME, operationalProductInfo.productName)
                .set(PLAN_PRODUCT_NAME, operationalProductInfo.planProductName)
                .set(DEPT_NAME, operationalProductInfo.deptName)
                .set(BG_NAME, operationalProductInfo.bgName)
                .execute()
        }
    }
}
