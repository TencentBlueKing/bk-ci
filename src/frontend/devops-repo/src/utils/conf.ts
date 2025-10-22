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
import { CommonOption } from './vue-ts';

export const LOCAL_CATEGORY = 'LOCAL';
export const COMPOSITE_CATEGORY = 'COMPOSITE';
export const GENERIC_REPO = 'generic';
export const HELM_REPO = 'helm';
export const GIT_REPO = 'git';
export const DOCKER_REPO = 'docker';
export const NPM_REPO = 'npm';
export const MAVEN_REPO = 'maven';
export const PUBLIC_AUTH = 'public';
export const PROJECT_AUTH = 'project';
export const SYSTEM_AUTH = 'system';
export const UNKNOW_FILE_EXT = 'file';

export const REPO_TYPE_LIST: CommonOption[] = [
  {
    id: GENERIC_REPO,
    name: 'Generic',
  },
  {
    id: HELM_REPO,
    name: 'Helm',
  },
];

export const SORT_PROPERTY = {
  MOD_DATE: 'lastModifiedDate',
  NAME: 'name',
  DOWNLOAD: 'downloads',
};

export const LEVEL_ENUM = {
  RELEASE: '@release',
  PRE_RELEASE: '@prerelease',
};

export const ACCESS_AUTH_TYPE_LIST: CommonOption[] = [
  {
    id: PROJECT_AUTH,
    tips: 'accessProjectAuth',
    name: 'projectOnlyAuth',
  },
  {
    id: PUBLIC_AUTH,
    tips: 'accessPublicAuth',
    name: 'publicAuth',
  },
];

export const FILE_TYPE: Record<string, string> = {
  gif: 'png',
  jpg: 'png',
  psd: 'png',
  jpge: 'png',
  mov: 'mp4',
  avi: 'mp4',
  asf: 'mp4',
  wmv: 'mp4',
  rmvb: 'mp4',
  rm: 'mp4',
  jar: 'zip',
  rar: 'zip',
  map: 'js',
  pyc: 'py',
  xsd: 'xml',
};

export const REPO_TYPE_ICON: Record<string, string> = {
  pypi: 'py',
};

export const DOCS = {
  META_DOCS: 'https://docs.bkci.net/services/bkrepo/meta',
};


// name: 'repoList',
// name: 'repo',
// name: 'genericRepo',
// name: 'commonRepo',
// name: 'repoPackage',
// name: 'repoConfig',
// name: 'repoSearch',
// name: 'repoToken',
// name: 'userCenter',
