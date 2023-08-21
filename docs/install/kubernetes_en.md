# Kubernetes

Kubernetes (K8s) is an open-source system for automating deployment, scaling, and management of containerized applications.

Hosting BK-CI on a Kubernetes Cluster is beneficial for dynamic scalable BK-CI services. Here, we see a step-by-step process for setting up BK-CI on a Kubernetes Cluster.

## System
LINUX

## Requirements
1. [Kubernetes](https://kubernetes.io/) (Recommend for individual: [Minikube](https://minikube.sigs.k8s.io/docs/start/))
2. [Helm3](https://helm.sh/docs/intro/install/)

## Steps
1. Download the latest bk-ci-charts.tgz from [Releases](https://github.com/TencentBlueKing/bk-ci/releases)
2. Run `helm install <ReleaseName> bk-ci-charts.tgz`
    - If cluster doesn\`t have ingress-controller , you need add parameter `--set nginx-ingress-controller.enabled=true`
    - If cluster has ingress-controller , you should modify `ingress.annotations.kubernetes.io/ingress.class` according to controller type.
3. After install, you can access `devops.example.com` in Web Browner (Edit hosts file, set the host to ingress ip)
