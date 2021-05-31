package com.tencent.devops.common.api.checkerset;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

/**
 * 规则集参数视图
 *
 * @version V1.0
 * @date 2020/1/5
 */
@Data
@ApiModel("规则集参数视图")
public class CheckerSetParamsVO
{
    /**
     * 语言列表
     */
    private List<CheckerSetCodeLangVO> codeLangs;

    /**
     * 规则集类型列表
     */
    private List<CheckerSetCategoryVO> catatories;

    /**
     * 规则集列表
     */
    private List<CheckerSetVO> checkerSets;

    /**
     * 来源
     */
    private List<CheckerSetCategoryVO> checkerSetSource;
}
