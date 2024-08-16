import os
import yaml

output_value_yaml = './values.yaml'
default_value_yaml = './base/values.yaml'
config_path = os.environ.get("config_path")
configmap_template_parent = './templates/configmap/tpl/'


def write_application_tpl(config_path, tpl_path):
    files_list = os.listdir(config_path)
    files_list.sort(reverse=True)
    for file_name in files_list:  # for循环读取application开头的yaml文件
        file_path = config_path+file_name
        if os.path.isfile(file_path) and file_name.startswith("application"):
            print("read : "+file_path)
            common_yaml = yaml.safe_load(open(file_path, 'r'))
            yaml.dump(common_yaml, tpl_path)
            tpl_path.write('---\n')


# 生成value.yaml
image_gateway_tag = '0.0'
image_backend_tag = '0.0'
value_file = open(output_value_yaml, 'w')
for line in open(default_value_yaml, 'r'):
    line = line.replace("__image_gateway_tag__", image_gateway_tag)
    line = line.replace("__image_backend_tag__", image_backend_tag)
    value_file.write(line)
value_file.flush()
value_file.close()

config_server = config_path+"/config-server/"
# 生成 tpl
common_tpl = open(configmap_template_parent+'_common.tpl', 'w')
common_tpl.write('{{- define "bkci.common.yaml" -}}')
write_application_tpl(config_server, common_tpl)
common_tpl.write('{{- end -}}')
common_tpl.flush()
common_tpl.close()
# service config
for service_name in os.listdir(config_server):
    service_path = config_server + service_name+'/'
    if os.path.isdir(service_path):
        service_tpl = open(configmap_template_parent+'_'+service_name+'.tpl', 'w')
        service_tpl.write('{{- define "bkci.'+service_name+'.yaml" -}}\n')
        write_application_tpl(service_path, service_tpl)
        service_tpl.write('{{- end -}}')
        service_tpl.flush()
        service_tpl.close()


# gateway config(伪装)
gateway_tpl = open(configmap_template_parent+'_gateway.tpl', 'w')
gateway_tpl.write('{{- define "bkci.gateway.yaml" -}}\n')
gateway_tpl.write('{{- end -}}')
gateway_tpl.flush()
gateway_tpl.close()

# 打包
os.system("helm package . --version " + os.environ.get("chart_version") + " --app-version "+os.environ.get("version"))
