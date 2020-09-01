package com.tencent.bk.codecc.defect.vo.coderepository;

import com.tencent.devops.common.api.CommonVO;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 代码仓库视图
 *
 * @version V1.0
 * @date 2019/11/17
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel("代码仓库信息视图")
public class CodeRepoVO extends CommonVO
{
    /**
     * 仓库ID
     */
    private String repoId;

    /**
     * 仓库版本号
     */
    private String revision;

    /**
     * 仓库分支
     */
    private String branch;

    /**
     * 仓库别名
     */
    private String aliasName;
}
