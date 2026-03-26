# Golang 插件开发

## 目录结构

```
my-golang-atom/
├── task.json
├── go.mod
├── go.sum
└── main.go
```

## task.json

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

## go.mod

```go
module xxx/bkdevops/myGolangAtom

go 1.14

require xxx/bkdevops/golang-atom-sdk v1.1.9
```

## main.go

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

    // 参数校验
    if greeting == "" || userName == "" {
        sdk.SetOutput(sdk.Output{
            Status:    sdk.StatusFailure,
            Message:   "缺少必要参数: greeting 或 userName",
            ErrorCode: 100004,
            ErrorType: 1,
            Type:      sdk.OutputTypeDefault,
            Data:      map[string]sdk.DataField{},
        })
        return
    }

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

## 跨平台构建配置

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

## 本地调试

```bash
# 运行
go run main.go

# 或构建后运行
go build -o app main.go
./app
```

**验证检查点**:
1. 确认 `go build` 无编译错误
2. 确认生成的二进制文件与 `task.json` 中 `target` 一致
3. 确认 `input.json` 和 `.sdk.json` 存在于执行目录
4. 执行后检查 `output.json` 中 `status` 为 `success`
