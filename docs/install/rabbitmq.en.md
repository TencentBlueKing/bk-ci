# RabbitMQ Installation Document

## System Requirement

- RabbitMQ 3.7.15+

## Installation Guide

- Refer to this [article](https://www.vultr.com/docs/how-to-install-rabbitmq-on-centos-7).
- Since the workflow engine of bk-ci depends on the delayed-messaging of RabbitMQ, the delay-message-exchange plugin needs to be installed. Download the plugin from [here](https://dl.bintray.com/rabbitmq/community-plugins/3.7.x/rabbitmq_delayed_message_exchange), unzipping and copying it to `/usr/lib/rabbitmq/lib/rabbitmq_server-${VERSION}/plugins/`, and run `rabbitmq-plugins enable rabbitmq_delayed_message_exchange`.
- After the installation is complete, add vhost and user and set the user’s permission.
