FROM bkci/jdk:0.0.4

LABEL maintainer="Tencent BlueKing Devops"

# 复制所有服务的包
COPY ./ci-docker /data/workspace/
COPY ./dockerfile/backend.bkci.sh /data/workspace/
