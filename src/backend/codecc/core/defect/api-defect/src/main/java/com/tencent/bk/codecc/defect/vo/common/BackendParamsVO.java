package com.tencent.bk.codecc.defect.vo.common;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

/**
 * 后端参数视图
 *
 * @version V1.0
 * @date 2019/12/14
 */
@Data
@ApiModel("后端参数视图")
public class BackendParamsVO
{
    private List<String> paramJsonRelateCheckerSetTools;

    private List<String> codeLangRelateCheckerSetTools;
}
