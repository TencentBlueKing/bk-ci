FROM bkci/jdk

LABEL maintainer="blueking"

COPY import_config_codecc.sh /data/docker/bkci/codecc/configuration/
COPY codecc_render_tpl /data/docker/bkci/codecc/configuration/
COPY support-files /data/docker/bkci/codecc/configuration/support-files

COPY mongodb-org-4.0.repo /etc/yum.repos.d/mongodb-org-4.0.repo

RUN cat /etc/yum.repos.d/mongodb-org-4.0.repo

RUN yum clean all &&\
    yum update -y &&\
    yum install -y mysql &&\
    yum install -y mongodb-org-shell-4.0.21 &&\
    yum install -y mongodb-org-tools-4.0.21

RUN chmod +x /data/docker/bkci/codecc/configuration/import_config_codecc.sh \
    && chmod +x /data/docker/bkci/codecc/configuration/codecc_render_tpl