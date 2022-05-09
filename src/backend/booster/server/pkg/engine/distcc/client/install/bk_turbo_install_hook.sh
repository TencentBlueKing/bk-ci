#!/bin/bash

# install client hook
echo "** install client hook lib **"
local_distcc=`which distcc`
local_ccache=`which ccache`
echo "found distcc installed in $local_distcc"
echo "found ccache installed in $local_ccache"

mkdir -p /etc/bkhook
current_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cp -R ${current_dir}/bkhook/* /etc/bkhook/
sed -i 's/$DISTCCPATH/'${local_distcc//\//\\\/}'/g' /etc/bkhook/*.json
sed -i 's/$CCACHEPATH/'${local_ccache//\//\\\/}'/g' /etc/bkhook/*.json
cp ${current_dir}/bkhook.so /lib64

echo "** install client hook lib end **"