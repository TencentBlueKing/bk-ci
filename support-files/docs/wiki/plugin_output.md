# 插件产出规范

---

- 插件执行完毕后，支持输出变量传递给流水线下游步骤使用，或者归档文件到仓库、归档报告到产出物报告
- 输出信息由写文件的方式指定，文件路径和名称由系统定义的环境变量指定
- 输出信息格式详细说明如下：

```json
{
    "status": "",      # 插件执行结果，值可以为success、failure、error
    "message": "",     # 插件执行结果说明，支持markdown格式
    "type": "default", # 产出数据模板类型，用于规定产出数据的解析入库方式。目前支持default、quality
    "data": {          # default模板的数据格式如下所示，各输出字段应先在task.json中定义
        "outVar_1": {
            "type": "string",
            "value": "testaaaaa"
        },
        "outVar_2": {
            "type": "artifact",
            "value": ["file_path_1", "file_path_2"] # 文件绝对路径，指定后，agent自动将这些文件归档到仓库
        },
        "outVar_3": {
            "type": "report",
            "reportType": "", # 报告类型 INTERNAL 内置报告， THIRDPARTY 第三方链接， 默认为INTERNAL
            "label": "",      # 报告别名，用于产出物报告界面标识当前报告
            "path": "",       # reportType=INTERNAL时，报告目录所在路径，相对于工作空间
            "target": "",     # reportType=INTERNAL时，报告入口文件
            "url": ""         # reportType=THIRDPARTY时，报告链接，当报告可以通过url访问时使用
        }
    }
}
```
<br/>
