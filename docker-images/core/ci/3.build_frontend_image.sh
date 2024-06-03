#!/bin/bash
set -e
echo "######################## BUILD FRONTEND IMAGE START... ########################"
docker build -t $1/bkci-frontend:$2 . -f ./dockerfile/frontend.Dockerfile
docker push $1/bkci-frontend:$2 
echo "######################## BUILD FRONTEND IMAGE FINISH ! ########################"
echo ''
