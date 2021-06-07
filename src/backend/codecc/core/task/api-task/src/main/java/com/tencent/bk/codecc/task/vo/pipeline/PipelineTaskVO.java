package com.tencent.bk.codecc.task.vo.pipeline;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
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
    private String projectId;

    private Long taskId;

    private String enName;

    private String cnName;

    private List<String> codeLanguages;

    private List<PipelineToolVO> tools;
}
