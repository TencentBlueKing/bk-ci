#!/bin/bash
# 使用helm安装

hub=__BK_CODECC_K8S_HUB__
nfs_server=__BK_CODECC_K8S_NFS_SERVER__
initConfig=__BK_CODECC_K8S_INIT_CONFIG__
namespace=__BK_CODECC_K8S_INSTALL_NS__

kubectl create ns bkcodecc

if $initConfig; then
  # echo 'unintall bk codecc with initConfig'
  # helm3 uninstall bkcodecc -n bkcodecc

  echo 'install bk codecc with initConfig'
  helm3 install bkcodecc codecc_k8s/deploy_yaml/business/bkcodecc --set image.hub=$hub,volume.nfs.server=$nfs_server \
  --namespace $namespace -f codecc_k8s/deploy_yaml/business/codecc_values.yaml
else
  # echo 'unintall bk codecc'
  # helm3 uninstall bkcodecc -n bkcodecc

  echo 'install bk codecc'
  helm3 install bkcodecc codecc_k8s/deploy_yaml/business/bkcodecc --set image.hub=$hub,volume.nfs.server=$nfs_server \
  --no-hooks --namespace $namespace -f codecc_k8s/deploy_yaml/business/codecc_values.yaml
fi

sleep 3s
echo "kubectl get job -n 'bkcodecc'"
kubectl get job -n 'bkcodecc'

echo "kubectl get pod -n 'bkcodecc'"
kubectl get pod -n 'bkcodecc'