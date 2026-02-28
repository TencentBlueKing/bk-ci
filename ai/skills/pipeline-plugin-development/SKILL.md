---
name: pipeline-plugin-development
description: 流水线插件开发完整指南，涵盖插件创建、task.json 配置规范、多语言开发示例（Python/Java/NodeJS/Golang）、输入输出规范、错误码规范、发布流程、调试方法。当用户需要开发蓝盾流水线插件、配置 task.json、处理插件输入输出或排查插件错误时使用。
---

# 流水线插件开发完整指南

> **模块定位**: 本指南详细介绍蓝盾（BK-CI）流水线插件的开发规范、配置方法、多语言实现示例、发布流程和调试技巧，帮助开发者快速上手插件开发。

## 一、插件开发概述

### 1.1 什么是流水线插件

流水线插件（Atom）是蓝盾流水线中的最小执行单元，用于完成特定的构建任务，如代码拉取、编译、测试、部署等。

### 1.2 支持的开发语言

| 语言 | 推荐度 | SDK |
|------|--------|-----|
| **Java** | ⭐⭐⭐⭐⭐ | `java-atom-sdk` |
| **Python** | ⭐⭐⭐⭐ | `python-atom-sdk` |
| **NodeJS** | ⭐⭐⭐ | `@tencent/nodejs_atom_sdk` |
| **Golang** | ⭐⭐⭐ | `golang-atom-sdk` |

### 1.3 插件开发流程

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        插件开发完整流程                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  1. 初始化插件          2. 开发插件           3. 发布插件                │
│  ┌──────────────┐      ┌──────────────┐      ┌──────────────┐           │
│  │ 登录工作台    │  ──► │ 编写 task.json│ ──► │ 提交构建     │           │
│  │ 新增插件      │      │ 开发业务逻辑  │      │ 测试验证     │           │
│  │ 克隆代码库    │      │ 本地调试      │      │ 审核发布     │           │
│  └──────────────┘      └──────────────┘      └──────────────┘           │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

## 二、task.json 配置规范

### 2.1 整体结构

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

> **注意**: task.json 内容长度最大为 **64KB**

### 2.2 execution 执行配置

| 属性名 | 说明 | 格式 | 必填 |
|--------|------|------|------|
| `language` | 开发语言 | 字符串 | 是 |
| `target` | 执行入口命令 | 字符串 | 是 |
| `demands` | 执行前置依赖命令 | 数组 | 否 |
| `runtimeVersion` | 运行时版本 | 字符串 | 否 |
| `finishKillFlag` | 执行完成后是否杀进程 | 布尔 | 否 |
| `os` | 操作系统相关配置 | 数组 | 否 |

#### 2.2.1 runtimeVersion 配置值

| 语言 | 配置值 | 备注 |
|------|--------|------|
| python | `python2` | 默认 |
| python | `python3` | 推荐 |
| nodejs | `10.*` ~ `18.*` | 默认 10.* |
| java | `8` | 默认 |
| java | `17` | 新版本 |

#### 2.2.2 os 跨平台配置示例

```json
{
    "execution": {
        "language": "golang",
        "os": [
            {
                "osName": "linux",
                "osArch": "amd64",
                "target": "./app",
                "demands": [],
                "defaultFlag": true
            },
            {
                "osName": "windows",
                "osArch": "386",
                "target": "./app.exe",
                "demands": [],
                "defaultFlag": false
            },
            {
                "osName": "darwin",
                "osArch": "arm64",
                "target": "./app",
                "demands": [],
                "defaultFlag": false
            }
        ]
    }
}
```

### 2.3 input 输入字段配置

#### 2.3.1 支持的 UI 组件类型

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

#### 2.3.2 输入字段公共属性

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

#### 2.3.3 完整输入字段示例

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

### 2.4 output 输出字段配置

```json
{
    "output": {
        "buildResult": {
            "type": "string",
            "description": "构建结果",
            "isSensitive": false
        },
        "artifactPath": {
            "type": "artifact",
            "description": "构建产物路径"
        },
        "reportHtml": {
            "type": "report",
            "description": "测试报告"
        }
    }
}
```

**输出类型说明**:

| 类型 | 说明 | 限制 |
|------|------|------|
| `string` | 字符串变量 | 长度不超过 4KB |
| `artifact` | 构件文件 | 自动归档到仓库 |
| `report` | 报告文件 | 支持 HTML 渲染 |

### 2.5 config 插件控制特性

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

### 2.6 内置变量

| 变量名 | 说明 |
|--------|------|
| `{projectId}` | 项目英文 ID |
| `{pipelineId}` | 流水线 ID |
| `{buildId}` | 构建 ID |

## 三、多语言开发示例

### 3.1 Python 插件开发

#### 3.1.1 目录结构

```
my-python-atom/
├── task.json
├── setup.py
├── requirements.txt
└── demo/
    ├── __init__.py
    └── command_line.py
```

#### 3.1.2 task.json

```json
{
    "atomCode": "myPythonAtom",
    "execution": {
        "language": "python",
        "demands": [],
        "target": "demo"
    },
    "input": {
        "inputDemo": {
            "label": "输入示例",
            "type": "vuex-input",
            "required": true
        }
    },
    "output": {
        "outputDemo": {
            "type": "string",
            "description": "输出示例"
        }
    }
}
```

#### 3.1.3 setup.py

```python
from setuptools import setup, find_packages

setup(
    name="myPythonAtom",
    packages=find_packages(),
    install_requires=["python-atom-sdk"],
    entry_points={
        "console_scripts": [
            "demo = demo.command_line:main"
        ]
    }
)
```

#### 3.1.4 command_line.py

```python
# -*- coding: utf-8 -*-
import python_atom_sdk as sdk

def main():
    sdk.log.info("插件开始执行")
    
    # 获取输入参数
    input_params = sdk.get_input()
    input_demo = input_params.get("inputDemo", "")
    sdk.log.info(f"输入参数: {input_demo}")
    
    # 业务逻辑处理
    result = f"处理结果: {input_demo}"
    
    # 设置输出
    output_data = {
        "status": sdk.status.SUCCESS,
        "message": "执行成功",
        "errorCode": 0,
        "type": sdk.output_template_type.DEFAULT,
        "data": {
            "outputDemo": {
                "type": sdk.output_field_type.STRING,
                "value": result
            }
        }
    }
    sdk.set_output(output_data)
    
    sdk.log.info("插件执行完成")
    exit(0)

if __name__ == "__main__":
    main()
```

### 3.2 Java 插件开发

#### 3.2.1 目录结构

```
my-java-atom/
├── task.json
├── pom.xml
├── settings.xml
└── src/main/
    ├── java/com/example/atom/
    │   ├── AtomParam.java
    │   └── DemoAtom.java
    └── resources/META-INF/services/
        └── com.tencent.bk.devops.atom.spi.TaskAtom
```

#### 3.2.2 task.json

```json
{
    "atomCode": "myJavaAtom",
    "defaultLocaleLanguage": "zh_CN",
    "execution": {
        "language": "java",
        "minimumVersion": "1.8",
        "demands": [],
        "target": "java -jar myJavaAtom-jar-with-dependencies.jar"
    },
    "input": {
        "desc": {
            "label": "描述",
            "type": "vuex-input",
            "placeholder": "请输入描述信息",
            "required": true
        }
    },
    "output": {
        "testResult": {
            "type": "string",
            "description": "执行结果"
        }
    }
}
```

#### 3.2.3 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.tencent.bk.devops.atom</groupId>
        <artifactId>sdk-dependencies</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>myJavaAtom</artifactId>
    <version>1.0.0</version>

    <properties>
        <sdk.version>1.1.58</sdk.version>
        <java.version>1.8</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.tencent.bk.devops.atom</groupId>
            <artifactId>java-atom-sdk</artifactId>
            <version>${sdk.version}</version>
        </dependency>
    </dependencies>

    <build>
        <finalName>${project.name}</finalName>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-assembly-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

#### 3.2.4 AtomParam.java

```java
package com.example.atom;

import com.tencent.bk.devops.atom.pojo.AtomBaseParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AtomParam extends AtomBaseParam {
    private String desc;
}
```

#### 3.2.5 DemoAtom.java

```java
package com.example.atom;

import com.tencent.bk.devops.atom.AtomContext;
import com.tencent.bk.devops.atom.common.Status;
import com.tencent.bk.devops.atom.pojo.AtomResult;
import com.tencent.bk.devops.atom.pojo.StringData;
import com.tencent.bk.devops.atom.spi.AtomService;
import com.tencent.bk.devops.atom.spi.TaskAtom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AtomService(paramClass = AtomParam.class)
public class DemoAtom implements TaskAtom<AtomParam> {

    private static final Logger logger = LoggerFactory.getLogger(DemoAtom.class);

    @Override
    public void execute(AtomContext<AtomParam> atomContext) {
        // 获取参数
        AtomParam param = atomContext.getParam();
        AtomResult result = atomContext.getResult();
        
        logger.info("输入参数: {}", param.getDesc());
        
        // 参数校验
        if (param.getDesc() == null || param.getDesc().isEmpty()) {
            result.setStatus(Status.failure);
            result.setMessage("描述不能为空");
            return;
        }
        
        // 业务逻辑
        logger.groupStart("执行任务");
        String output = "处理结果: " + param.getDesc();
        logger.info(output);
        logger.groupEnd("执行任务");
        
        // 设置输出
        result.setStatus(Status.success);
        result.getData().put("testResult", new StringData(output));
    }
}
```

#### 3.2.6 SPI 配置文件

`src/main/resources/META-INF/services/com.tencent.bk.devops.atom.spi.TaskAtom`:
```
com.example.atom.DemoAtom
```

### 3.3 NodeJS 插件开发

#### 3.3.1 目录结构

```
my-nodejs-atom/
├── task.json
├── package.json
├── rollup.config.js
├── index.js
└── .gitignore
```

#### 3.3.2 task.json

```json
{
    "atomCode": "myNodejsAtom",
    "execution": {
        "language": "nodejs",
        "demands": [],
        "target": "node dist/bundle.js"
    },
    "input": {
        "inputDemo": {
            "label": "输入示例",
            "type": "vuex-input",
            "required": true
        }
    },
    "output": {
        "outputDemo": {
            "type": "string",
            "description": "输出示例"
        }
    }
}
```

#### 3.3.3 package.json

```json
{
    "name": "myNodejsAtom",
    "version": "1.0.0",
    "main": "./dist/bundle.js",
    "dependencies": {
        "@tencent/nodejs_atom_sdk": "^1.1.12"
    },
    "devDependencies": {
        "@babel/core": "^7.4.5",
        "@babel/preset-env": "^7.4.5",
        "rollup": "^1.16.2",
        "rollup-plugin-babel": "^4.3.3"
    }
}
```

#### 3.3.4 rollup.config.js

```javascript
import babel from 'rollup-plugin-babel'

export default {
    input: 'index.js',
    output: {
        file: 'dist/bundle.js',
        format: 'cjs'
    },
    plugins: [babel()]
}
```

#### 3.3.5 index.js

```javascript
import { 
    getInputParams,
    setOutput,
    BK_ATOM_STATUS,
    BK_OUTPUT_TEMPLATE_TYPE
} from '@tencent/nodejs_atom_sdk'

const params = getInputParams()
console.log('输入参数:', params)

const inputDemo = params.inputDemo || ''
const result = `处理结果: ${inputDemo}`

setOutput({
    "type": BK_OUTPUT_TEMPLATE_TYPE.DEFAULT,
    "status": BK_ATOM_STATUS.SUCCESS,
    "data": {
        "outputDemo": {
            "type": "string",
            "value": result
        }
    }
})
```

### 3.4 Golang 插件开发

#### 3.4.1 目录结构

```
my-golang-atom/
├── task.json
├── go.mod
├── go.sum
└── main.go
```

#### 3.4.2 task.json

```json
{
    "atomCode": "myGolangAtom",
    "execution": {
        "language": "golang",
        "demands": [],
        "target": "./app"
    },
    "input": {
        "greeting": {
            "label": "欢迎词",
            "type": "vuex-input",
            "default": "Hello",
            "required": true
        },
        "userName": {
            "label": "用户名",
            "type": "vuex-input",
            "required": true
        }
    },
    "output": {
        "result": {
            "type": "string",
            "description": "输出结果"
        }
    }
}
```

#### 3.4.3 go.mod

```go
module xxx/bkdevops/myGolangAtom

go 1.14

require xxx/bkdevops/golang-atom-sdk v1.1.9
```

#### 3.4.4 main.go

```go
package main

import (
    "fmt"
    sdk "xxx/bkdevops/golang-atom-sdk"
)

func main() {
    // 获取输入参数
    input := sdk.GetInput()
    greeting := input["greeting"]
    userName := input["userName"]
    
    fmt.Printf("输入参数: greeting=%s, userName=%s\n", greeting, userName)
    
    // 业务逻辑
    result := fmt.Sprintf("%s, %s!", greeting, userName)
    
    // 设置输出
    output := sdk.Output{
        Status:  sdk.StatusSuccess,
        Message: "执行成功",
        Type:    sdk.OutputTypeDefault,
        Data: map[string]sdk.DataField{
            "result": {
                Type:  "string",
                Value: result,
            },
        },
    }
    sdk.SetOutput(output)
}
```

## 四、插件输出规范

### 4.1 输出数据结构

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

### 4.2 status 状态值

| 值 | 说明 |
|----|------|
| `success` | 执行成功 |
| `failure` | 执行失败 |

### 4.3 输出类型详解

#### 4.3.1 string 类型

```json
{
    "outputVar": {
        "type": "string",
        "value": "输出字符串，长度不超过4KB"
    }
}
```

#### 4.3.2 artifact 类型（构件归档）

```json
{
    "buildArtifact": {
        "type": "artifact",
        "value": ["/workspace/output/app.apk", "/workspace/output/app.ipa"],
        "artifactoryType": "PIPELINE",
        "path": "/release/"
    }
}
```

| 属性 | 说明 |
|------|------|
| `value` | 文件绝对路径数组 |
| `artifactoryType` | `PIPELINE`（流水线仓库）或 `CUSTOM_DIR`（自定义仓库） |
| `path` | 自定义仓库时的相对路径 |

#### 4.3.3 report 类型（报告归档）

```json
{
    "testReport": {
        "type": "report",
        "reportType": "INTERNAL",
        "label": "单元测试报告",
        "path": "/workspace/test-report",
        "target": "index.html"
    }
}
```

| 属性 | 说明 |
|------|------|
| `reportType` | `INTERNAL`（内置报告）或 `THIRDPARTY`（第三方链接） |
| `label` | 报告别名 |
| `path` | 报告目录绝对路径 |
| `target` | 入口文件（相对于 path） |
| `url` | 第三方报告链接（reportType=THIRDPARTY 时） |

## 五、错误码规范

### 5.1 错误类型（errorType）

| 值 | 类型 | 说明 |
|----|------|------|
| 1 | USER | 用户配置错误（参数不合法等） |
| 2 | THIRD_PARTY | 第三方平台错误 |
| 3 | PLUGIN | 插件逻辑错误（默认） |

### 5.2 通用错误码（100 开头）

| 错误码 | 说明 | message 规范 |
|--------|------|--------------|
| 100001 | 输入参数非法 | 输入参数[xxx]非法：<具体要求> |
| 100002 | 网络连接超时 | 接口[xxx]，连接超时 |
| 100003 | 插件异常 | 未知的插件异常：<报错信息> |
| 100004 | 缺少必要参数 | 缺少必要参数xxx |
| 100400 | HTTP 400 | 接口[xxx]，返回码[400] |
| 100401 | HTTP 401 | 接口[xxx]，返回码[401] |
| 100403 | HTTP 403 | 接口[xxx]，返回码[403] |
| 100404 | HTTP 404 | 接口[xxx]，返回码[404] |
| 100500 | HTTP 500 | 接口[xxx]，返回码[500] |
| 100502 | HTTP 502 | 接口[xxx]，返回码[502] |

### 5.3 插件自定义错误码

- 统一以 **8** 开头
- 在插件代码库下增加 `error.json` 文件声明：

```json
[
    {
        "errorCode": 800001,
        "errorMsgZhCn": "配置文件不存在",
        "errorMsgEn": "Config file not found"
    },
    {
        "errorCode": 800002,
        "errorMsgZhCn": "编译失败",
        "errorMsgEn": "Build failed"
    }
]
```

### 5.4 第三方平台错误上报

当 `errorType=2` 时，需要填写：

```json
{
    "errorType": 2,
    "errorCode": 106001,
    "platformCode": "tgit",
    "platformErrorCode": 500,
    "message": "调用工蜂接口失败：/api/v4/projects，返回码[500]"
}
```

**已注册的第三方平台标识**:

| 平台 | 标识 | errorCode 前缀 |
|------|------|----------------|
| 蓝盾 | bkci | 101 |
| CodeCC | bkci_codecc | 102 |
| 制品库 | bkci_repo | 103 |
| 编译加速 | bkci_turbo | 104 |
| 作业平台 | bk_job | 105 |
| 工蜂 | tgit | 106 |
| TAPD | tapd | 107 |
| 智研 | zhiyan | 108 |
| 七彩石 | rainbow | 109 |
| 腾讯云 COS | cloud_cos | 110 |

## 六、插件发布流程

### 6.1 发布流程图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         插件发布流程                                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐          │
│  │ 提交发布  │ ─► │ 系统构建  │ ─► │ 测试验证  │ ─► │ 审核发布  │          │
│  │          │    │          │    │          │    │          │          │
│  │ 填写信息  │    │ 自动打包  │    │ 创建流水线 │    │ 首次需审核 │          │
│  │ 选择版本  │    │ 上传制品  │    │ 验证功能  │    │ 升级免审核 │          │
│  └──────────┘    └──────────┘    └──────────┘    └──────────┘          │
│       │               │               │               │                 │
│       ▼               ▼               ▼               ▼                 │
│    COMMITTING     BUILDING        TESTING         AUDITING              │
│                       │                               │                 │
│                       ▼                               ▼                 │
│                  BUILD_FAIL                     AUDIT_REJECT            │
│                                                       │                 │
│                                                       ▼                 │
│                                                   RELEASED              │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 6.2 发布类型

| 类型 | 说明 | 版本号变化 |
|------|------|------------|
| 新上架 | 首次发布 | 1.0.0 |
| 非兼容式升级 | 输入输出不兼容 | 主版本号 +1 |
| 兼容式功能更新 | 新增功能，兼容旧版 | 次版本号 +1 |
| 兼容式问题修正 | Bug 修复 | 修正号 +1 |
| 历史大版本 Bug fix | 修复历史版本 | 修正号 +1 |

### 6.3 版本号规范

遵循 [SemVer](https://semver.org/lang/zh-CN/) 规范：`主版本号.次版本号.修正号`

- **主版本号**：不兼容的 API 修改
- **次版本号**：向下兼容的功能性新增
- **修正号**：向下兼容的问题修正

## 七、本地调试指南

### 7.1 调试准备

在代码库根目录创建以下文件（**不要提交到代码库**）：

#### 7.1.1 input.json

```json
{
    "inputParam1": "测试值1",
    "inputParam2": "测试值2"
}
```

#### 7.1.2 .sdk.json

```json
{
    "buildType": "DOCKER",
    "projectId": "test-project",
    "agentId": "1",
    "secretKey": "test-secret",
    "gateway": "xxx.xxx.xxx.xxx",
    "buildId": "test-build-id",
    "vmSeqId": "1"
}
```

> **注意**: 本地调试时不能调用蓝盾后台服务（需要在流水线中执行才有权限）

### 7.2 Python 插件调试

```bash
# 安装 SDK
pip install python-atom-sdk

# 打包插件
python ./setup.py sdist

# 安装插件
pip install dist/XXX.tar.gz

# 在 input.json 所在目录执行入口命令
demo
```

### 7.3 Java 插件调试

```bash
# 打包
mvn clean package

# 运行
java -jar target/myJavaAtom-jar-with-dependencies.jar
```

### 7.4 NodeJS 插件调试

```bash
# 安装依赖
npm install

# 打包
rollup -c rollup.config.js

# 运行（需要先在根目录创建 mock 目录，将 input.json 放入）
node dist/bundle.js
```

### 7.5 Golang 插件调试

```bash
# 运行
go run main.go

# 或构建后运行
go build -o app main.go
./app
```

## 八、最佳实践

### 8.1 参数校验

```python
# Python 示例
def validate_params(input_params):
    required_fields = ["repositoryUrl", "branch"]
    for field in required_fields:
        if not input_params.get(field):
            return False, f"缺少必要参数: {field}"
    return True, ""
```

### 8.2 错误处理

```java
// Java 示例
try {
    // 业务逻辑
} catch (IOException e) {
    result.setErrorInfo(
        Status.failure,
        100002,  // 网络错误
        ErrorType.THIRD_PARTY,
        new String[]{"接口调用失败: " + e.getMessage()}
    );
}
```

### 8.3 日志输出

```python
# Python 示例 - 使用日志分组
sdk.log.info("开始执行任务")
sdk.log.group_start("编译阶段")
sdk.log.info("正在编译...")
sdk.log.group_end("编译阶段")
```

### 8.4 敏感信息处理

- 在 `task.json` 中将敏感字段设置 `isSensitive: true`
- 敏感信息不会在日志中明文显示
- 使用插件私有设置管理账号密码等敏感数据

## 九、常见问题

**Q: task.json 修改后流水线没有生效？**
A: 需要重新构建插件，并刷新流水线页面清除缓存。

**Q: 如何获取上一个插件的输出变量？**
A: 在参数类中定义同名参数即可自动接收。

**Q: 插件执行报错 "Plugin File Sha1 is wrong!"？**
A: 联系插件作者重新发布插件版本。

**Q: 如何让插件支持多操作系统？**
A: 在 `execution.os` 中配置不同操作系统的执行命令。

**Q: 插件输出变量长度限制？**
A: string 类型输出不能超过 4KB。

---

**版本**: 1.0.0 | **更新日期**: 2025-12-26
