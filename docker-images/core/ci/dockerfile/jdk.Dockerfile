FROM bkci/os:0.0.1

LABEL maintainer="Tencent BlueKing Devops"

## 安装JDK
RUN mkdir -p /data && \
    cd /data/ &&\
    curl -L https://github.com/Tencent/TencentKona-17/releases/download/TencentKona-17.0.8/TencentKona-17.0.8.b1-jdk_linux-x86_64.tar.gz -o kona.tar.gz &&\
    tar -xzf kona.tar.gz &&\
    rm -f kona.tar.gz
ENV JAVA_HOME=/data/TencentKona-17.0.8.b1
ENV PATH=${JAVA_HOME}/bin:$PATH
ENV CLASSPATH=.:${JAVA_HOME}/lib

# 安装软件
RUN yum update -y &&\
    yum install -y epel-release &&\
    yum install -y mysql &&\
    yum install -y redis &&\
    yum install -y python3 &&\
    pip3 install requests 

# 安装Java工具
RUN mkdir -p /data/tools && \
    curl -o /data/tools/arthas.jar https://arthas.aliyun.com/arthas-boot.jar  && \
    curl -o /data/tools/ot-agent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar -L

# 安装第三方构建机JRE
RUN wget "https://github.com/bkdevops-projects/devops-jre/raw/main/linux/jre.zip" -P /data/workspace/agent-package/jre/linux/ &&\
    wget "https://github.com/bkdevops-projects/devops-jre/raw/main/windows/jre.zip" -P /data/workspace/agent-package/jre/windows/ &&\
    wget "https://github.com/bkdevops-projects/devops-jre/raw/main/macos/jre.zip" -P /data/workspace/agent-package/jre/macos/ &&\
    wget "https://github.com/bkdevops-projects/devops-jre/raw/main/linux_arm64/jre.zip" -P /data/workspace/agent-package/jre/linux_arm64/ &&\
    wget "https://github.com/bkdevops-projects/devops-jre/raw/main/macos_arm64/jre.zip" -P /data/workspace/agent-package/jre/macos_arm64/ 
