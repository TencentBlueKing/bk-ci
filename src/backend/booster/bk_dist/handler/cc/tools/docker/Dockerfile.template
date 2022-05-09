FROM __BASE_IMAGE__

COPY ./bk-dist-worker/* /data/bcss/bk-dist-worker/

RUN chmod a+x /data/bcss/bk-dist-worker/start.sh

CMD ["sh", "-c", "/data/bcss/bk-dist-worker/start.sh"]