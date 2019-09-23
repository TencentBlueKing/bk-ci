# 6. 初始化及配置模版(support-files)

```
|- bk-ci
  |- support-files
    |- agent-package  # 初始化时需要分发到部署主目录下，用于存放agent安装包供下载
    |- file           # 初始化时需要分发到application-artifactory.yml指定的目录下
    |- sql            # sql初始化脚本，在开发编译时就需要先初始化，否则JOOQ无法正常生成PO
    |- template       # 模板
```

## 变量占位符

占位符号以双下划线开头，以又下划线结尾。
比如配置占位符为__BKCI_PORT__  变量名为BKCI_PORT

### 如何使用

1. 在相应模块配置文件下定义好变量
2. 在bk-ci/scripts/bkenv.properties中修改增加对该变量的配置
3. bk-ci/scripts/render_tpl -m bkci bk-ci/support-files/templates/* 
    
   - 这个命令将会：
     - 读取bkenv.properties中值，替换掉support-files/templates/下所有文件中的占变量位符号
     - 按support-files/templates/目录下文件名，改名并移动到相对应的部署目录下
     - 比如 #etc#bkci#common.yml 会移到 /data/bkee/etc/bkci/common.yml



