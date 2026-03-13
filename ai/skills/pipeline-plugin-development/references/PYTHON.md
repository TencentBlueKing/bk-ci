# Python 插件开发

## 目录结构

```
my-python-atom/
├── task.json
├── setup.py
├── requirements.txt
└── demo/
    ├── __init__.py
    └── command_line.py
```

## task.json

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

## setup.py

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

## command_line.py

```python
# -*- coding: utf-8 -*-
import python_atom_sdk as sdk

def main():
    sdk.log.info("插件开始执行")

    # 获取输入参数
    input_params = sdk.get_input()
    input_demo = input_params.get("inputDemo", "")
    sdk.log.info(f"输入参数: {input_demo}")

    # 参数校验
    if not input_demo:
        sdk.set_output({
            "status": sdk.status.FAILURE,
            "message": "缺少必要参数: inputDemo",
            "errorCode": 100004,
            "errorType": 1,
            "type": sdk.output_template_type.DEFAULT,
            "data": {}
        })
        exit(1)

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

## 本地调试

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

**验证检查点**:
1. 确认 `input.json` 和 `.sdk.json` 存在于执行目录
2. 执行后检查当前目录是否生成 `output.json`
3. 检查 `output.json` 中 `status` 字段为 `success`

## 最佳实践

```python
# 参数校验
def validate_params(input_params):
    required_fields = ["repositoryUrl", "branch"]
    for field in required_fields:
        if not input_params.get(field):
            return False, f"缺少必要参数: {field}"
    return True, ""

# 日志分组
sdk.log.info("开始执行任务")
sdk.log.group_start("编译阶段")
sdk.log.info("正在编译...")
sdk.log.group_end("编译阶段")
```
