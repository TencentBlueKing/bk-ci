#!/bin/bash
set -e
cp ../../../bkci-docker.tar.gz .
rm -rf ci
tar -xzf bkci-docker.tar.gz
