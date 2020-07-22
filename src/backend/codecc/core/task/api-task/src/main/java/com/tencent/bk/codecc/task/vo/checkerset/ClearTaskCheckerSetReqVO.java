package com.tencent.bk.codecc.task.vo.checkerset;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 清除任务工具对应的规则集请求体视图
 *
 * @version V4.0
 * @date 2019/11/1
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("清除任务工具对应的规则集请求体视图")
public class ClearTaskCheckerSetReqVO extends CommonVO
{
    @ApiModelProperty(value = "工具名称列表", required = true)
    private List<String> toolNames;

    @ApiModelProperty(value = "是否需要同步到流水线", required = true)
    private Boolean needUpdatePipeline;
}
