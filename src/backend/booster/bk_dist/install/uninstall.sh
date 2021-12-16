#!/bin/bash

olddir=`pwd`

echo "** turbo client uninstall begin **"

user=`whoami`
if [ "${user}" != "root" ];then
	echo "need root to run uninstall, please su to root"
	cd ${olddir}
	exit 1
fi

# remove binaries
echo "1. removing binaries"
rm -f /usr/local/bin/bk-booster
rm -f /usr/local/bin/bk-booster.bak
rm -f /usr/local/bin/bk-dist-controller
rm -f /usr/local/bin/bk-dist-controller.bak
rm -f /usr/local/bin/bk-dist-executor
rm -f /usr/local/bin/bk-dist-executor.bak

# remove configuration files
echo "2. removing configuration files"
rm -rf /etc/bk_dist

echo "** turbo client uninstall end **"

cd ${olddir}