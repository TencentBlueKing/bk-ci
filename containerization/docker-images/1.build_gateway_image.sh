#!/bin/bash
set -e
echo "######################## BUILD GATEWAY IMAGE START... ########################"
docker build -t mirrors.tencent.com/bkce/bkci/gateway:1.16.0 . -f ./dockerfile/gateway.Dockerfile
echo "######################## BUILD GATEWAY IMAGE FINISH ! ########################"
echo ''
