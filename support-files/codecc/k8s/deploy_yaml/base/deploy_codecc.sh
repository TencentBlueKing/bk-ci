echo '脚本使用helm3语法'

set -e

# 配置ingress负载
echo 'kubectl apply -f ingress-codecc.yaml'
kubectl apply -n bkcodecc -f codecc_k8s/deploy_yaml/base/ingress_codecc.yaml

set +e

echo 'ingress安装完成'
echo '注: 配置外网反向代理到ingress(如nginx或CLB)...'