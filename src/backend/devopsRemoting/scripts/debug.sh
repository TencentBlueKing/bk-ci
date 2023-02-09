#!/bin/bash

make debug.ide.vscode.image -f makefile

nohup docker run --name remoting-debug -p 23000:23000 -p 23001:23001 -p 22999:22999 devops/remoting:debug >> temp/debug.log