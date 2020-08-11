package com.tencent.bk.codecc.task.vo.pipeline;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 流水线代码仓库信息视图
 *
 * @version V1.0
 * @date 2019/11/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("流水线CodeCC原子视图")
public class CodeRepoInfoVO extends CommonVO
{
    private String repoId;

    private String revision;
}
