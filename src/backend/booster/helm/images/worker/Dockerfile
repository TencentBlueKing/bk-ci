FROM mirrors.tencent.com/tlinux/tlinux2.4-minimal:latest

RUN yum install libmpc-devel -y && yum group install "Development Tools" -y && yum install procps -y

COPY * /data/bcss/bk-dist-worker/

RUN chmod a+x /data/bcss/bk-dist-worker/start.sh

CMD ["sh", "-c", "/data/bcss/bk-dist-worker/start.sh"]