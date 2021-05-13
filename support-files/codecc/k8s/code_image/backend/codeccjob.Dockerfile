FROM bkci/jdk

LABEL maintainer="blueking"

ENV module codeccjob

RUN pwd

COPY boot-codeccjob.jar /data/docker/bkci/codecc/backend/
COPY module_run_codecc.sh /data/docker/bkci/codecc/backend/
COPY classpath /data/docker/bkci/codecc/backend/classpath
COPY bootstrap /data/docker/bkci/codecc/backend/bootstrap

RUN chmod +x /data/docker/bkci/codecc/backend/module_run_codecc.sh