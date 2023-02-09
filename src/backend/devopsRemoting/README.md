## DevopsRemoting
蓝盾远程开发容器相关部分

### 代码结构
- ide: 真实的被执行ide进程模块
- openssh: openssh实例以及获取脚本
- workspace-images: 基础镜像相关
- src: devopsRemoting以及相关模块
  - cmd 各个模块编译相关，包含cli工具命令设置
  - pkg 具体代码模块
    - remoting 远程Ide相关的管理进程
    - remoting remoting的api抽象层，方便其他模块引用
    - cli 命令行工具公共层
    - wsproxy 工作空间流量转发层
- public 一些公共网页文件，方便wsproxy引用
- scripts 各类脚本
- docker 存放模块docker构建相关
- manifests 存放模块kubernetes/helm相关    
- common 公共代码工具
- internal 修改部分内部go包后的代码