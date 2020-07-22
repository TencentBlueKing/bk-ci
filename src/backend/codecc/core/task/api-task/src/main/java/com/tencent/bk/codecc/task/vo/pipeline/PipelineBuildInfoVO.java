package com.tencent.bk.codecc.task.vo.pipeline;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 流水线构建信息视图
 *
 * @version V1.0
 * @date 2019/11/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("流水线CodeCC原子视图")
public class PipelineBuildInfoVO extends CommonVO
{
    /**
     * 代码仓库列表
     */
    private List<String> repoIds;

    /**
     * 扫描白名单列表
     */
    private List<String> repoWhiteList;
}
