#! /usr/bin/python3
import os
import re
import humps
import sys
import yaml
import tempfile

config_parent = '../../../support-files/templates/'
template_parent = './templates/configmap/tpl/'
env_properties_file = '../../../scripts/bkenv.properties'
output_value_yaml = './values.yaml'
default_env_path = './base/default_env.yaml'
default_value_yaml = './base/values.yaml'

# 创建目录
os.system("mkdir -p "+template_parent)

# 设置一些默认值
with open(default_env_path, 'r') as default_env:
    default_value_dict = yaml.safe_load(default_env)

# include 模板
include_dict = {
    'BK_CI_MYSQL_ADDR': '{{ include "bkci.mysqlAddr" . }}',
    'BK_CI_MYSQL_USER': '{{ include "bkci.mysqlUsername" . }}',
    'BK_CI_MYSQL_PASSWORD': '{{ include "bkci.mysqlPassword" . }}',
    'BK_CI_REDIS_HOST': '{{ if eq .Values.redis.enabled true }}{{ printf "%s.%s.%s" (include "bkci.redisHost" .) .Release.Namespace "svc.cluster.local" | quote}}{{ else }}{{ include "bkci.redisHost" . }}{{ end }}',
    'BK_CI_REDIS_PASSWORD': '{{ include "bkci.redisPassword" . }}',
    'BK_CI_REDIS_PORT': '{{ include "bkci.redisPort" . | quote }}',
    'BK_CI_ES_PASSWORD': '{{ include "bkci.elasticsearchPassword" . }}',
    'BK_CI_ES_REST_ADDR': '{{ include "bkci.elasticsearchHost" . }}',
    'BK_CI_ES_REST_PORT': '{{ include "bkci.elasticsearchPort" . | quote }}',
    'BK_CI_ES_USER': '{{ include "bkci.elasticsearchUsername" . }}',
    'BK_CI_RABBITMQ_ADDR': '{{ include "bkci.rabbitmqAddr" . }}',
    'BK_CI_RABBITMQ_PASSWORD': '{{ include "bkci.rabbitmqPassword" . }}',
    'BK_CI_RABBITMQ_USER': '{{ include "bkci.rabbitmqUser" . }}',
    'BK_CI_RABBITMQ_VHOST': '{{ include "bkci.rabbitmqVhost" . }}',
    'BK_CI_INFLUXDB_HOST': '{{ if eq .Values.influxdb.enabled true }}{{ printf "%s.%s.%s" (include "bkci.influxdbHost" .) .Release.Namespace "svc.cluster.local" | quote}}{{ else }}{{ include "bkci.influxdbHost" . }}{{ end }}',
    'BK_CI_INFLUXDB_PORT': '{{ include "bkci.influxdbPort" . | quote }}',
    'BK_CI_INFLUXDB_USER': '{{ include "bkci.influxdbUsername" . }}',
    'BK_CI_INFLUXDB_PASSWORD': '{{ include "bkci.influxdbPassword" . }}',
    'BK_CI_INFLUXDB_ADDR': 'http://{{ include "bkci.influxdbHost" . }}:{{ include "bkci.influxdbPort" . | quote }}',
    'BK_CI_VERSION': '{{ .Chart.AppVersion }}',
    'BK_CI_DISPATCH_KUBERNETES_NS': '{{ .Release.Namespace }}',
    'BK_CI_CONSUL_DISCOVERY_TAG': '{{ .Release.Namespace }}',
    'BK_CI_PRIVATE_URL': '{{ if empty .Values.config.bkCiPrivateUrl }}{{ .Release.Name }}-bk-ci-gateway{{ else }}{{ .Values.config.bkCiPrivateUrl }}{{ end }}'
}

# 大写风格转换为驼峰
replace_dict = {}
with open(env_properties_file, 'r') as env_file:
    value_re = re.compile(r'')
    for line in env_file:
        if line.startswith('BK_'):
            datas = line.split("=")
            key = datas[0]
            # 排除掉 include 相关值
            if include_dict.__contains__(key):
                continue
            replace_dict[key] = humps.camelize(key.lower())

# 生成value.yaml
image_registry = sys.argv[1]
image_gateway_tag = sys.argv[2]
image_backend_tag = sys.argv[3]
image_frontend_tag = sys.argv[4]
with open(output_value_yaml, 'w') as value_file:
    for line in open(default_value_yaml, 'r'):
        line = line.replace("__image_registry__", image_registry)
        line = line.replace("__image_gateway_tag__", image_gateway_tag)
        line = line.replace("__image_backend_tag__", image_backend_tag)
        line = line.replace("__image_frontend_tag__", image_frontend_tag)
        value_file.write(line)

    value_file.write('\nconfig:\n')
    for key in sorted(replace_dict):
        default_value = '""'
        if key.endswith("PORT"):
            default_value = '80'
        value = str(default_value_dict.get(replace_dict[key], default_value))
        value_file.write('  '+replace_dict[key]+': '+value+'\n')

# 匹配大写变量
replace_pattern = re.compile(r'__BK_[A-Z_]*__')

# 生成服务tpl
config_re = re.compile(r'-[a-z\-]*|common')
for config_name in os.listdir(config_parent):
    if "turbo" in config_name:
        continue
    if config_name.endswith('yaml') or config_name.endswith('yml'):
        with open(config_parent + config_name, 'r') as config_file:
            the_name = config_re.findall(config_name)[0].replace('-', '', 1)
            with tempfile.NamedTemporaryFile(mode="w+") as tmp:
                common_yaml = yaml.safe_load(config_file)
                yaml.dump(common_yaml, tmp)
                tmp.seek(0)
                with open(template_parent+'_'+the_name+'.tpl', 'w') as new_file:
                    new_file.write('{{- define "bkci.'+the_name+'.yaml" -}}\n')
                    for line in tmp:
                        for key in replace_pattern.findall(line):
                            upper_key = key[2:-2]
                            if include_dict.__contains__(upper_key):
                                line = line.replace(key, include_dict[upper_key])
                            else:
                                line = line.replace(key, '{{ .Values.config.'+replace_dict.get(upper_key, '')+' }}')
                        new_file.write(line)
                    new_file.write('{{ end }}')

# 生成网关的configmap
with open(template_parent+"/_gateway.tpl", "w") as gateway_config_file:
    gateway_config_file.write('{{- define "bkci.gateway.yaml" -}}\n')
    for env in replace_dict:
        gateway_config_file.write(env+": {{ .Values.config."+replace_dict.get(env, '')+" | quote }}\n")
    for key in include_dict:
        gateway_config_file.write(key+": "+include_dict[key]+"\n")
    gateway_config_file.write('NAMESPACE: {{ .Release.Namespace }}\n')
    gateway_config_file.write('CHART_NAME: {{ include "bkci.names.fullname" . }}\n')
    gateway_config_file.write('{{ end }}')
