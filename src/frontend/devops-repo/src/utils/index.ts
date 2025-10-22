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
import { RepoParams, RepoItem, RepoNameEnum, PromiseFn, Artifact } from '@/utils/vue-ts';
import { Message } from 'bkui-vue';
import { COMPOSITE_CATEGORY, FILE_TYPE, GENERIC_REPO, LOCAL_CATEGORY, REPO_TYPE_ICON, UNKNOW_FILE_EXT } from './conf';

export const IS_CI_MODE = process.env.VUE_APP_MODE_CONFIG === 'ci';

export function classes(dynamicCls: object, constCls = ''): string {
  return Object.entries(dynamicCls).filter(entry => entry[1])
    .map(entry => entry[0])
    .join(' ')
    .concat(constCls ? ` ${constCls}` : '');
}

export function throttle(func: Function, interval = 1000) {
  let lastFunc: number;
  let lastRan: number;
  return function (...args: any[]) {
    if (!lastRan) {
      func(...args);
      lastRan = Date.now();
    } else {
      clearTimeout(lastFunc);
      lastFunc = setTimeout(() => {
        if ((Date.now() - lastRan) >= interval) {
          func(...args);
          lastRan = Date.now();
        }
      }, interval - (Date.now() - lastRan));
    }
  };
}

export function replaceRepoName(name: string, t: any): string {
  if (!IS_CI_MODE) return name;
  switch (name) {
    case RepoNameEnum.CUSTOM:
      return t('custom');
    case RepoNameEnum.PIPELINE:
      return t('pipeline');
    default:
      return name;
  }
}
export function getRepoTypeTheme(repo: RepoItem) {
  const isCustom = [RepoNameEnum.CUSTOM, RepoNameEnum.PIPELINE].includes(repo.name);
  let theme = '';
  let label = '';
  switch (true) {
    case IS_CI_MODE && isCustom:
      theme = 'success';
      label = 'innerType';
      break;
    case repo.configuration?.settings?.system:
      theme = 'primary';
      label = 'systemType';
      break;
    case repo.public:
      theme = 'warning';
      label = 'publicType';
      break;
  }
  return {
    theme,
    label,
  };
}

export function getRepoAdress(repo: RepoParams, projectId: string) {
  const { type, name } = repo;
  return `${location.origin}/${type.toLowerCase()}/${projectId}/${name}/`;
}

function prezero(num: string | number) {
  return num.toString().padStart(2, '0');
}

export function formatDate(ms: string | number) {
  if (!ms) return '--';
  const time = new Date(ms);
  return `${time.getFullYear()}-${prezero(time.getMonth() + 1)}-${prezero(time.getDate())} ${prezero(time.getHours())}:${prezero(time.getMinutes())}:${prezero(time.getSeconds())}`;
}

export function convertBytesToGb(bytes: number, decimals = 2) {
  if (bytes === 0 || !Number.isInteger(bytes)) return '0GB';
  return `${parseFloat((bytes / (1024 ** 3)).toFixed(decimals))}GB`;
};

export function formatSize(size: number, unitIndex = 0): string {
  const unitList = ['B', 'KB', 'MB', 'GB', 'TB'];
  const unit = unitList[unitIndex];
  if (size > 1024) {
    return formatSize(size / 1024, unitIndex + 1);
  }

  return `${unitIndex ? size.toFixed(2) : size}${unit}`;
}

export function getFileExt(fileName: string): string {
  if (typeof fileName !== 'string') return UNKNOW_FILE_EXT;
  return fileName.split('.').pop() ?? UNKNOW_FILE_EXT;
}

export const OPERATION = {
  MATCH: 'MATCH',
  EQ: 'EQ',
  PREFIX: 'PREFIX',
  NIN: 'NIN',
  IN: 'IN',
};

export const RELATION = {
  OR: 'OR',
  AND: 'AND',
};

export const SORT_TYPE = {
  DESC: 'DESC',
  ASC: 'ASC',
};

export function formatPath(fullPath?: string) {
  if (typeof fullPath !== 'string') return '/';
  return fullPath.endsWith('/') ? fullPath : `${fullPath}/`;
}

export function generateRule({
  sortProperties,
  sortType,
  relation = RELATION.AND,
  ruleMap,
  page = 1,
  pageSize = 10000,
}: any) {
  const hasSortField = !!(Array.isArray(sortProperties) || sortType);
  return {
    page: {
      pageNumber: page,
      pageSize,
    },
    ...(hasSortField ? {
      sort: {
        properties: sortProperties,
        direction: sortType,
      },
    } : {}),
    rule: generateRuleObject(ruleMap, relation),
  };
}

export function generateRuleObject(ruleMap: any, relation =  RELATION.AND) {
  return {
    rules: Object.keys(ruleMap).filter(field => typeof ruleMap[field][0] !== 'undefined')
      .map((field) => {
        const [value, operation = OPERATION.EQ] = ruleMap[field];
        return {
          field,
          value,
          operation,
        };
      }),
    relation,
  };
}

export function formatTreeNode(item: any, parentPath: string): Artifact {
  return {
    ...item,
    children: [],
    displayName: item.metadata?.displayName ?? item.name,
    parentPath,
    isOpen: false,
  };
}
export function formatArtifactNode(item: any, parentPath: string): Artifact {
  return {
    ...item,
    children: [],
    displayName: item.metadata?.displayName ?? item.name,
    parentPath,
  };
}

export async function copyToClipboard(str: string, message: string) {
  try {
    if (navigator.clipboard) {
      await navigator.clipboard.writeText(str);
    } else {
      const input = document.createElement('input');
      document.body.appendChild(input);
      input.setAttribute('value', str);
      input.select();
      if (document.execCommand('copy')) {
        document.execCommand('copy');
      }
      document.body.removeChild(input);
    }
    Message({
      theme: 'success',
      message,
    });
  } catch (error) {
    console.log(error);
  }
}

export function getPath(fullPath?: string): string {
  if (!fullPath) return '';
  return fullPath.substring(0, fullPath.lastIndexOf('/'));
};

export function getFileName(fullPath?: string): string {
  if (!fullPath) return '';
  return fullPath.substring(fullPath.lastIndexOf('/') + 1);
};

export function getIconNameByExt(ext: string): string {
  return FILE_TYPE[ext] ?? ext;
}

export function getIconNameByFileName(fileName: string): string {
  const fileExt = getFileExt(fileName);
  return FILE_TYPE[fileExt] ?? fileExt;
}

export function getIconNameByRepoType(repoType = 'placeholder'): string {
  return REPO_TYPE_ICON[repoType] ?? repoType;
}

export function asyncAction<T>(action: PromiseFn<T>, msg: string) {
  return async (...args: any[]) => {
    let message = msg;
    let theme = 'success';
    try {
      const res = await action(...args);
      return res;
    } catch (error: any) {
      theme = 'error';
      message = error.message ?? error;
    } finally {
      Message({
        theme,
        message,
      });
    }
  };
}

export function getRepoParams(projectId: string, repoFormData: RepoParams) {
  const { type, name, description, system } = repoFormData;
  const isGeneric = type.toUpperCase() === GENERIC_REPO;
  let interceptors;
  if (isGeneric) {
    const downloadTypeList = ['mobile', 'web'] as const;
      type DownloadTypeKey = typeof downloadTypeList[number];
      interceptors = downloadTypeList.reduce((acc: any[], downloadType: string) => {
        const { enable, filename, metadata } = repoFormData[downloadType as DownloadTypeKey];
        if (enable) {
          acc.push({
            type: downloadType.toUpperCase(),
            rules: {
              filename,
              metadata,
            },
          });
        }
        return acc;
      }, []);
  };
  return {
    projectId,
    name,
    description,
    public: repoFormData.public,
    type: type.toUpperCase(),
    category: isGeneric ? LOCAL_CATEGORY : COMPOSITE_CATEGORY,
    configuration: {
      type: 'composite',
      settings: {
        system,
        interceptors,
      },
    },
  };
}
