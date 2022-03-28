FROM nginx

LABEL maintainer="Tencent BlueKing Devops"

COPY nginx.conf /etc/nginx/
COPY scripts/init.sh /root/
COPY scripts/start.sh /root/

RUN mkdir -p /data/clients && chmod +x /root/*.sh && echo "sh /root/init.sh" >> /root/.profile

COPY linux-turbo-client.tgz /data/clients/
COPY scripts/linux_client_download_install.sh /data/clients/install.sh

CMD /root/start.sh