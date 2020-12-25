package com.tencent.bk.codecc.task.vo.pipeline;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 流水线工具参数视图
 *
 * @version V4.0
 * @date 2019/11/12
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ApiModel("流水线工具参数视图")
public class PipelineToolParamVO extends CommonVO
{
    private String paramKey;

    private String paramValue;
}
