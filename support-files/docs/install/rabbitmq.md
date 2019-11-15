# rabbitMQ 安装文档

## 系统要求

- rabbitmq 3.7.15版本及以上版本

## 安装说明

- 参考这篇[文章](https://www.vultr.com/docs/how-to-install-rabbitmq-on-centos-7)

- 因为bkci流程引擎依赖rabbit的delay message，需要安装delay-message-exchange插件，从[这里](https://dl.bintray.com/rabbitmq/community-plugins/3.7.x/rabbitmq_delayed_message_exchange)下载解压并copy到`/usr/lib/rabbitmq/lib/rabbitmq_server-${VERSION}/plugins/`并执行`rabbitmq-plugins enable rabbitmq_delayed_message_exchange`

- 安装完成之添加vhost，user并设置其权限
