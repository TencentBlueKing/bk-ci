#!/bin/bash

# 先处理kubernetes-manager的chart
cd local_chart/kubernetes-management
rm -f Chart.lock
helm dependency build
cd ../..

# 再处理根chart
helm dependency build
