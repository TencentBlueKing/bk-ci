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
import { OPERATION } from '.';

export const LOG_REPO_NAME = 'log';
export const REPORT_REPO_NAME = 'report';

export type PromiseFn<T> = (...args: any[]) => Promise<T>;
export type PromiseOr = ((...args: any[]) => Promise<any>) | ((...args: any[]) => void);
export enum RepoNameEnum {
  CUSTOM = 'custom',
  PIPELINE = 'pipeline',
  REPORT = 'report',
  LOG = 'log',
};
export type ObjectMap = Record<string, any>;

export interface CommonOption {
  id: string
  name: string
  tips?: string
  selected?: boolean
}

export interface PageApiResponse {
  count: number
  page: number
  pageNumber: number
  pageSize: number
  records: RepoItem[]
  totalPages: number
  totalRecords: number
}


interface WebHook {
  webHookList: []
}
interface RepoConfSettings {
  system?: boolean
}

interface RepoConf {
  type: string
  webHook: WebHook
  settings: RepoConfSettings
}

export interface RepoItem {
  category: string
  configuration: RepoConf
  createdBy: string
  createdDate: string
  description: null
  lastModifiedBy: string
  lastModifiedDate: string
  name: RepoNameEnum,
  projectId: string
  public: boolean
  quota?: number
  storageCredentialsKey?: string
  type: string
  used: number,
  isGenericRepo?: boolean,
  displayName?: string
}

export interface User {
  userId: string
  name: string
}

export interface UserInfo {
  name?: string
  username: string
  admin?: boolean
  manage?: boolean
  email?: string
  phone?: string
}

export type UserInfoFields = 'username' | 'admin' | 'manage' | 'email' | 'phone' | 'name';

export interface Project {
  id: string
  name: string
  [key: string]: string | number
}

export interface Artifact {
  displayName: string
  children: Artifact[]
  folder: boolean
  metadata?: object
  fullPath: string
  parentPath?: string
  [key: string]: any
}

export interface DownloadType {
  enable: boolean
  filename: string
  metadata: string
}

export interface RepoParams {
  type: string
  public: boolean
  system: boolean
  name: string
  address?: string
  mobile: DownloadType
  web: DownloadType
  description: string
  repoType?: string
}

export interface PaginationConfType {
  limit: number
  current: number
  count: number,
  align?: string,
}

export type PaginationConfFieldType = 'limit' | 'current' | 'count';

export interface Operation {
  id: string
  name: string
  message?: string
  confirmMessage?: string
  before?: PromiseOr
  callback?: PromiseOr
  show: boolean
  disabled?: boolean
}

export interface OperationDialogProps {
  isShow: boolean
  filesCount?: number
  operation?: Operation
  artifact?: Artifact
  done?: PromiseOr
}

export interface MetaData {
  key: string | number
  value: string | number
}

export interface RepoTreeParam {
  projectId: string
  repoName: string
}

export enum OperateName {
  DOWNLOAD = 'download',
  SHARE = 'share',
  DETAIL = 'detail',
  RENAME = 'rename',
  MOVE = 'move',
  COPY = 'copy',
  DELETE = 'delete',
  SECSCAN = 'secScan',
  CREATE_FOLDER = 'createFolder',
  UPLOAD = 'uploadFile',
  SETTING = 'setting',
  UPGRADE = 'upgrade'
}

export interface MoveOrCopyParam {
  srcFullPath: string
  srcProjectId: string
  srcRepoName: string
  destFullPath: string
  destProjectId: string
  destRepoName: string
  overwrite?: boolean
}

export interface FileState {
  file: Blob | null
  name: string
  ext: string
  type: string
  size: string | number
  overwrite: boolean
  expires?: number
}

export interface Permission {
  write: boolean
  edit: boolean
  delete: boolean
}

export interface Rule {
  field: string
  value: any,
  operation: keyof typeof OPERATION
}

export interface SearchArtifactParam {
  projectId: string
  repoType: string
  repoName: string
  property?: string[]
  direction: string
  packageName?: string,
  limit: number,
  current: number,
}

export interface Token {
  name: string
  [key: string]: string | number
  createdDate: string
  expiredDate: string
}

export interface EmptyGuide {
  title: string
  main: GuideStep[]
}

export interface GuideStep {
  subTitle: string
  codeList?: string[]
}
