#!/bin/bash

# 先处理kubernetes-manager的chart
echo "update kubernetes-management dependencies"
cd local_chart/kubernetes-management
rm -f Chart.lock
helm dependency build
cd ../..

# 再处理根chart
echo "update root dependencies"
rm -f Chart.lock
helm dependency build
