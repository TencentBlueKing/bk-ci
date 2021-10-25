#TODO : 后续可以维护统一个openresty
FROM blueking/jdk:0.0.1

LABEL maintainer="Tencent BlueKing Devops"

RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone && \
    yum install mysql -y && \
    wget "https://github.com/stubenhuang/jre/raw/main/linux/jre.zip" -P /data/workspace/agent-package/jre/linux/ &&\
    wget "https://github.com/stubenhuang/jre/raw/main/windows/jre.zip" -P /data/workspace/agent-package/jre/windows/ &&\
    wget "https://github.com/stubenhuang/jre/raw/main/macos/jre.zip" -P /data/workspace/agent-package/jre/macos/ &&\
    export LANG="en_US.UTF-8"

COPY ./ci /data/workspace/
COPY ./dockerfile/backend.bkci.sh /data/workspace/

WORKDIR /data/workspace
