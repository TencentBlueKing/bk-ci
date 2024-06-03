#!/bin/bash
set -e
# 排除掉前端文件夹,缩小体积
cat << EOF > ./.dockerignore
./ci/frontend
EOF

echo "######################## BUILD BKCI BACKEND IMAGE START... ########################"
docker build -t $1/bkci-backend:$2 . -f ./dockerfile/backend.bkci.Dockerfile
docker push $1/bkci-backend:$2
echo "######################## BUILD BKCI BACKEND IMAGE FINISH ! ########################"
echo ''
rm -f ./.dockerignore
