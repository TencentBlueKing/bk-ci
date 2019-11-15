# 插件开发框架说明：

**一、插件代码工程的整体结构如下：**

```
|- demo                       # 插件包名
    |- demo                   # 插件包名
        |- __init__.py py     # py包标识
        |- command_line.py    # 命令入口文件
        |- python_atom_sdk.py # 插件开发SDK
    |- MANIFEST.in            # 包文件类型申明
    |- requirements.txt       # 依赖申明
    |- setup.py               # 执行包打包配置
```


**二、如何开发插件：**

参考插件代码工程的整体结构和demo示例：

- 创建插件代码工程
- 修改包名为有辨识度的名称，建议可以和插件标识一致
- 实现插件功能
- 规范：
    - [插件开发规范](../../docs/wiki/plugin_dev.md)
    - [插件产出规范](../../docs/wiki/plugin_output.md)


**三、如何打成插件市场要求的发布包：**

 1. 进入插件代码工程根目录下
 2. 执行 python setup.py sdist (或其他打包命令，本示例以sdist为例)
 3. 在任意位置新建文件夹，如 demo_release
 4. 将步骤 2 生产的执行包拷贝到 demo_release 下
 5. 添加task.json文件到 demo_release 下
    task.json 见示例，按照插件功能配置。
    - [插件配置规范](../../docs/wiki/plugin_config.md)
 6. 把 demo_release 打成zip包即可
