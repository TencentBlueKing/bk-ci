import os
import yaml
import base64

output_value_yaml = './values.yaml'
default_value_yaml = './build/values.yaml'
config_path = os.environ.get("config_path")
spring_profile = os.environ.get("spring_profile")
configmap_template_parent = './templates/configmap/tpl/'
secret_template_parent = './templates/secret/tpl/'

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
common_yaml = yaml.safe_load(open(config_server+'application.yml', 'r'))
common_yaml.update(yaml.safe_load(open(config_server+'application-'+spring_profile+'.yml', 'r')))
common_tpl = open(configmap_template_parent+'_common.tpl', 'a+')
common_tpl.write('{{- define "bkci.common.yaml" -}}')
yaml.dump(common_yaml, common_tpl)
common_tpl.write('{{- end -}}')
common_tpl.flush()
common_tpl.close()
# service config
for c_path in os.listdir(config_server):
    service_path = config_server+c_path
    if os.path.isdir(service_path):
        service_name = c_path
        service_tpl = open(configmap_template_parent+'_'+service_name+'.tpl', 'w')
        service_yaml = {}
        if os.path.isfile(service_path+'/application.yml'):
            service_yaml.update(yaml.safe_load(open(service_path+'/application.yml')))
        if os.path.isfile(service_path+'/application-'+spring_profile+'.yml'):
            service_yaml.update(yaml.safe_load(open(service_path+'/application-'+spring_profile+'.yml')))
        service_tpl.write('{{- define "bkci.'+service_name+'.yaml" -}}\n')
        yaml.dump(service_yaml, service_tpl)
        service_tpl.write('{{- end -}}')
        service_tpl.flush()
        service_tpl.close()

# 生成lambda的es的certificate
lambda_certificate_path = config_server+'lambda/certificate/'+spring_profile
if os.path.isdir(lambda_certificate_path):
    keystore64 = ''
    truststore64 = ''
    with open(lambda_certificate_path+'/keystore.jks', 'rb') as ksObj:
        keystore64 = base64.b64encode(ksObj.read()).decode('utf-8')
        print(keystore64)
    with open(lambda_certificate_path+'/truststore.jks', 'rb') as tsObj:
        truststore64 = base64.b64encode(tsObj.read()).decode('utf-8')
        print(truststore64)
    os.mkdir(secret_template_parent)
    lambda_tpl = open(secret_template_parent+'_lambda.tpl', 'w')
    lambda_tpl.write('{{- define "secret.lambda.yaml" -}}\n')
    lambda_tpl.write('keystore.jks: "'+keystore64+'"\n')
    lambda_tpl.write('truststore.jks: "'+truststore64+'"\n')
    lambda_tpl.write('{{- end -}}')
    lambda_tpl.flush()


# gateway config(伪装)
gateway_tpl = open(configmap_template_parent+'_gateway.tpl', 'w')
gateway_tpl.write('{{- define "bkci.gateway.yaml" -}}\n')
gateway_tpl.write('{{- end -}}')
gateway_tpl.flush()
gateway_tpl.close()

# 打包
os.system("helm package . --version " + os.environ.get("chart_version") + " --app-version "+os.environ.get("version"))
