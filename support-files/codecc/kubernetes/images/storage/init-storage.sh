#!/bin/bash

# copy script_download to ${BK_CODECC_FILE_DATA_PATH}/download/script_download
if [ ! -d "${BK_CODECC_FILE_DATA_PATH}/download" ]; then
  mkdir ${BK_CODECC_FILE_DATA_PATH}/download
fi
cp -r /data/workspace/script_download ${BK_CODECC_FILE_DATA_PATH}/download

# create default bkrepo project|repo|user 
if [[ "$BK_CODECC_STORAGE_TYPE" == "bkrepo" ]];then
    # get authorization header
    authorization=$(echo -n $BK_CODECC_STORAGE_BKREPO_ADMIN_USERNAME:$BK_CODECC_STORAGE_BKREPO_ADMIN_PASSWORD | base64)
    #create bkrepo project for codecc
    curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' --header "Authorization: Basic $authorization" -d "{
        \"name\": \"$BK_CODECC_STORAGE_BKREPO_PROJECT\",
        \"displayName\": \"$BK_CODECC_STORAGE_BKREPO_PROJECT\",
        \"description\": \"codecc project\"
    }" "${BK_CODECC_STORAGE_BKREPO_SCHEMA}://$BK_CODECC_STORAGE_BKREPO_HOST/repository/api/project/create"

    #create bkrepo project repo for codecc
    curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' --header "Authorization: Basic $authorization" -d "{
        \"projectId\": \"$BK_CODECC_STORAGE_BKREPO_PROJECT\",
        \"name\": \"$BK_CODECC_STORAGE_BKREPO_REPO\",
        \"type\": \"GENERIC\",
        \"category\": \"LOCAL\",
        \"public\": false
    }" "${BK_CODECC_STORAGE_BKREPO_SCHEMA}://$BK_CODECC_STORAGE_BKREPO_HOST/repository/api/repo/create"

    #create bkrepo project repo user for codecc
    curl -X POST --header 'Content-Type: application/json' --header 'Accept: application/json' --header "Authorization: Basic $authorization" -d "{
        \"projectId\": \"$BK_CODECC_STORAGE_BKREPO_PROJECT\",
        \"repoName\": \"$BK_CODECC_STORAGE_BKREPO_REPO\",
        \"name\": \"$BK_CODECC_STORAGE_BKREPO_USERNAME\",
        \"pwd\": \"$BK_CODECC_STORAGE_BKREPO_PASSWORD\",
        \"userId\": \"$BK_CODECC_STORAGE_BKREPO_USERNAME\",
        \"group\": false
    }" "${BK_CODECC_STORAGE_BKREPO_SCHEMA}://$BK_CODECC_STORAGE_BKREPO_HOST/auth/api/user/create/repo"
fi  