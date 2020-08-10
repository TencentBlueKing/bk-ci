package com.tencent.bk.codecc.apiquery.vo.pipeline;

import com.tencent.bk.codecc.apiquery.vo.ToolConfigInfoVO;
import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 流水线CodeCC原子视图
 *
 * @version V4.0
 * @date 2019/11/11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("流水线CodeCC原子视图")
public class PipelineTaskVO extends CommonVO
{
    @ApiModelProperty("蓝盾项目ID")
    private String projectId;

    @ApiModelProperty("任务ID")
    private Long taskId;

    @ApiModelProperty("流水线ID")
    private String pipelineId;

    @ApiModelProperty("创建来源")
    private String createFrom;

    @ApiModelProperty("事业群ID")
    private Integer bgId;

    @ApiModelProperty("部门ID")
    private Integer deptId;

    @ApiModelProperty("有效工具列表")
    private List<String> tools;
}
