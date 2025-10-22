/*
* Tencent is pleased to support the open source community by making
* 蓝鲸智云PaaS平台社区版 (BlueKing PaaS Community Edition) available.
*
* Copyright (C) 2021 Tencent.  All rights reserved.
*
* 蓝鲸智云PaaS平台社区版 (BlueKing PaaS Community Edition) is licensed under the MIT License.
*
* License for 蓝鲸智云PaaS平台社区版 (BlueKing PaaS Community Edition):
*
* ---------------------------------------------------
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
* documentation files (the "Software"), to deal in the Software without restriction, including without limitation
* the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
* to permit persons to whom the Software is furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all copies or substantial portions of
* the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
* THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
* CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
* IN THE SOFTWARE.
*/
export const ROOT_KEY = 'tree-root-key';
// action const
export const LOGIN = 'LOGIN';
export const FETCH_RSA = 'FETCH_RSA';
export const FETCH_USER_INFO = 'FETCH_USER_INFO';
export const FETCH_USER_SETTING = 'FETCH_USER_SETTING'; // TODO: 获取用户详情
export const FETCH_PROJECT_LIST = 'FETCH_PROJECT_LIST';
export const FETCH_USER_LIST = 'FETCH_USER_LIST';
export const FETCH_REPO_LIST = 'FETCH_REPO_LIST';
export const CREATE_REPO = 'CREATE_REPO';
export const CHECK_REPO_EXIST = 'CHECK_REPO_EXIST';
export const DELETE_REPO = 'DELETE_REPO';
export const INIT_REPO_TREE = 'INIT_REPO_TREE';
export const FETCH_TREE_CHILDREN = 'FETCH_TREE_CHILDREN';
export const EXPAND_TREE_ITEM = 'EXPAND_TREE_ITEM';
export const FETCH_ARTIFACTORIES = 'FETCH_ARTIFACTORIES';
export const CALCULATE_FOLDER_SIZE = 'CALCULATE_FOLDER_SIZE';
export const GET_ARTIFACTORY_INFO = 'GET_ARTIFACTORY_INFO';
export const CREATE_TOKEN = 'CREATE_TOKEN';
export const DELETE_META_DATA = 'DELETE_META_DATA';
export const ADD_META_DATA = 'ADD_META_DATA';
export const GET_DOWNLOAD_URL = 'GET_DOWNLOAD_URL';
export const SHARE_ARTIFACTORY = 'SHARE_ARTIFACTORY';
export const CREATE_FOLDER = 'CREATE_FOLDER';
export const RENAME = 'RENAME';
export const MOVE = 'MOVE';
export const COPY = 'COPY';
export const DELETE = 'DELETE';
export const GET_FOLDER_FILES_COUNT = 'GET_FOLDER_FILES_COUNT';
export const UPLOAD = 'UPLOAD';
export const UPDATE_REPO_INFO = 'UPDATE_REPO_INFO';
export const FETCH_REPO_INFO = 'FETCH_REPO_INFO';
export const FETCH_SEARCH_REPO = 'FETCH_SEARCH_REPO';
export const SEARCH = 'SEARCH';
export const FETCH_USER_TOKEN = 'FETCH_USER_TOKEN';
export const DELETE_USER_TOKEN = 'DELETE_USER_TOKEN';
export const UPDATE_USER_INFO = 'UPDATE_USER_INFO';
export const MODIFY_USER_PWD = 'MODIFY_USER_PWD';
export const GET_PACKAGE_INFO = 'GET_PACKAGE_INFO';
export const GET_PACKAGE_VERSIONS = 'GET_PACKAGE_VERSIONS';
export const DELETE_PACKAGE_VERSION = 'DELETE_PACKAGE_VERSION';
export const FETCH_PACKAGE_DETAIL = 'FETCH_PACKAGE_DETAIL';
export const GET_ARTIFACTORY_URL = 'GET_ARTIFACTORY_URL';
export const DELETE_PACKAGE = 'DELETE_PACKAGE';
export const PACKAGE_UPGRADE = 'PACKAGE_UPGRADE';
export const FETCH_SEC_SCAN_LIST = 'FETCH_SEC_SCAN_LIST';
export const SCAN = 'SCAN';
export const GET_DOMAIN = 'GET_DOMAIN';

// mutation const
export const SET_REPO_LIST = 'SET_REPO_LIST';
export const UPDATE_REPO_MAP = 'UPDATE_REPO_MAP';
export const SET_REPO_TREE = 'SET_REPO_TREE';
export const UPDATE_REPO_TREE_ITEM = 'UPDATE_REPO_TREE_ITEM';
export const CLEAR_TREE = 'CLEAR_TREE';
export const CLOSE_OPERATION = 'CLOSE_OPERATION';
export const ACTIVE_OPERATION = 'ACTIVE_OPERATION';
export const SET_USER_INFO = 'SET_USER_INFO';
export const SET_PROJECT_LIST = 'SET_PROJECT_LIST';
export const SET_USER_MAP = 'SET_USER_MAP';
export const SET_DOMAIN = 'SET_DOMAIN';
