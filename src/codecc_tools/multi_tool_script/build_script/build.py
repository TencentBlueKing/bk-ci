#!/usr/bin/python
# -*- coding: utf-8 -*-
import sys,os
import multiprocessing
import xmlrpc.client
import zipfile
import shutil

def main_input(stream_name, argv):
    command = 'python scan.py ' + stream_name
    print(command)
    for i in range(len(argv)-2):
        command += ' \''+argv[i+2]+'\''
    ret = os.system(command)
    if not ret == 0:
        raise

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
                
if __name__ == "__main__" :
    current_path=sys.path[0]
    if len(sys.argv) > 3:
        scan_tools = ''
        mount_path = ''
        for i in range(len(sys.argv)-2):
            if not "=" in sys.argv[i+2] or not "-D" in sys.argv[i+2]:
                print("Usage python %s [stream_name] -Dxxx=xxx" % sys.argv[0])
                sys.exit()
        for i in range(len(sys.argv)-2):
            tmp = sys.argv[i+2].split("=",1)
            if 'SCAN_TOOLS' in tmp[0].replace("-D",""):
                scan_tools = tmp[1].replace("\n", "")
                print('scan tool is: '+scan_tools)
            if 'MOUNT_PATH' in tmp[0].replace("-D",""):
                mount_path = tmp[1].replace("\n", "")
                print('mount path is: '+mount_path)
        if scan_tools == '':
            print('can\'t found the SCAN_TOOLS options, please add it, as: -DSCAN_TOOLS=cpplint')
            sys.exit()
        #download scan script...
        print("download multi tool scan script...")
        try:
            bin_folder = current_path+"/codecc_scan"
            scan_file = current_path+"/codecc_scan.zip"
            if os.path.exists(bin_folder): delete_file_folder(bin_folder)
            if os.path.exists(scan_file):
                delete_file_folder(scan_file)
            #unzip bin.zip...
            print("unzip codecc_scan folder...")
            src_path = "%s/client/codecc_scan.zip" % mount_path
            shutil.copyfile(src_path, current_path+"/codecc_scan.zip")
            unzip_file(current_path+"/codecc_scan.zip", current_path)   
        except Exception as e:
            print(e)
            exit(1)
            
        if not os.path.exists(bin_folder):
            print('can\'t find the path '+bin_folder)
            exit(1)
            
        os.chdir(bin_folder+'/bin') 
        process = multiprocessing.Pool(processes = int(len(scan_tools.split(','))))
        for tool in scan_tools.split(','):
            try:
                rec = process.apply_async(main_input, (sys.argv[1]+'_'+tool, sys.argv,))
                rec.get()
            except Exception as e:
                sys.exit(1)
        process.close()
        process.join() 
        print('scan tool end: '+scan_tools)
    else:
        print(" Usage python %s [stream_name]_[cpplint&astyle&pylint] " % sys.argv[0])
        sys.exit()