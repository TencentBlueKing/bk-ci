#! /usr/bin/python3
import os
import re
import humps
import sys
import yaml
import tempfile

config_parent = '../../../support-files/templates/'
template_parent = './templates/configmap/tpl/'
frontend_path = '../../../src/frontend/'
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
    'bkCiMysqlAddr': '{{ include "bkci.mysqlAddr" . }}',
    'bkCiMysqlUser': '{{ include "bkci.mysqlUsername" . }}',
    'bkCiMysqlPassword': '{{ include "bkci.mysqlPassword" . }}',
    'bkCiRedisHost': '{{ if eq .Values.redis.enabled true }}{{ printf "%s.%s.%s" (include "bkci.redisHost" .) .Release.Namespace "svc.cluster.local" | quote}}{{ else }}{{ include "bkci.redisHost" . }}{{ end }}',
    'bkCiRedisPassword': '{{ include "bkci.redisPassword" . }}',
    'bkCiRedisPort': '{{ include "bkci.redisPort" . | quote }}',
    'bkCiEsPassword': '{{ include "bkci.elasticsearchPassword" . }}',
    'bkCiEsRestAddr': '{{ include "bkci.elasticsearchHost" . }}',
    'bkCiEsRestPort': '{{ include "bkci.elasticsearchPort" . | quote }}',
    'bkCiEsUser': '{{ include "bkci.elasticsearchUsername" . }}',
    'bkCiRabbitmqAddr': '{{ include "bkci.rabbitmqAddr" . }}',
    'bkCiRabbitmqPassword': '{{ include "bkci.rabbitmqPassword" . }}',
    'bkCiRabbitmqUser': '{{ include "bkci.rabbitmqUser" . }}',
    'bkCiRabbitmqVhost': '{{ include "bkci.rabbitmqVhost" . }}',
    'bkCiInfluxdbHost': '{{ if eq .Values.influxdb.enabled true }}{{ printf "%s.%s.%s" (include "bkci.influxdbHost" .) .Release.Namespace "svc.cluster.local" | quote}}{{ else }}{{ include "bkci.influxdbHost" . }}{{ end }}',
    'bkCiInfluxdbPort': '{{ include "bkci.influxdbPort" . | quote }}',
    'bkCiInfluxdbUser': '{{ include "bkci.influxdbUsername" . }}',
    'bkCiInfluxdbPassword': '{{ include "bkci.influxdbPassword" . }}',
    'bkCiInfluxdbAddr': 'http://{{ include "bkci.influxdbHost" . }}:{{ include "bkci.influxdbPort" . }}',
    'bkCiVersion': '{{ .Chart.AppVersion }}',
    'bkCiDispatchKubernetesNs': '{{ .Release.Namespace }}',
    'bkCiConsulDiscoveryTag': '{{ .Release.Namespace }}',
    'bkCiPrivateUrl': '{{ if empty .Values.config.bkCiPrivateUrl }}{{ .Release.Name }}-bk-ci-gateway{{ else }}{{ .Values.config.bkCiPrivateUrl }}{{ end }}'
}

# 正则匹配 __BK_XXX__
replace_pattern = re.compile(r'__BK_[A-Z_]*__')

# 驼峰名称集合 (不包括 include 模板)
camelize_set = set([])

# 生成服务tpl
print("generate service tpl...")
config_re = re.compile(r'-[a-z\-]*|common')
for config_name in os.listdir(config_parent):
    if "turbo" in config_name:
        continue
    if config_name.endswith('yaml') or config_name.endswith('yml'):
        with open(os.path.join(config_parent, config_name), 'r') as config_file:
            the_name = config_re.findall(config_name)[0].replace('-', '', 1)
            print("    processing service: "+the_name)
            with tempfile.NamedTemporaryFile(mode="w+") as tmp:
                common_yaml = yaml.safe_load(config_file)
                yaml.dump(common_yaml, tmp)  # 格式化yaml , 使得 configmap 美观
                tmp.seek(0)
                with open(template_parent+'_'+the_name+'.tpl', 'w') as new_file:
                    new_file.write('{{- define "bkci.'+the_name+'.yaml" -}}\n')
                    for line in tmp:
                        for key in replace_pattern.findall(line):
                            camelize_key = humps.camelize(key[2:-2].lower())
                            if camelize_key in include_dict:
                                line = line.replace(key, include_dict[camelize_key])
                            else:
                                camelize_set.add(camelize_key)
                                if line.replace(key, "").strip().endswith(":"):
                                    line = line.replace(key, '{{ .Values.config.'+camelize_key+' | quote }}')
                                else:
                                    line = line.replace(key, '{{ .Values.config.'+camelize_key+' }}')
                        new_file.write(line)
                    new_file.write('{{ end }}')

# 生成网关的configmap
print("generate gateway tpl...")
with open(template_parent+"/_gateway.tpl", "w") as gateway_config_file:
    gateway_config_file.write('{{- define "bkci.gateway.yaml" -}}\n')

    gateway_done_set = set([])
    # 网关模板
    for conf_name in os.listdir(config_parent):
        if conf_name.startswith('gateway'):
            print("    processing gateway: "+conf_name)
            with open(os.path.join(config_parent, conf_name), 'r') as conf_file:
                for line in conf_file:
                    for key in replace_pattern.findall(line):
                        env = key[2:-2]
                        camelize_key = humps.camelize(env.lower())
                        if camelize_key in gateway_done_set:
                            continue
                        gateway_done_set.add(camelize_key)
                        if camelize_key in include_dict:
                            gateway_config_file.write(env+": "+include_dict[camelize_key]+"\n")
                        else:
                            camelize_set.add(camelize_key)
                            gateway_config_file.write(env+": "+'{{ .Values.config.'+camelize_key+" | quote }}\n")
    # 前端文件
    for root, dirs, files in os.walk(frontend_path):
        for frontend_file in files:
            file_path = os.path.join(root, frontend_file)
            if (file_path.endswith("html") or file_path.endswith("js")) and "node_modules" not in file_path:
                with open(file_path, 'r') as f:
                    for line in f:
                        for key in replace_pattern.findall(line):
                            print("    processing frontend: "+file_path + " , key: "+key)
                            env = key[2:-2]
                            camelize_key = humps.camelize(env.lower())
                            if camelize_key in gateway_done_set:
                                continue
                            gateway_done_set.add(camelize_key)
                            if camelize_key in include_dict:
                                gateway_config_file.write(env+": "+include_dict[camelize_key]+"\n")
                            else:
                                camelize_set.add(camelize_key)
                                gateway_config_file.write(env+": "+'{{ .Values.config.'+camelize_key+" | quote }}\n")
    gateway_config_file.write('NAMESPACE: {{ .Release.Namespace }}\n')
    gateway_config_file.write('CHART_NAME: {{ include "bkci.names.fullname" . }}\n')
    gateway_config_file.write('{{ end }}')


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
    for camelize in sorted(camelize_set):
        default_value = '""'
        if camelize.lower().endswith("port"):
            default_value = '80'
        value = str(default_value_dict.get(camelize, default_value))
        value_file.write('  '+camelize+': '+value+'\n')
