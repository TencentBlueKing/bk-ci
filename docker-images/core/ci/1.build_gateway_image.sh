#!/bin/bash
set -e
echo "######################## BUILD GATEWAY IMAGE START... ########################"
docker build -t $1/bkci-gateway:$2 . -f ./dockerfile/gateway.Dockerfile
docker push $1/bkci-gateway:$2 
echo "######################## BUILD GATEWAY IMAGE FINISH ! ########################"
echo ''
