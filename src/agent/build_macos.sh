#!/bin/bash

WORK_DIR="$(cd `dirname $0`; pwd)"
export GOPATH=${WORK_DIR}

mkdir -p bin
rm -f bin/*

cd ${WORK_DIR}/src/cmd/agent
go build -o ${WORK_DIR}/bin/devopsAgent_macos

cd ${WORK_DIR}/src/cmd/daemon
go build -o ${WORK_DIR}/bin/devopsDaemon_macos

cd ${WORK_DIR}/src/cmd/upgrader
go build -o ${WORK_DIR}/bin/upgrader_macos

cd ${WORK_DIR}