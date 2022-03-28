#!/bin/bash

olddir=`pwd`

echo "** turbo client install begin **"

user=`whoami`
if [ "${user}" != "root" ];then
	echo "need root to install this tool, please su to root"
	cd ${olddir}
	exit 1
fi

# check if rpm is exists
if rpm --version >/dev/null 2>&1; then
    echo "1. rpm is ok"
else
    echo "rpm no found"
	cd ${olddir}
    exit 1
fi

# install ccache if not exists
echo "2. check ccache"
if ccache --version >/dev/null 2>&1; then
    echo "ccache already exists"
    local_ccache=`which ccache`
else
    echo "ccache not installed, run \"yum install ccache -y\" if you need"
    local_ccache="/usr/bin/ccache"
fi

# install bk clients
echo "3. install bk clients"
current_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
boostername="bk-booster"
executorname="bk-dist-executor"
controllername="bk-dist-controller"
if [ ! -f ${current_dir}/${executorname} -o ! -f ${current_dir}/${controllername} -o ! -f ${current_dir}/${boostername} ];then
	echo "not found ${current_dir}/${executorname} or ${current_dir}/${controllername} or ${current_dir}/${boostername}, please ensure these files existed "
	cd ${olddir}
	exit 1
fi

userbin="/usr/local/bin"
mkdir -p ${userbin}
if [ -f ${userbin}/${boostername} ];then
	mv ${userbin}/${boostername} ${userbin}/${boostername}.bak
fi
cp ${current_dir}/${boostername} ${userbin}/

if [ -f ${userbin}/${executorname} ];then
	mv ${userbin}/${executorname} ${userbin}/${executorname}.bak
fi
cp ${current_dir}/${executorname} ${userbin}/

if [ -f ${userbin}/${controllername} ];then
	mv ${userbin}/${controllername} ${userbin}/${controllername}.bak
fi
cp ${current_dir}/${controllername} ${userbin}/


# install hook so
echo "4. install bk hook so"
soname="bk-hook.so"
if [ ! -f ${current_dir}/${soname} ];then
	echo "not found ${current_dir}/${soname}, please ensure these files existed "
	cd ${olddir}
	exit 1
fi

cp ${current_dir}/${soname} /usr/lib64


# install and modify config files
echo "5. install hook config files"
jsonnum=`ls ${current_dir}/template/*.json|wc -l`
if [ ${jsonnum} -le 0 ];then
	echo "not found any json template file in ${current_dir}/template, please ensure these files existed "
	cd ${olddir}
	exit 1
fi

configpath="/etc/bk_dist"
mkdir -p ${configpath}
cp -R ${current_dir}/template/*.json ${configpath}/
cp -R ${current_dir}/launcher ${configpath}/

localexecutor="${userbin}/${executorname}"
sed -i -e 's:\$EXECUTORPATH:'${localexecutor}':g' ${configpath}/*.json
sed -i -e 's:\$CCACHEPATH:'${local_ccache}':g' ${configpath}/*.json

echo "** turbo client install end **"

cd ${olddir}