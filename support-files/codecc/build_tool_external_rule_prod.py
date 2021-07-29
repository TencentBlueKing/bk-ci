# -*- coding: utf-8 -*-
import sys,os,zipfile
import json, http.client
import multiprocessing
import subprocess
import hashlib
import platform
os_type = platform.system()
stream_info={}
env='prod'
download_file='codecc_scan_external_'+env+'.zip'
gray_download_file = 'codecc_scan_external_gray.zip'
if os_type == "Windows":
    download_file = 'win_codecc_scan_external_'+env+'.zip'
    gray_download_file = 'win_codecc_scan_external_gray.zip'
codecc_server_host = ''
codecc_server_post = '80'

def config_tool_in_properties():    
    headers = {"Content-type": "application/json","x-devops-build-id": stream_info['LANDUN_BUILDID'], "x-devops-vm-sid": stream_info['DEVOPS_AGENT_VM_SID'], "x-devops-project-id": stream_info['DEVOPS_PROJECT_ID'], "x-devops-build-type": stream_info['DEVOPS_BUILD_TYPE'], "x-devops-agent-id": stream_info['DEVOPS_AGENT_ID'], "x-devops-agent-secret-key": stream_info['DEVOPS_AGENT_SECRET_KEY']}
    conn = None
    try:
        conn = http.client.HTTPConnection(codecc_server_host, codecc_server_post)
        conn.request("GET", "/ms/task/api/build/toolmeta/list", '', headers)
        response = conn.getresponse()
        return str(response.read().decode())
    except Exception as e:
        print(e)
        return e
    finally:
        conn.close()

def codecc_config_by_stream(stream_name):    
    headers = {"Content-type": "application/json","x-devops-build-id": stream_info['LANDUN_BUILDID'], "x-devops-vm-sid": stream_info['DEVOPS_AGENT_VM_SID'], "x-devops-project-id": stream_info['DEVOPS_PROJECT_ID'], "x-devops-build-type": stream_info['DEVOPS_BUILD_TYPE'], "x-devops-agent-id": stream_info['DEVOPS_AGENT_ID'], "x-devops-agent-secret-key": stream_info['DEVOPS_AGENT_SECRET_KEY']}
    conn = None
    try:
        conn = http.client.HTTPConnection(codecc_server_host, codecc_server_post)
        conn.request("GET", "/ms/task/api/build/task/streamName/"+stream_name, '', headers)
        response = conn.getresponse()
        return str(response.read().decode())
    except Exception as e:
        return e
    finally:
        conn.close()

def getMd5(filename):
    size = os.path.getsize(filename)
    btyes_buffer=100*1024*1024
    if size < btyes_buffer:
        btyes_buffer = size
    myhash = hashlib.md5()
    f = open(filename,'rb')
    while True:
        b = f.read(btyes_buffer)
        if not b :
            break
        myhash.update(b)
    f.close()
    return myhash.hexdigest()

def compare_file_md5(filePath, result_name, download_type, offline_md5): 
    headers = {"Content-type": "application/json","x-devops-build-id": stream_info['LANDUN_BUILDID'], "x-devops-vm-sid": stream_info['DEVOPS_AGENT_VM_SID'], "x-devops-project-id": stream_info['DEVOPS_PROJECT_ID'], "x-devops-build-type": stream_info['DEVOPS_BUILD_TYPE'], "x-devops-agent-id": stream_info['DEVOPS_AGENT_ID'], "x-devops-agent-secret-key": stream_info['DEVOPS_AGENT_SECRET_KEY']}
    try:
        params = {}
        params['fileName'] = result_name
        params['downloadType'] = download_type
        jdata = json.dumps(params)
        conn = http.client.HTTPConnection(codecc_server_host, codecc_server_post, timeout=60)
        conn.request("POST", "/ms/schedule/api/build/fs/download/fileInfo", jdata, headers)
        response = conn.getresponse()
        data = str(response.read().decode())
        if not data == "":
            data_array = json.loads(data)
            if data_array["status"] != 0:
                print(json.dumps(data_array))
                raise
            else:
                file_info = data_array['data']
                if 'contentMd5' in file_info:
                    online_md5 = file_info['contentMd5']
                    if online_md5 == offline_md5:
                        return True
    except Exception as e:
        raise Exception('get the download file '+result_name+' fileInfo failded! please contact The CodeCC to check it!')
    finally:
        conn.close()
    return False

def download(filePath, result_name, download_type): 
    size = 0
    headers = {"Content-type": "application/json","x-devops-build-id": stream_info['LANDUN_BUILDID'], "x-devops-vm-sid": stream_info['DEVOPS_AGENT_VM_SID'], "x-devops-project-id": stream_info['DEVOPS_PROJECT_ID'], "x-devops-build-type": stream_info['DEVOPS_BUILD_TYPE'], "x-devops-agent-id": stream_info['DEVOPS_AGENT_ID'], "x-devops-agent-secret-key": stream_info['DEVOPS_AGENT_SECRET_KEY']}
    try:
        params = {}
        params['fileName'] = result_name
        params['downloadType'] = download_type
        jdata = json.dumps(params)
        conn = http.client.HTTPConnection(codecc_server_host, codecc_server_post, timeout=60)
        conn.request("POST", "/ms/schedule/api/build/fs/download/fileSize", jdata, headers)
        response = conn.getresponse()
        data = str(response.read().decode())
        if not data == "":
            data_array = json.loads(data)
            if data_array["status"] != 0:
                print(json.dumps(data_array))
                raise
            else:
                size = int(data_array['data'])
    except Exception as e:
        raise Exception('get the download file '+result_name+' size failded! please contact The CodeCC to check it!')
    finally:
        conn.close()

    if size >0:
        params = {}
        params['fileName'] = result_name
        params['downloadType'] = download_type
        conn = http.client.HTTPConnection(codecc_server_host, codecc_server_post, timeout=60)
        print("downloading "+result_name+" Size is "+str(int(size/(1024*1024)))+"M")
        retain_size = size
        label=0
        send_buffer=0
        is_stop=False
        btyes_buffer=100*1024*1024
        while 1:
            if retain_size > btyes_buffer:
                retain_size -= btyes_buffer
                send_buffer = btyes_buffer
                is_stop=False
            else:
                send_buffer = retain_size
                is_stop=True 
            a = "#" * int(label*100/size) + " " * int(100-label*100/size) + "["+str(int(label*100/size)) + "%"+"]"
            sys.stdout.write("\r%s" %a)
            sys.stdout.flush()
            params['beginIndex'] = str(label)
            params['btyeSize'] = str(send_buffer)
            jdata = json.dumps(params)
            conn.request("POST", "/ms/schedule/api/build/fs/download", jdata, headers)
            response = conn.getresponse()
            if label == 0:
                with open(filePath, "wb") as handle:
                    handle.write(response.read())
            else:
                with open(filePath, "ab+") as handle:
                    handle.write(response.read())
                    
            label += send_buffer
            if is_stop:
                a = "#" * int(label*100/size) + " " * int(100-label*100/size) + "["+str(int(label*100/size)) + "%"+"]"
                sys.stdout.write("\r%s\n" %a)
                sys.stdout.flush()
                print("download sucessful!")
                return True
    return False
    
def download_and_unzip(data_root_path):
    global download_file
    #backup codecc_scan folder..
    if not os.path.exists(data_root_path): os.mkdir(data_root_path)
    bin_folder = data_root_path + "/codecc_scan"
    zip_path = data_root_path + "/codecc_scan.zip"
    if stream_info['DEVOPS_PROJECT_ID'] == 'codecc':
        download_file = gray_download_file
    # if 'DATA_ROOT_PATH' in stream_info:
    #     zip_path = os.path.dirname(stream_info['DATA_ROOT_PATH']) + '/codecc_scan.zip'
    if os.path.exists(bin_folder): delete_file_folder(bin_folder)
    is_download = True
    if os.path.exists(zip_path):
        offline_md5 = getMd5(zip_path)
        if compare_file_md5(zip_path, download_file, 'BUILD_SCRIPT', offline_md5):
            is_download = False
    #download scan script...  
    if is_download:
        print("download codecc scan script...")
        if not download(zip_path, download_file, 'BUILD_SCRIPT'):
            print('the codecc_scan.zip download failded! please contact the CodeCC')
            exit(1)
        
    #unzip codecc_build.zip...
    print("unzip codecc_scan folder...")
    unzip_file(zip_path, data_root_path)
    #os.system('chmod -R 755 '+bin_folder)
    return bin_folder
        
def main_input_new(scan_path, stream_name, tool,  argv):
    tool_list = []
    if isinstance(tool, list):
        tool_list.extend(tool)
    else:
        tool_list.append(tool)
    for scan_tool in tool_list:
        result_status = 0
        os.chdir(scan_path)
        command = 'python3 scan.py ' + stream_name +' '+scan_tool
        for i in range(len(argv)-2):
            if os_type == "Windows":
                command += ' \"'+argv[i+2].replace('\"','\\"')+'\"'
            else:
                command += ' \''+argv[i+2]+'\''
        proc = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,shell=True, start_new_session=True)
        try:
            while True:
                next_line = proc.stdout.readline()
                if str(next_line.decode()).strip() == '' and proc.poll() != None:
                    break
                if str(next_line.decode()).strip() != '':
                    sys.stdout.write("["+scan_tool+"]:"+str(next_line.decode()))
                    sys.stdout.flush()
        finally:
            result_status = proc.wait()
        if not result_status == 0:
            raise

def unzip_file(zipfilename, unziptodir):
    if not os.path.exists(unziptodir): os.makedirs(unziptodir)
    zfobj = zipfile.ZipFile(zipfilename)
    for name in zfobj.namelist():
        name = name.replace('\\','/')
        if name.endswith('/'):
            os.makedirs(os.path.join(unziptodir, name))
        else:            
            ext_filename = os.path.join(unziptodir, name)
            ext_dir= os.path.dirname(ext_filename)
            if not os.path.exists(ext_dir) : os.makedirs(ext_dir)
            with open(ext_filename, 'wb') as outfile:
                outfile.write(zfobj.read(name))
                
def delete_file_folder(src):  
    if os.path.isfile(src):  
        try:  
            os.remove(src)  
        except:  
            pass 
    elif os.path.isdir(src):  
        for item in os.listdir(src):  
            itemsrc=os.path.join(src,item)  
            delete_file_folder(itemsrc)  
        try:  
            os.rmdir(src)  
        except:  
            pass  
              
if __name__ == "__main__" :
    #global codecc_server_host
    current_path=sys.path[0]
    build_status = 0
    if len(sys.argv) > 3:
        nocompile_scan_tools = []
        compile_scan_tools = []
        rec_list = []
        scan_tools = []
        for i in range(len(sys.argv)-2):
            if not "=" in sys.argv[i+2] or not "-D" in sys.argv[i+2]:
                print(sys.argv[i+2])
                print("Usage python %s [stream_name] -Dxxx=xxx" % sys.argv[0])
                sys.exit()
        for i in range(len(sys.argv)-2):
            tmp = sys.argv[i+2].split("=",1)
            stream_info[tmp[0].replace("-D","")] = tmp[1].replace("\n", "")

        if 'CODECC_API_WEB_SERVER' in stream_info:
            codecc_server_host = stream_info['CODECC_API_WEB_SERVER']
        else:
            print('can not found the CODECC_API_WEB_SERVER option please check it')
            sys.exit()

        config_data = codecc_config_by_stream(sys.argv[1])
        if config_data != "":
            data_array=json.loads(config_data)
            if 'data' in data_array:
                data = data_array['data']
                if 'status' in data and data['status'] == 1:
                    print('this project already stoped, can\'t scan by CodeCC!')
                    exit(1)
                if 'toolSet' in data:
                    tool_name_list = data['toolSet']
                    for tool in tool_name_list:
                        tool = tool.lower()
                        if os_type == "Windows" and tool == 'cloc':
                            continue
                        if tool in ['coverity', 'klocwork', 'pinpoint', 'codeql']:
                            compile_scan_tools.append(tool)
                        else:
                            nocompile_scan_tools.append(tool)
    
        bin_folder = download_and_unzip(stream_info['DATA_ROOT_PATH'])
            
        if not os.path.exists(bin_folder):
            print('can\'t find the path '+bin_folder)
            exit(1)
        
        if len(nocompile_scan_tools) > 0:
            scan_tools.extend(nocompile_scan_tools)
        if len(compile_scan_tools) > 0:
            scan_tools.append(compile_scan_tools)
        #update the properties to add tool_list
        tool_config_json = json.loads(config_tool_in_properties())
        if 'data' in tool_config_json:
            config_file = bin_folder + '/codecc_agent/config/config.properties'
        with open(config_file, 'a+', encoding='utf-8') as file:
            if 'CODECC_API_WEB_SERVER' in stream_info:
                file.write('CODECC_API_WEB_SERVER='+stream_info['CODECC_API_WEB_SERVER']+'\n')
            for tool_json in tool_config_json['data']:
                tool_name = ''
                if 'name' in tool_json:
                    tool_name = tool_json['name']
                if 'toolScanCommand' in tool_json:
                    file.write(tool_name.upper()+'_SCAN_COMMAND='+tool_json['toolScanCommand']+'\n')
                if 'dockerTriggerShell' in tool_json:
                    file.write(tool_name.upper()+'_TRIGGER_SHELL='+tool_json['dockerTriggerShell']+'\n')
                if 'dockerImageURL' in tool_json:
                    if ':' in tool_json['dockerImageURL']:
                        file.write(tool_name.upper()+'_IMAGE_PATH='+tool_json['dockerImageURL']+'\n')
                    elif not 'dockerImageVersion' in tool_json or tool_json['dockerImageVersion'] == '':
                        file.write(tool_name.upper()+'_IMAGE_PATH='+tool_json['dockerImageURL']+'\n')
                    else:
                        file.write(tool_name.upper()+'_IMAGE_PATH='+tool_json['dockerImageURL']+':'+tool_json['dockerImageVersion']+'\n')
                if 'toolEnv' in tool_json:
                    file.write(tool_name.upper()+'_ENV='+tool_json['toolEnv']+'\n')
                else:
                    file.write(tool_name.upper()+'_ENV=\n')
                if 'toolRunType' in tool_json:
                    file.write(tool_name.upper()+'_RUN_TYPE='+tool_json['toolRunType']+'\n')
                else:
                    file.write(tool_name.upper()+'_RUN_TYPE=docker\n')
                if 'dockerImageAccount' in tool_json:
                    file.write(tool_name.upper()+'_REGISTRYUSER='+tool_json['dockerImageAccount']+'\n')
                else:
                    file.write(tool_name.upper()+'_REGISTRYUSER=\n')
                if 'dockerImagePasswd' in tool_json:
                    file.write(tool_name.upper()+'_REGISTRYPWD='+tool_json['dockerImagePasswd']+'\n')
                else:
                    file.write(tool_name.upper()+'_REGISTRYPWD=\n') 
                    
        scan_path = bin_folder+'/codecc_agent/bin'
        if len(scan_tools) > 0:
            print('tools: '+str(scan_tools))
            rec_list = [i for i in range(len(scan_tools))]
            process = multiprocessing.Pool(processes = int(len(scan_tools)))
            for index, tool in enumerate(scan_tools):
                rec_list[index] = process.apply_async(main_input_new, (scan_path, sys.argv[1], tool, sys.argv,))
            process.close()
            process.join()
        
        try:
            for rec in rec_list:
                if not isinstance(rec, int) and rec.get() == 1:
                    sys.exit(1)
        except Exception as e:
            sys.exit(1)
        print('scan tool end: '+str(scan_tools))
    else:
        print(" Usage python %s [stream_name]_[cpplint&astyle&pylint] " % sys.argv[0])
        sys.exit()
