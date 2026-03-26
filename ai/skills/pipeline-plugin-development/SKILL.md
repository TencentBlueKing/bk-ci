---
name: pipeline-plugin-development
description: 流水线插件开发完整指南，涵盖插件创建、task.json 配置规范、多语言开发示例（Python/Java/NodeJS/Golang）、输入输出规范、错误码规范、发布流程、调试方法。当用户需要开发蓝盾流水线插件、配置 task.json、处理插件输入输出或排查插件错误时使用。
---

# 流水线插件开发指南

## 开发流程

1. **初始化**: 登录工作台 → 新增插件 → 克隆代码库
2. **开发**: 编写 `task.json` → 开发业务逻辑 → 本地调试
3. **发布**: 提交构建 → 测试验证 → 审核发布

## 支持的开发语言

| 语言 | SDK | 入口配置 (`execution.target`) | 运行时版本 |
|------|-----|-------------------------------|------------|
| **Python** | `python-atom-sdk` | `"demo"`（setup.py entry_points 名） | `python3`（推荐）/ `python2` |
| **Java** | `java-atom-sdk` | `"java -jar name-jar-with-dependencies.jar"` | `8`（默认）/ `17` |
| **NodeJS** | `@tencent/nodejs_atom_sdk` | `"node dist/bundle.js"` | `10.*` ~ `18.*` |
| **Golang** | `golang-atom-sdk` | `"./app"` | — |

语言完整开发示例: [Python](references/PYTHON.md) | [Java](references/JAVA.md) | [NodeJS](references/NODEJS.md) | [Golang](references/GOLANG.md)

## task.json 快速参考

> 完整字段说明见 [task.json 配置参考](references/TASK_JSON_REFERENCE.md)

```json
{
    "atomCode": "myAtom",
    "execution": {
        "language": "python",
        "demands": [],
        "target": "demo"
    },
    "input": {
        "repositoryUrl": {
            "label": "代码库地址",
            "type": "vuex-input",
            "required": true,
            "rule": { "regex": "^(https?|git)://" }
        }
    },
    "output": {
        "buildResult": {
            "type": "string",
            "description": "构建结果"
        }
    }
}
```

**验证检查点**: task.json 写完后用 JSON 校验器确认语法正确，确认 `atomCode` 与工作台注册一致，确认 `execution.target` 与实际入口匹配。

## 插件开发模式（Python 示例）

```python
# -*- coding: utf-8 -*-
import python_atom_sdk as sdk

def main():
    # 1. 获取输入
    input_params = sdk.get_input()
    repo_url = input_params.get("repositoryUrl", "")

    # 2. 参数校验 — 失败时设置 errorType + errorCode
    if not repo_url:
        sdk.set_output({
            "status": sdk.status.FAILURE,
            "message": "缺少必要参数: repositoryUrl",
            "errorCode": 100004,
            "errorType": 1,
            "type": sdk.output_template_type.DEFAULT,
            "data": {}
        })
        exit(1)

    # 3. 业务逻辑
    sdk.log.group_start("克隆代码库")
    result = f"已克隆: {repo_url}"
    sdk.log.info(result)
    sdk.log.group_end("克隆代码库")

    # 4. 设置输出
    sdk.set_output({
        "status": sdk.status.SUCCESS,
        "message": "执行成功",
        "errorCode": 0,
        "type": sdk.output_template_type.DEFAULT,
        "data": {
            "buildResult": {
                "type": sdk.output_field_type.STRING,
                "value": result
            }
        }
    })
    exit(0)

if __name__ == "__main__":
    main()
```

**验证检查点**: 执行后检查当前目录是否生成 `output.json`，确认 `status` 为 `success`，确认 `data` 中输出字段与 task.json `output` 定义一致。

## 输出规范

| 类型 | 说明 | 限制 |
|------|------|------|
| `string` | 字符串变量 | 长度不超过 4KB |
| `artifact` | 构件归档 | 需指定 `artifactoryType`（`PIPELINE` 或 `CUSTOM_DIR`） |
| `report` | 报告归档 | 需指定 `reportType`（`INTERNAL` 或 `THIRDPARTY`）+ `target` 入口文件 |

状态值: `success` 或 `failure`

## 错误码

> 完整错误码表见 [错误码规范](references/ERROR_CODES.md)

- **errorType 1** (USER): 用户配置错误 → errorCode `100xxx`
- **errorType 2** (THIRD_PARTY): 第三方平台错误 → errorCode 按平台前缀（101-110）
- **errorType 3** (PLUGIN): 插件逻辑错误（默认）
- **自定义错误码**: 以 `8` 开头，在 `error.json` 中声明

## 发布流程

1. **COMMITTING** → 填写发布信息，选择版本类型
2. **BUILDING** → 系统自动打包（失败 → BUILD_FAIL，检查构建日志）
3. **TESTING** → 创建测试流水线验证功能
4. **AUDITING** → 首次上架需审核，升级免审核（拒绝 → AUDIT_REJECT，修改后重新提交）
5. **RELEASED**

**版本号规范**（SemVer）:
- 非兼容式升级: 主版本号 +1
- 兼容式功能更新: 次版本号 +1
- 兼容式问题修正: 修正号 +1

**验证检查点**: 发布前确认 task.json 中 `input`/`output` 变更是否向后兼容，不兼容则选择"非兼容式升级"。

## 本地调试

在代码库根目录创建（**不要提交到代码库**）：

**input.json** — 模拟输入参数:
```json
{
    "repositoryUrl": "https://git.example.com/repo.git",
    "branch": "master"
}
```

**.sdk.json** — 模拟 SDK 环境:
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

> 本地调试时不能调用蓝盾后台服务（需要在流水线中执行才有权限）

**验证检查点**: 执行后确认 `output.json` 已生成且 `status` 为 `success`；如果调用了蓝盾后台 API 会报权限错误，属于预期行为。

## 敏感信息处理

- 在 `task.json` 中对敏感字段设置 `"isSensitive": true`
- 敏感信息不会在日志中明文显示
- 使用插件私有设置管理账号密码等敏感数据

## 常见问题

| 问题 | 解决方案 |
|------|----------|
| task.json 修改后流水线没有生效 | 重新构建插件，刷新流水线页面清除缓存 |
| 如何获取上一个插件的输出变量 | 在参数类中定义同名参数即可自动接收 |
| 报错 "Plugin File Sha1 is wrong!" | 联系插件作者重新发布插件版本 |
| 插件支持多操作系统 | 在 `execution.os` 中配置不同操作系统的执行命令，见 [Golang 跨平台示例](references/GOLANG.md#跨平台构建配置) |
| 插件输出变量长度限制 | string 类型输出不超过 4KB |
