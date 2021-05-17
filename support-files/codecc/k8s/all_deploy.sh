#!/bin/bash

## init config
sh codecc/scripts/codecc/codecc_render_tpl -m codecc/k8s codecc/support-files/codecc/k8s/deploy_yaml/base/ingress_codecc.yaml
build_render_result=`sh codecc/scripts/codecc/codecc_render_tpl -m codecc/k8s codecc/support-files/codecc/k8s/code_image/build_codecc.sh`
install_render_result=`sh codecc/scripts/codecc/codecc_render_tpl -m codecc/k8s codecc/support-files/codecc/k8s/deploy_yaml/business/install_codecc.sh`
uninstall_render_result=`sh codecc/scripts/codecc/codecc_render_tpl -m codecc/k8s codecc/support-files/codecc/k8s/deploy_yaml/business/uninstall_codecc.sh`

build_command=`echo $build_render_result | awk '{print $4}'`
sh "$build_command"

uninstall_command=`echo $uninstall_render_result | awk '{print $4}'`
sh "$uninstall_command"

install_command=`echo $install_render_result | awk '{print $4}'`
sh "$install_command"

