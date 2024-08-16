## DevopsRemoting
蓝盾远程开发容器相关部分

### 代码结构(各模块集中在src下)
- common 公共代码工具
- ide: 真实的被执行ide进程模块
- remoting: 远程Ide相关的管理进程
  - openssh: openssh实例以及获取脚本
- remoting-api:  remoting的api抽象层，方便其他模块引用 
- wsproxy 工作空间流量转发层
  - public 一些公共网页文件，方便wsproxy引用
