package com.tencent.bk.codecc.task.vo.pipeline;

import com.tencent.devops.common.api.CodeRepoVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 流水线构建信息视图
 *
 * @version V1.0
 * @date 2019/11/18
 */
@Data
@ApiModel("流水线CodeCC原子视图")
public class PipelineBuildInfoVO
{
    @ApiModelProperty(value = "代码仓库repoId列表，V2插件使用")
    private List<String> repoIds;

    @ApiModelProperty(value = "本次扫描的代码仓库列表，V3插件使用")
    private List<CodeRepoVO> codeRepos;

    @ApiModelProperty(value = "扫描白名单列表")
    private List<String> repoWhiteList;
}
