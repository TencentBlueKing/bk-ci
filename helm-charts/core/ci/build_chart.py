#! /usr/bin/python3
import os
import re
import humps
import json
import sys

files = os.listdir('.')
replace_pattern = re.compile(r'__BK_[A-Z_]*__')
replace_dict = {}
config_parent = '../../../support-files/templates/'
template_parent = './templates/configmap/tpl/'
env_properties_file = '../../../scripts/bkenv.properties'
output_value_yaml = './values.yaml'
default_value_json = './build/values.json'
default_value_yaml = './build/values.yaml'

# 创建目录
os.system("mkdir -p "+template_parent)

# 设置一些默认值
default_value_dict = {
    'bkCiDataDir': '/data/dir',
    'bkCiHttpPort': '80',
    'bkCiRedisDb': '0',
    'bkCiAuthProvider': 'sample',
    'bkCiLogStorageType': 'elasticsearch',
    'bkCiEsClusterName': 'devops',
    'bkCiProcessEventConcurrent': '10',
    'bkCiLogsDir': '/data/logs',
    'bkCiHome': '/data/bkee/ci',
    'bkCiGatewayDnsAddr': 'local=on',
    'bkCiEnvironmentAgentCollectorOn': 'false',
    'bkHttpSchema': 'http',
    'bkCiHost': 'devops.example.com',
    'bkCiPublicUrl': 'devops.example.com',
    'bkCiPublicHostIp': '127.0.0.1',
    'bkCiIamCallbackUser': 'bk_iam',
    'bkCiAppCode': 'bk_ci',
    'bkCiNotifyWeworkSendChannel': 'weworkAgent',
    'bkCiInfluxdbDb': 'agentMetrix',
    'bkCiEnvironmentAgentCollectorOn': 'true',
    'bkCiDocsUrl': 'https://bk.tencent.com/docs/markdown/持续集成平台/产品白皮书',
    'bkCiArtifactoryRealm': 'local',
    'bkRepoHost': 'repo.demo.com',
    'bkRepoFqdn': 'repo.demo.com',
    'bkRepoGatewayIp': '127.0.0.1',
    'bkCiStreamScmType': 'CODE_GIT',
    'bkCiStreamUrl': 'devops.example.com',
    'bkCiStreamHost': 'devops.example.com',
    'bkCiStreamGitUrl': 'www.github.com',
    'bkCiClusterTag': 'devops',
    'bkCiRepositoryGithubServer':'repository',
}

if os.path.isfile(default_value_json):
    default_value_dict.update(json.load(open(default_value_json)))

# include 模板
include_dict = {
    '__BK_CI_MYSQL_ADDR__': '{{ include "bkci.mysqlAddr" . }}',
    '__BK_CI_MYSQL_USER__': '{{ include "bkci.mysqlUsername" . }}',
    '__BK_CI_MYSQL_PASSWORD__': '{{ include "bkci.mysqlPassword" . }}',
    '__BK_CI_REDIS_HOST__': '{{ if eq .Values.redis.enabled true }}{{ printf "%s.%s.%s" (include "bkci.redisHost" .) .Release.Namespace "svc.cluster.local" | quote}}{{ else }}{{ include "bkci.redisHost" . }}{{ end }}',
    '__BK_CI_REDIS_PASSWORD__': '{{ include "bkci.redisPassword" . }}',
    '__BK_CI_REDIS_PORT__': '{{ include "bkci.redisPort" . }}',
    '__BK_CI_ES_PASSWORD__': '{{ include "bkci.elasticsearchPassword" . }}',
    '__BK_CI_ES_REST_ADDR__': '{{ include "bkci.elasticsearchHost" . }}',
    '__BK_CI_ES_REST_PORT__': '{{ include "bkci.elasticsearchPort" . }}',
    '__BK_CI_ES_USER__': '{{ include "bkci.elasticsearchUsername" . }}',
    '__BK_CI_RABBITMQ_ADDR__': '{{ include "bkci.rabbitmqAddr" . }}',
    '__BK_CI_RABBITMQ_PASSWORD__': '{{ include "bkci.rabbitmqPassword" . }}',
    '__BK_CI_RABBITMQ_USER__': '{{ include "bkci.rabbitmqUser" . }}',
    '__BK_CI_RABBITMQ_VHOST__': '{{ include "bkci.rabbitmqVhost" . }}',
    '__BK_CI_INFLUXDB_HOST__': '{{ if eq .Values.influxdb.enabled true }}{{ printf "%s.%s.%s" (include "bkci.influxdbHost" .) .Release.Namespace "svc.cluster.local" | quote}}{{ else }}{{ include "bkci.influxdbHost" . }}{{ end }}',
    '__BK_CI_INFLUXDB_PORT__': '{{ include "bkci.influxdbPort" . }}',
    '__BK_CI_INFLUXDB_USER__': '{{ include "bkci.influxdbUsername" . }}',
    '__BK_CI_INFLUXDB_PASSWORD__': '{{ include "bkci.influxdbPassword" . }}',
    '__BK_CI_INFLUXDB_ADDR__': 'http://{{ include "bkci.influxdbHost" . }}:{{ include "bkci.influxdbPort" . }}',
    '__BK_CI_VERSION__': '{{ .Chart.AppVersion }}',
    '__BK_CI_DISPATCH_KUBERNETES_NS__': '{{ .Release.Namespace }}',
    '__BK_CI_CONSUL_DISCOVERY_TAG__': '{{ .Release.Namespace }}'
}

# 读取变量映射
env_file = open(env_properties_file, 'r')
value_re = re.compile(r'')
for line in env_file:
    if line.startswith('BK_'):
        # 排除掉数据库的相关值
        if ('_MYSQL_' in line) or ('_REDIS_' in line and 'DB' not in line and 'SENTINEL' not in line) or ('_ES_' in line and 'CLUSTER' not in line) or ('_RABBITMQ_' in line) or ('_INFLUXDB_' in line and 'DB' not in line):
            continue
        datas = line.split("=")
        key = datas[0]
        replace_dict[key] = humps.camelize(key.lower())
env_file.close()

# 生成value.yaml
image_registry = sys.argv[1]
image_gateway_tag = sys.argv[2]
image_backend_tag = sys.argv[3]
value_file = open(output_value_yaml, 'w')
for line in open(default_value_yaml, 'r'):
    line = line.replace("__image_registry__", image_registry)
    line = line.replace("__image_gateway_tag__", image_gateway_tag)
    line = line.replace("__image_backend_tag__", image_backend_tag)
    value_file.write(line)

value_file.write('\nconfig:\n')
for key in sorted(replace_dict):
    default_value = '""'
    if key.endswith("PORT"):
        default_value = '80'
    value_file.write('  '+replace_dict[key]+': '+default_value_dict.get(replace_dict[key], default_value)+'\n')
value_file.flush()
value_file.close()

# 生成服务tpl
config_re = re.compile(r'-[a-z\-]*|common')
for config_name in os.listdir(config_parent):
    if "turbo" in config_name:
        continue
    if config_name.endswith('yaml') or config_name.endswith('yml'):
        config_file = open(config_parent + config_name, 'r')
        the_name = config_re.findall(config_name)[0].replace('-', '', 1)
        new_file = open(template_parent+'_'+the_name+'.tpl', 'w')

        new_file.write('{{- define "bkci.'+the_name+'.yaml" -}}\n')
        for line in config_file:
            for key in replace_pattern.findall(line):
                if include_dict.__contains__(key):
                    line = line.replace(key, include_dict[key])
                else:
                    line = line.replace(key, '{{ .Values.config.'+replace_dict.get(key.replace('__', ''), '')+' }}')
            new_file.write(line)
        new_file.write('\n{{- end -}}')

        new_file.flush()
        new_file.close()
        config_file.close()

# 生成网关的configmap
gateway_envs = set(["__BK_CI_PUBLIC_URL__", "__BK_CI_DOCS_URL__",
                    "__BK_CI_PAAS_LOGIN_URL__", "__BK_CI_VERSION__", "__BK_CI_BADGE_URL__","__BK_REPO_HOST__"])  # frondend需要的变量
for file in os.listdir(config_parent):
    if file.startswith('gateway'):
        for line in open(config_parent+file, 'r'):
            finds = replace_pattern.findall(line)
            for find in finds:
                gateway_envs.add(find)
gateway_config_file = open(template_parent+"/_gateway.tpl", "w")

gateway_config_file.write('{{- define "bkci.gateway.yaml" -}}\n')
for env in gateway_envs:
    if include_dict.__contains__(env):
        gateway_config_file.write(env.replace(
            "__", "")+": "+include_dict[env].replace(' . ', ' . | quote')+"\n")
    else:
        gateway_config_file.write(env.replace(
            "__", "")+": {{ .Values.config."+humps.camelize(env.replace("__", "").lower())+" | quote }}\n")
gateway_config_file.write('NAMESPACE: {{ .Release.Namespace }}\n')
gateway_config_file.write('CHART_NAME: {{ include "bkci.names.fullname" . }}\n')
gateway_config_file.write('{{- end -}}')
gateway_config_file.flush()
gateway_config_file.close()
