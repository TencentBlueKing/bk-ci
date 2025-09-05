FROM bkci/os:0.0.2

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
RUN dnf makecache &&\
    dnf install -y mysql &&\
    dnf install -y redis &&\
    dnf install -y python3 &&\
    dnf install -y pip3 &&\
    dnf install -y openssl &&\
    pip install requests

# 安装Java工具
RUN mkdir -p /data/tools && \
    curl -o /data/tools/arthas.jar https://arthas.aliyun.com/arthas-boot.jar  && \
    curl -o /data/tools/ot-agent.jar https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar -L

# 安装第三方构建机JRE
RUN wget "https://raw.githubusercontent.com/bkdevops-projects/devops-jre/refs/heads/jdk17/linux/jdk17.zip" -P /data/workspace/agent-package/jre/linux/ &&\
    wget "https://raw.githubusercontent.com/bkdevops-projects/devops-jre/refs/heads/jdk17/windows/jdk17.zip" -P /data/workspace/agent-package/jre/windows/ &&\
    wget "https://raw.githubusercontent.com/bkdevops-projects/devops-jre/refs/heads/jdk17/macos/jdk17.zip" -P /data/workspace/agent-package/jre/macos/ &&\
    wget "https://raw.githubusercontent.com/bkdevops-projects/devops-jre/refs/heads/jdk17/linux_arm64/jdk17.zip" -P /data/workspace/agent-package/jre/linux_arm64/ &&\
    wget "https://raw.githubusercontent.com/bkdevops-projects/devops-jre/refs/heads/jdk17/macos_arm64/jdk17.zip" -P /data/workspace/agent-package/jre/macos_arm64/
