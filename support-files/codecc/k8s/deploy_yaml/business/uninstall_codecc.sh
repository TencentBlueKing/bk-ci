#!/bin/bash
namespace=__BK_CODECC_K8S_INSTALL_NS__

helm3 uninstall bkcodecc --namespace $namespace

while :
do
  kubectl get pv -n bkcodecc | grep 'nfs-artifactory'
  if [ $? == 0 ]
  then
    echo 'wait pv uninstall...'
    sleep 2s
  else
    echo 'pv has uninstall!'
    break;
  fi
done