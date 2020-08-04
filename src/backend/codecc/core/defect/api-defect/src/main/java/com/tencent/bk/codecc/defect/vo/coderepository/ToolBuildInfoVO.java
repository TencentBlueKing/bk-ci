package com.tencent.bk.codecc.defect.vo.coderepository;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.util.List;

/**
 * 工具构建信息视图
 *
 * @version V1.0
 * @date 2019/11/17
 */
@Data
@ApiModel("代码仓库信息视图")
public class ToolBuildInfoVO extends CommonVO
{
    /**
     * 强制全量扫描标志 Y：下次全量扫描 N：按任务配置
     */
    private String forceFullScan;

    /**
     * 仓库列表
     */
    private List<CodeRepoVO> repoList;

    /**
     * 扫描目录白名单
     */
    private List<String> repoWhiteList;
}
