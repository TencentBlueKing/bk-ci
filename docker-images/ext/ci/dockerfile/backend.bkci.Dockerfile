FROM blueking/jdk:0.0.1

LABEL maintainer="Tencent BlueKing Devops"

ENV LANG="en_US.UTF-8"

RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone && \
    yum install mysql -y && \
    wget "https://bkrepo.woa.com/api/generic/bkdevops/static/jre/linux/jre.zip?download=true" -P /data/workspace/agent-package/jre/linux/ &&\
    wget "https://bkrepo.woa.com/api/generic/bkdevops/static/jre/windows/jre.zip?download=true" -P /data/workspace/agent-package/jre/windows/ &&\
    wget "https://bkrepo.woa.com/api/generic/bkdevops/static/jre/macos/jre.zip?download=true" -P /data/workspace/agent-package/jre/macos/ 

COPY ./ci /data/workspace/
COPY ./backend.bkci.sh /data/workspace/

WORKDIR /data/workspace
