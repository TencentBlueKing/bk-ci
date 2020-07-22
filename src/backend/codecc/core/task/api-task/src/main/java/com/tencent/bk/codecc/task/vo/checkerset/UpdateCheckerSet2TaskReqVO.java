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
 * 添加规则集ID到任务工具配置请求体视图
 *
 * @version V4.0
 * @date 2019/11/1
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("清除任务工具对应的规则集请求体视图")
public class UpdateCheckerSet2TaskReqVO extends CommonVO
{
    @ApiModelProperty(value = "工具名称列表", required = true)
    private List<ToolCheckerSetVO> toolCheckerSets;
}
