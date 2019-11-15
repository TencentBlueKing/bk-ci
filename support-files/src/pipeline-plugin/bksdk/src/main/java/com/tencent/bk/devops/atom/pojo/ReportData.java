package com.tencent.bk.devops.atom.pojo;

import com.tencent.bk.devops.atom.common.DataType;
import com.tencent.bk.devops.atom.common.ReportType;
import lombok.Getter;
import lombok.Setter;

/**
 * "out_var_3": {
 * "type": "report",
 * "reportType": "", # 报告类型 INTERNAL 内置报告， THIRDPARTY 第三方链接， 默认为INTERNAL
 * "label": "",      # 报告别名，用于产出物报告界面标识当前报告
 * "path": "",       # reportType=INTERNAL时，报告目录所在路径，相对于工作空间
 * "target": "",     # reportType=INTERNAL时，报告入口文件
 * "url": ""         # reportType=THIRDPARTY时，报告链接，当报告可以通过url访问时使用
 * }
 *
 * @version 1.0
 */
@Getter
@Setter
@SuppressWarnings("all")
public class ReportData extends DataField {

    private String label;
    private String path;
    private String target;
    private String url;
    private ReportType reportType;

    public ReportData(String label, String url) {
        super(DataType.report);
        this.label = label;
        this.url = url;
        this.reportType = ReportType.THIRDPARTY;
    }

    public ReportData(String label, String path, String target) {
        super(DataType.report);
        this.label = label;
        this.path = path;
        this.target = target;
        this.reportType = ReportType.INTERNAL;
    }

    public static ReportData createUrlReport(String label, String url) {
        return new ReportData(label, url);
    }

    public static ReportData createLocalReport(String label, String path, String target) {
        return new ReportData(label, path, target);
    }
}
