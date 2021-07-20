package com.tencent.bk.codecc.task.vo;

import com.tencent.devops.common.api.CommonVO;
import com.tencent.devops.common.api.checkerset.CheckerSetVO;
import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * 工具特殊参数和规则集视图
 *
 * @version V1.0
 * @date 2019/11/25
 */
@Data
@ApiModel("工具特殊参数和规则集视图")
public class ToolParamJsonAndCheckerSetVO extends CommonVO
{
    /**
     * 工具名称
     */
    private String toolName;

    /**
     * 个性化参数
     */
    private String paramJson;

    /**
     * 规则集ID
     */
    private CheckerSetVO checkerSet;
}
