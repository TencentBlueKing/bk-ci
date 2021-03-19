package com.tencent.bk.codecc.defect.vo.customtool;

import com.tencent.devops.common.api.CommonVO;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 文件作者关联实体类
 *
 * @version V4.0
 * @date 2019/10/15
 */
@Data
public class ScmBlameVO extends CommonVO
{
    private long taskId;

    private String branch;

    private long fileUpdateTime;

    private String fileRelPath;

    private String revision;

    private String filePath;

    private String url;

    private String rootUrl;

    private String scmType;

    private Map<String, String> extraInfoMap;

    private List<ScmBlameChangeRecordVO> changeRecords;
}
