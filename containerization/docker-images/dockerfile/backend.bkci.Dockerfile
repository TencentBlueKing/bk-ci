#TODO : 后续可以维护统一个openresty
FROM blueking/jdk:0.0.1

LABEL maintainer="Tencent BlueKing Devops"

RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone && \
    yum install mysql -y

COPY ./ci /data/workspace/
COPY ./dockerfile/backend.bkci.sh /data/workspace/

WORKDIR /data/workspace
