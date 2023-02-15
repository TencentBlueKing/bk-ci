FROM bkci/os:0.0.1

LABEL maintainer="Tencent BlueKing Devops"

# 下载
RUN mkdir -p /data/tmp/installfiles &&\
    wget https://openresty.org/package/centos/7/x86_64/openresty-zlib-1.2.11-3.el7.centos.x86_64.rpm -P /data/tmp/installfiles &&\
    wget https://openresty.org/package/centos/7/x86_64/openresty-pcre-8.44-1.el7.x86_64.rpm -P /data/tmp/installfiles &&\
    wget https://openresty.org/package/centos/7/x86_64/openresty-openssl111-1.1.1g-3.el7.x86_64.rpm -P /data/tmp/installfiles &&\
    wget https://openresty.org/package/centos/7/x86_64/openresty-1.17.8.2-1.el7.x86_64.rpm -P /data/tmp/installfiles

# 安装
RUN chmod 775 /data/tmp/installfiles/*.rpm &&\
    rpm -ivh /data/tmp/installfiles/openresty-zlib-1.2.11-3.el7.centos.x86_64.rpm --replacefiles &&\
    rpm -ivh /data/tmp/installfiles/openresty-pcre-8.44-1.el7.x86_64.rpm --replacefiles &&\
    rpm -ivh /data/tmp/installfiles/openresty-openssl111-1.1.1g-3.el7.x86_64.rpm --replacefiles &&\
    rpm -ivh /data/tmp/installfiles/openresty-1.17.8.2-1.el7.x86_64.rpm --replacefiles
