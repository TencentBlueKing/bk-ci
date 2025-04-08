FROM fedora:42

LABEL maintainer="Tencent BlueKing Devops"

ENV LANG="en_US.UTF-8"

# 操作系统相关
RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime &&\
    echo 'alias ls="ls --color=auto"' >> ~/.bashrc &&\
    echo 'alias ll="ls -l"' >> ~/.bashrc &&\
    echo 'alias tailf="tail -f"' >> ~/.bashrc

# 安装软件
RUN dnf install -y procps && \
    dnf install -y vi && \
    dnf install -y vim && \
    dnf install -y less && \
    dnf install -y wget && \
    dnf install -y lrzsz
