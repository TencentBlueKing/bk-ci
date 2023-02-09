FROM bkci/jdk:0.0.2

LABEL maintainer="Tencent BlueKing Devops"

ENV LANG="en_US.UTF-8"

RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone && \
    yum update -y && \
    yum install mysql -y && \
    yum install -y epel-release &&\
    yum install -y python3 &&\
    pip3 install requests 

RUN wget "https://github.com/bkdevops-projects/devops-jre/raw/main/linux/jre.zip" -P /data/workspace/agent-package/jre/linux/ &&\
    wget "https://github.com/bkdevops-projects/devops-jre/raw/main/windows/jre.zip" -P /data/workspace/agent-package/jre/windows/ &&\
    wget "https://github.com/bkdevops-projects/devops-jre/raw/main/macos/jre.zip" -P /data/workspace/agent-package/jre/macos/ 

COPY ./ci-docker /data/workspace/
COPY ./dockerfile/backend.bkci.sh /data/workspace/

WORKDIR /data/workspace
