#!/bin/bash
# 将ssh通过镜像编译并拆出编译后结果

# 打包ssh镜像
docker build -f Dockerfile -t temp/temp:1 .

# 保存镜像为本地文件
mkdir -p temp
docker save temp/temp:1 -o temp/ssh.tar
cd temp || exit

# 解压文件
tar xfv ssh.tar

# 解压其中的层文件
find . | grep layer.tar | while read f; do tar xfv "$f"; done

# 找到需要的ssh
cp usr/sbin/sshd ../sshd
cp usr/bin/ssh-keygen ../ssh-keygen

# 删除剩余的文件和镜像
cd ..
rm -rf temp
docker image rm temp/temp:1
