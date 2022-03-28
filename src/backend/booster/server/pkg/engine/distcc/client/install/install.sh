#!/bin/bash

echo "** turbo client install begin **"

# check if rpm is exists
if rpm --version >/dev/null 2>&1; then
    echo "1. rpm is ok"
else
    echo "rpm no found"
    exit 1
fi

# install ccache if not exists
echo "2. install ccache"
if ccache --version >/dev/null 2>&1; then
    echo "ccache already exists"
else
    yum install ccache -y
fi

# install distcc if not exists
echo "3. install distcc"
if distcc -dumpversion >/dev/null 2>&1; then
    echo "distcc already exists"
else
    yum install distcc -y
fi

# install client
mkdir -p /etc/bk_distcc

if [[ -f /usr/bin/bk-make ]]; then
    mv /usr/bin/bk-make /usr/bin/bk-make.bak
fi
echo "4. install bk-make"
cp bk-make /usr/bin/bk-make

if [[ -f /usr/bin/bk-cmake ]]; then
    mv /usr/bin/bk-cmake /usr/bin/bk-cmake.bak
fi
echo "5. install bk-cmake"
cp bk-cmake /usr/bin/bk-cmake

if [[ -f /usr/bin/bk-bazel ]]; then
    mv /usr/bin/bk-bazel /usr/bin/bk-bazel.bak
fi
echo "6. install bk-bazel"
cp bk-bazel /usr/bin/bk-bazel
cp -R bazel/ /etc/bk_distcc/
echo "bazel templates are under /etc/bk_distcc"

if [[ -f /usr/bin/bk-blade ]]; then
    mv /usr/bin/bk-blade /usr/bin/bk-blade.bak
fi
echo "7. install bk-blade"
cp bk-blade /usr/bin/bk-blade

echo "8. install client hook lib"
./bk_turbo_install_hook.sh

echo "** turbo client install end **"