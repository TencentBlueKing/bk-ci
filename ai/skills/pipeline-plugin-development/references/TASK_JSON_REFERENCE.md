# task.json 配置完整参考

> task.json 内容长度最大为 **64KB**

## 整体结构

```json
{
    "atomCode": "myAtom",           // 插件唯一标识（必填）
    "defaultLocaleLanguage": "zh_CN", // 默认语言（非必填）
    "execution": {                   // 执行配置（必填）
        "language": "python",
        "demands": [],
        "target": "demo"
    },
    "inputGroups": [],               // 输入字段分组（非必填）
    "input": {},                     // 输入字段定义（必填）
    "output": {},                    // 输出字段定义（非必填）
    "config": {}                     // 插件控制特性（非必填）
}
```

## execution 执行配置

| 属性名 | 说明 | 格式 | 必填 |
|--------|------|------|------|
| `language` | 开发语言 | 字符串 | 是 |
| `target` | 执行入口命令 | 字符串 | 是 |
| `demands` | 执行前置依赖命令 | 数组 | 否 |
| `runtimeVersion` | 运行时版本 | 字符串 | 否 |
| `finishKillFlag` | 执行完成后是否杀进程 | 布尔 | 否 |
| `os` | 操作系统相关配置 | 数组 | 否 |

### runtimeVersion 配置值

| 语言 | 配置值 | 备注 |
|------|--------|------|
| python | `python2` | 默认 |
| python | `python3` | 推荐 |
| nodejs | `10.*` ~ `18.*` | 默认 10.* |
| java | `8` | 默认 |
| java | `17` | 新版本 |

## input 输入字段配置

### 支持的 UI 组件类型

| 组件 type | 组件名称 | 获取到的值格式 |
|-----------|----------|----------------|
| `vuex-input` | 单行文本框 | 字符串 |
| `vuex-textarea` | 多行文本框 | 字符串 |
| `atom-ace-editor` | 代码编辑框 | 字符串 |
| `selector` | 下拉框（只选不输入） | 单选: 字符串; 多选: `["str1", "str2"]` |
| `select-input` | 可输入下拉框 | 同上 |
| `devops-select` | 可输入下拉框（仅变量） | 同上 |
| `atom-checkbox-list` | 复选框列表 | `["id1", "id2"]` |
| `atom-checkbox` | 复选框（布尔） | `"true"` 或 `"false"` |
| `enum-input` | 单选 Radio | 字符串 |
| `time-picker` | 日期选择器 | 字符串 |
| `staff-input` | 人名选择器（项目成员） | 字符串 |
| `company-staff-input` | 人名选择器（公司成员） | 字符串 |
| `tips` | 提示信息 | 无 |
| `parameter` | 不定参数列表 | JSON 字符串 |
| `dynamic-parameter` | 动态参数列表 | JSON 字符串 |
| `dynamic-parameter-simple` | 动态参数（简易版） | JSON 字符串 |

### 输入字段公共属性

| 属性名 | 说明 | 格式 | 必填 |
|--------|------|------|------|
| `label` | 中文名 | 字符串 | 是 |
| `type` | 组件类型 | 字符串 | 是 |
| `default` | 默认值 | 根据组件类型 | 否 |
| `placeholder` | 占位提示 | 字符串 | 否 |
| `groupName` | 所属分组 | 字符串 | 否 |
| `desc` | 字段说明 | 字符串 | 否 |
| `required` | 是否必填 | 布尔 | 否 |
| `disabled` | 是否禁用 | 布尔 | 否 |
| `hidden` | 是否隐藏 | 布尔 | 否 |
| `isSensitive` | 是否敏感信息 | 布尔 | 否 |
| `rely` | 条件显示/隐藏 | 对象 | 否 |
| `rule` | 值校验规则 | 对象 | 否 |

### 完整输入字段示例

```json
{
    "input": {
        "repositoryUrl": {
            "label": "代码库地址",
            "type": "vuex-input",
            "placeholder": "请输入 Git 仓库地址",
            "desc": "支持 HTTP/HTTPS/SSH 协议",
            "required": true,
            "rule": {
                "regex": "^(https?|git)://"
            }
        },
        "branch": {
            "label": "分支",
            "type": "select-input",
            "default": "master",
            "optionsConf": {
                "searchable": true,
                "multiple": false,
                "url": "/repository/api/user/repositories/{projectId}/branches",
                "paramId": "name",
                "paramName": "name"
            }
        },
        "enableCache": {
            "label": "",
            "type": "atom-checkbox",
            "text": "启用缓存",
            "default": true
        },
        "buildType": {
            "label": "构建类型",
            "type": "enum-input",
            "default": "release",
            "list": [
                {"label": "Debug", "value": "debug"},
                {"label": "Release", "value": "release"}
            ]
        },
        "advancedOptions": {
            "label": "高级选项",
            "type": "vuex-input",
            "rely": {
                "operation": "AND",
                "expression": [
                    {"key": "enableCache", "value": true}
                ]
            }
        }
    }
}
```

## output 输出字段配置

### 输出类型

| 类型 | 说明 | 限制 |
|------|------|------|
| `string` | 字符串变量 | 长度不超过 4KB |
| `artifact` | 构件文件 | 自动归档到仓库 |
| `report` | 报告文件 | 支持 HTML 渲染 |

### 输出数据结构

```json
{
    "status": "success",
    "message": "执行成功",
    "errorType": 1,
    "errorCode": 0,
    "platformCode": "",
    "platformErrorCode": 0,
    "type": "default",
    "data": {
        "outputVar1": {
            "type": "string",
            "value": "输出值"
        },
        "outputVar2": {
            "type": "artifact",
            "value": ["/path/to/file1", "/path/to/file2"],
            "artifactoryType": "PIPELINE",
            "path": "/test/"
        },
        "outputVar3": {
            "type": "report",
            "reportType": "INTERNAL",
            "label": "测试报告",
            "path": "/workspace/report",
            "target": "index.html"
        }
    }
}
```

### artifact 类型属性

| 属性 | 说明 |
|------|------|
| `value` | 文件绝对路径数组 |
| `artifactoryType` | `PIPELINE`（流水线仓库）或 `CUSTOM_DIR`（自定义仓库） |
| `path` | 自定义仓库时的相对路径 |

### report 类型属性

| 属性 | 说明 |
|------|------|
| `reportType` | `INTERNAL`（内置报告）或 `THIRDPARTY`（第三方链接） |
| `label` | 报告别名 |
| `path` | 报告目录绝对路径 |
| `target` | 入口文件（相对于 path） |
| `url` | 第三方报告链接（reportType=THIRDPARTY 时） |

## config 插件控制特性

```json
{
    "config": {
        "canPauseBeforeRun": false,
        "defaultTimeout": 60,
        "defaultFailPolicy": "MANUALLY_CONTINUE",
        "defaultRetryPolicy": ["AUTO_RETRY"],
        "retryTimes": 3
    }
}
```

## 内置变量

| 变量名 | 说明 |
|--------|------|
| `{projectId}` | 项目英文 ID |
| `{pipelineId}` | 流水线 ID |
| `{buildId}` | 构建 ID |
