#!/bin/bash

# 处理根chart
echo "update root dependencies"
rm -f Chart.lock
helm dependency build
