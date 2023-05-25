ARG  BASE_IMAGE

FROM ${BASE_IMAGE}

COPY distcc/start.sh /data/start.sh

RUN chmod a+x /data/start.sh

CMD ["sh", "-c", "/data/start.sh"]