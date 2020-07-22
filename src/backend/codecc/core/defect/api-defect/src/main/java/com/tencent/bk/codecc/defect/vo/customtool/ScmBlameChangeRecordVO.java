package com.tencent.bk.codecc.defect.vo.customtool;

import com.tencent.devops.common.api.CommonVO;
import lombok.Data;

import java.util.List;

/**
 * 文件变更记录实体类
 *
 * @version V4.0
 * @date 2019/10/16
 */
@Data
public class ScmBlameChangeRecordVO extends CommonVO
{
    private String author;

    private List<Object> lines;

    private long lineUpdateTime;
}
