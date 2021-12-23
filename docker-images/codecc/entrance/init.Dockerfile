FROM mirrors.tencent.com/ruitaoyuan/mongo-shell:0.0.1

LABEL maintainer="blueking"

COPY ./ /data/workspace/

RUN chmod +x /data/workspace/init-entrance.sh
WORKDIR /data/workspace