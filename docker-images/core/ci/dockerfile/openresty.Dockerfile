FROM bkci/os:0.0.1

LABEL maintainer="Tencent BlueKing Devops"

ENV INSTALL_PATH="/data/tmp/installfiles"

# 下载
RUN mkdir -p ${INSTALL_PATH} &&\
    wget https://openresty.org/package/centos/7/x86_64/openresty-zlib-1.2.11-3.el7.centos.x86_64.rpm -P ${INSTALL_PATH} &&\
    wget https://openresty.org/package/centos/7/x86_64/openresty-pcre-8.44-1.el7.x86_64.rpm -P ${INSTALL_PATH} &&\
    wget https://openresty.org/package/centos/7/x86_64/openresty-openssl111-1.1.1g-3.el7.x86_64.rpm -P ${INSTALL_PATH} &&\
    wget https://openresty.org/package/centos/7/x86_64/openresty-1.17.8.2-1.el7.x86_64.rpm -P ${INSTALL_PATH}

# 安装
RUN chmod 775 ${INSTALL_PATH}/*.rpm &&\
    rpm -ivh ${INSTALL_PATH}/openresty-zlib-1.2.11-3.el7.centos.x86_64.rpm --replacefiles &&\
    rpm -ivh ${INSTALL_PATH}/openresty-pcre-8.44-1.el7.x86_64.rpm --replacefiles &&\
    rpm -ivh ${INSTALL_PATH}/openresty-openssl111-1.1.1g-3.el7.x86_64.rpm --replacefiles &&\
    rpm -ivh ${INSTALL_PATH}/openresty-1.17.8.2-1.el7.x86_64.rpm --replacefiles

# 删除文件
RUN rm -rf ${INSTALL_PATH}
