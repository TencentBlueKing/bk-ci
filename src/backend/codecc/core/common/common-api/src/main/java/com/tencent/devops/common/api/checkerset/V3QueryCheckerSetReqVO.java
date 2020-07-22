package com.tencent.devops.common.api.checkerset;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * 查询规则集列表请求体
 *
 * @version V1.0
 * @date 2020/1/2
 */
@Data
@ApiModel("查询规则集列表请求体")
public class V3QueryCheckerSetReqVO
{
    /**
     * 项目ID
     */
    private String projectId;

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序方向
     */
    private String sortType;
}
