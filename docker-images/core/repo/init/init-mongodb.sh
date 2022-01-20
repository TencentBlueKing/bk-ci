#!/bin/bash

username=$BK_REPO_USERNAME
password_md5=$(echo -n $BK_REPO_PASSWORD | md5sum | cut -d ' ' -f1)
sed -i "s/\"admin\"/\"$username\"/g" init-data.js
sed -i "s/5f4dcc3b5aa765d61d8327deb882cf99/$password_md5/g" init-data.js

mongo $BK_REPO_MONGODB_URI init-data.js
