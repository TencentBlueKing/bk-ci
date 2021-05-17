package com.tencent.bk.codecc.defect.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分析记录分组查询视图类
 *
 * @version V1.0
 * @date 2019/5/17
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("分析记录代码库信息查询视图类")
public class TaskLogRepoInfoVO {
    @ApiModelProperty("代码库路径")
    private String repoUrl;

    @ApiModelProperty("代码库版本号")
    private String revision;

    @ApiModelProperty("提交时间")
    private String commitTime;

    @ApiModelProperty("提交用户")
    private String commitUser;

    @ApiModelProperty("分支名")
    private String branch;
}
