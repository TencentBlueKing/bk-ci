#!/bin/bash
set -e
cp ../../../bkci-slim.tar.gz .
rm -rf ci
tar -xzf bkci-slim.tar.gz
