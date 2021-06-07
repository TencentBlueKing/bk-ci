package com.tencent.bk.codecc.apiquery.defect

import com.tencent.devops.common.api.CommonVO
import com.tencent.devops.common.api.pojo.Page

data class DefectDetailVO(
    val defectDetailList: Page<CommonVO>
)