#!/bin/bash
set -e
cp ../../../bkci-docker.tar.gz .
rm -rf ci-docker
tar -xzf bkci-docker.tar.gz
