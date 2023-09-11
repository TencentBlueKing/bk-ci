#!/bin/bash

# 当有新的依赖加入时, 运行该脚本
# 扫描工程依赖的第三方开源组件License是否合规, 要追加License或豁免需要明确不是传染性协议,需要评审
# 根据需要修改 buildSrc/src/main/resources/allowed-licenses.json 豁免相应包
# 相应在build/reports/dependency-license下生成:
# 第三方包依赖License文档: THIRD-PARTY-NOTICES.txt
# 以及意外包含的协议:dependencies-without-allowed-license.json
# 最后将THIRD-PARTY-NOTICES.txt 复制到工程的根目录一并提交更新.

./gradlew checkLicense
