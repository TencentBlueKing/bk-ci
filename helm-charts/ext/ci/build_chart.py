import os
import re
import subprocess
import sys

import yaml

output_value_yaml = './values.yaml'
default_value_yaml = './base/values.yaml'
configmap_template_parent = './templates/configmap/tpl/'

# 用于 chart_version / version：只允许字母、数字、点、下划线、短横线、加号
SAFE_TOKEN_RE = re.compile(r'^[A-Za-z0-9._+\-]+$')
# 用于 config_path：允许常规目录字符
SAFE_PATH_RE = re.compile(r'^[A-Za-z0-9._/\-]+$')
# 用于服务名/文件名：避免路径穿越（不含 . / \）和注入到 helm 模板
SAFE_NAME_RE = re.compile(r'^[a-zA-Z0-9_\-]+$')


def require_env(name, pattern):
    """读取环境变量并做白名单校验，避免被用于命令/路径/模板注入。"""
    value = os.environ.get(name)
    if not value:
        sys.exit("environment variable '{}' is required".format(name))
    if not pattern.match(value):
        sys.exit("environment variable '{}' contains invalid characters".format(name))
    return value


def safe_join(base, *parts):
    """在 base 下安全地拼接子路径，拒绝逃逸到 base 之外的路径穿越。"""
    base_real = os.path.realpath(base)
    target_real = os.path.realpath(os.path.join(base_real, *parts))
    if target_real != base_real and not target_real.startswith(base_real + os.sep):
        sys.exit("unsafe path detected: {}".format(target_real))
    return target_real


def write_application_tpl(src_dir, tpl_file):
    """读取 src_dir 下所有 application*.yaml 并以规范化形式写入 tpl_file。"""
    files_list = sorted(os.listdir(src_dir), reverse=True)
    for file_name in files_list:
        if not file_name.startswith("application"):
            continue
        if not SAFE_NAME_RE.match(file_name.split('.')[0]):
            print("skip unsafe file name: " + file_name)
            continue
        file_path = safe_join(src_dir, file_name)
        if not os.path.isfile(file_path):
            continue
        print("read : " + file_path)
        with open(file_path, 'r') as f:
            common_yaml = yaml.safe_load(f)
        yaml.dump(common_yaml, tpl_file)
        tpl_file.write('---\n')


config_path = os.path.realpath(require_env("config_path", SAFE_PATH_RE))
if not os.path.isdir(config_path):
    sys.exit("config_path does not exist or is not a directory: " + config_path)

chart_version = require_env("chart_version", SAFE_TOKEN_RE)
version = require_env("version", SAFE_TOKEN_RE)

# 生成 value.yaml
image_gateway_tag = '0.0'
image_backend_tag = '0.0'
with open(default_value_yaml, 'r') as src, open(output_value_yaml, 'w') as value_file:
    for line in src:
        line = line.replace("__image_gateway_tag__", image_gateway_tag)
        line = line.replace("__image_backend_tag__", image_backend_tag)
        value_file.write(line)

config_server = safe_join(config_path, "config-server")
if not os.path.isdir(config_server):
    sys.exit("config-server directory not found under config_path: " + config_server)

# 生成 common tpl
common_tpl_path = safe_join(configmap_template_parent, '_common.tpl')
with open(common_tpl_path, 'w') as common_tpl:
    common_tpl.write('{{- define "bkci.common.yaml" -}}')
    write_application_tpl(config_server, common_tpl)
    common_tpl.write('{{- end -}}')

# service config
for service_name in os.listdir(config_server):
    if not SAFE_NAME_RE.match(service_name):
        print("skip unsafe service name: " + service_name)
        continue
    service_path = safe_join(config_server, service_name)
    if not os.path.isdir(service_path):
        continue
    service_tpl_path = safe_join(configmap_template_parent, '_' + service_name + '.tpl')
    with open(service_tpl_path, 'w') as service_tpl:
        service_tpl.write('{{- define "bkci.' + service_name + '.yaml" -}}\n')
        write_application_tpl(service_path, service_tpl)
        service_tpl.write('{{- end -}}')


# gateway config(伪装)
gateway_tpl_path = safe_join(configmap_template_parent, '_gateway.tpl')
with open(gateway_tpl_path, 'w') as gateway_tpl:
    gateway_tpl.write('{{- define "bkci.gateway.yaml" -}}\n')
    gateway_tpl.write('{{- end -}}')

# 打包：使用 subprocess 列表形式调用，避免走 shell，杜绝命令注入
subprocess.run(
    ["helm", "package", ".", "--version", chart_version, "--app-version", version],
    check=True,
)
