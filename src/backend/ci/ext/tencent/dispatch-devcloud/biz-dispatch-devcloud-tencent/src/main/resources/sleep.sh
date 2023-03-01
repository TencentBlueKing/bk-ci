#!/bin/bash

http_proxy=devnet-proxy.oa.com:8080
https_proxy=devnet-proxy.oa.com:8080
ftp_proxy=devnet-proxy.oa.com:8080
no_proxy=10.10.10.0,tlinux-mirror.tencent-cloud.com,tlinux-mirrorlist.tencent-cloud.com,localhost,.oa.com,.local,10.240.99.238,10.12.216.232,10.168.134.78

export http_proxy https_proxy ftp_proxy no_proxy

echo "" >> /etc/profile
echo "export http_proxy=devnet-proxy.oa.com:8080" >> /etc/profile
echo "export https_proxy=devnet-proxy.oa.com:8080" >> /etc/profile
echo "ftp_proxy=devnet-proxy.oa.com:8080" >> /etc/profile
echo "no_proxy=10.10.10.0,tlinux-mirror.tencent-cloud.com,tlinux-mirrorlist.tencent-cloud.com,localhost,.oa.com,.local,10.240.99.238,10.12.216.232,10.168.134.78" >> /etc/profile
echo "" >> /etc/profile
echo "export http_proxy https_proxy ftp_proxy no_proxy" >> /etc/profile

while true
do
   sleep 5m
done
