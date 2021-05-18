package com.tencent.bk.codecc.apiquery.service

import com.tencent.bk.codecc.apiquery.defect.model.CheckerDetailModel

interface ICheckerService {


    /**
     * 根据规则集类型查询规则详情
     * @param checkerSetType
     * @return
     */
    fun queryCheckerDetail(checkerSetType: String): List<CheckerDetailModel>
}