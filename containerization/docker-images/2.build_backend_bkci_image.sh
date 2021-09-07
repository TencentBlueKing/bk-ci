#!/bin/bash
set -e
# 排除掉前端和agent两个大文件夹,缩小体积
cat << EOF > ./.dockerignore
./ci/frontend
./ci/agent-package
EOF

echo "######################## BUILD BKCI BACKEND IMAGE START... ########################"
docker build -t mirrors.tencent.com/bkci/backend:0.0.1 . -f ./dockerfile/backend.bkci.Dockerfile
echo "######################## BUILD BKCI BACKEND IMAGE FINISH ! ########################"
echo ''
rm -f ./.dockerignore
