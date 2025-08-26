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
import { useStore } from '@/store';
import {
  ACTIVE_OPERATION,
  CLOSE_OPERATION,
  COPY,
  CREATE_FOLDER,
  DELETE,
  DELETE_PACKAGE_VERSION,
  DELETE_REPO,
  GET_ARTIFACTORY_URL,
  GET_DOWNLOAD_URL,
  GET_FOLDER_FILES_COUNT,
  MOVE,
  PACKAGE_UPGRADE,
  RENAME,
  SCAN,
  SHARE_ARTIFACTORY,
} from '@/store/constants';
import { getPath } from '@/utils';
import { LEVEL_ENUM, MAVEN_REPO } from '@/utils/conf';
import { Artifact, OperateName, Operation, PromiseOr, RepoItem, RepoTreeParam } from '@/utils/vue-ts';
import { Message } from 'bkui-vue';
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import useRouteParams from './useRouteParam';

const PIPELINE = 'pipeline';

export function useArtifactOperation(artifact: Artifact, isSearching = false) {
  const store = useStore();
  const repoParams = useRouteParams();
  const { t } = useI18n();
  const { permission } = store.state;
  const isApp = !artifact.folder && /\.[ipa|apk|jar]$/.test(artifact.name);
  const isPipelineRepo = computed(() => repoParams.value.repoName === PIPELINE);

  function renameArtifact({
    projectId,
    repoName,
    fullPath,
    newFullPath,
  }: Record<string, any>) {
    return store.dispatch(RENAME, {
      projectId,
      repoName,
      fullPath,
      newFullPath,
    });
  }

  function moveArtifact(params: Record<string, any>) {
    return store.dispatch(MOVE, params);
  }

  function copyArtifact(params: Record<string, any>) {
    return store.dispatch(COPY, params);
  }

  function deleteArtifact({
    projectId,
    repoName,
    fullPath,
  }: Record<string, any>) {
    return store.dispatch(DELETE, {
      projectId,
      repoName,
      fullPath,
    });
  }
  let operations: Operation[] = [
    {
      id: OperateName.DOWNLOAD,
      name: OperateName.DOWNLOAD,
      show: true,
      before: async (row: Artifact) => {
        try {
          const downloadUrl = await store.dispatch(GET_DOWNLOAD_URL, {
            ...repoParams.value,
            repoName: row.repoName ?? repoParams.value.repoName,
            fullPath: encodeURIComponent(row.fullPath),
          });
          window.open(`/web${downloadUrl}`, '_self');
        } catch (e: any) {
          const message = e.status === 403 ? t('fileDownloadError') : t('fileError');
          Message({
            theme: 'error',
            message,
          });
        } finally {
          return false;
        }
      },
    },
    {
      id: OperateName.SHARE,
      name: OperateName.SHARE,
      message: 'shareSuccess',
      show: !artifact.folder,
      callback: async (shareParams: object) => store.dispatch(SHARE_ARTIFACTORY, {
        ...repoParams.value,
        repoName: artifact.repoName ?? repoParams.value.repoName,
        ...shareParams,
      }),
    },
    {
      id: OperateName.DETAIL,
      name: OperateName.DETAIL,
      show: true,
    },
  ];
  if (isSearching) { // 制品搜索只提供下载与分享
    return operations.slice(0, 3);
  }
  if (!isPipelineRepo.value) {
    operations = [
      ...operations,
      {
        id: OperateName.RENAME,
        name: OperateName.RENAME,
        message: 'renameSuccess',
        show: permission.edit,
        callback: async (newFileName: string) => {
          const dir = getPath(artifact.fullPath);
          const newFullPath = `${dir}/${newFileName}`;
          await renameArtifact({
            ...repoParams.value,
            fullPath: encodeURIComponent(artifact.fullPath),
            newFullPath: encodeURIComponent(newFullPath),
          });

          if (artifact.folder) {
            store.state.repoMap.set(newFullPath, {
              ...artifact,
              name: newFileName,
              displayName: newFileName,
              fullPath: newFullPath,
            });
            store.state.repoMap.delete(artifact.fullPath);
          }
        },
      },
      {
        id: OperateName.MOVE,
        name: OperateName.MOVE,
        message: 'moveSuccess',
        show: permission.write,
        callback: async (params: any) => {
          const { projectId, repoName } = repoParams.value;
          await moveArtifact({
            srcProjectId: projectId,
            srcFullPath: artifact.fullPath,
            srcRepoName: repoName,
            destProjectId: projectId,
            destRepoName: repoName,
            destFullPath: params.destFullPath,
            overwrite: params.overwrite,
          });
          if (artifact.folder) {
            const fullPath = `${params.destFullPath}/${artifact.displayName}`;
            store.state.repoMap.set(fullPath, {
              ...artifact,
              fullPath,
              path: params.destFullPath,
              parentPath: params.destFullPath,
            });
            store.state.repoMap.delete(artifact.fullPath);
          }
        },
      },
      {
        id: OperateName.COPY,
        name: OperateName.COPY,
        message: 'copySuccess',
        show: permission.write,
        callback: async (params: any) => {
          const { projectId, repoName } = repoParams.value;
          await copyArtifact({
            srcProjectId: projectId,
            srcFullPath: artifact.fullPath,
            srcRepoName: repoName,
            destProjectId: projectId,
            destRepoName: repoName,
            destFullPath: params.destFullPath,
            overwrite: params.overwrite,
          });
          if (artifact.folder) {
            const fullPath = `${params.destFullPath}/${artifact.displayName}`;
            store.state.repoMap.set(fullPath, {
              ...artifact,
              fullPath,
              path: params.destFullPath,
              parentPath: params.destFullPath,
            });
          }
        },
      },
      {
        id: OperateName.DELETE,
        name: OperateName.DELETE,
        show: permission.delete,
        message: 'deleteSuccess',
        confirmMessage: t(artifact.folder ? 'deleteFolderTitle' : 'deleteArtifactTitle', [artifact.fullPath]),
        before: async (row: Artifact) => {
          if (row.folder) {
            return await store.dispatch(GET_FOLDER_FILES_COUNT, {
              ...repoParams.value,
              fullPath: row.fullPath,
            });
          }
          return 0;
        },
        callback: async () => {
          await deleteArtifact({
            ...repoParams.value,
            fullPath: encodeURIComponent(artifact.fullPath),
          });
          store.state.repoMap.delete(artifact.fullPath);
        },
      },
    ];
  }
  if (isApp) {
    operations = [
      ...operations,
      {
        id: OperateName.SECSCAN,
        name: OperateName.SECSCAN,
        show: true,
        message: t('QueueSecScanSuccess'),
        callback: async (model: any) => store.dispatch(SCAN, {
          id: model.scanType,
          projectId: repoParams.value.projectId,
          repoType: repoParams.value.repoType.toUpperCase(),
          repoName: repoParams.value.repoName,
          fullPath: artifact.fullPath,
        }),
      },
    ];
  }

  return operations.filter(opertaion => opertaion.show);;
}

export function useFolderOperation(repo: Artifact, repoParams: RepoTreeParam) {
  const store = useStore();
  const { permission } = store.state;
  const isPipelineRepo = computed(() => repoParams.repoName === PIPELINE);

  const operations: Operation[] = [
    {
      id: OperateName.DETAIL,
      name: OperateName.DETAIL,
      show: !repo.isRoot,
    },
    {
      id: OperateName.CREATE_FOLDER,
      name: OperateName.CREATE_FOLDER,
      message: 'createFolerSuccess',
      show: permission.write && !isPipelineRepo.value,
      callback: async (fullPath: string) => {
        await store.dispatch(CREATE_FOLDER, {
          ...repoParams,
          fullPath: encodeURIComponent(fullPath),
        });
        const name = fullPath.substring(fullPath.lastIndexOf('/') + 1);
        store.state.repoMap.set(fullPath, {
          async: true,
          children: [],
          displayName: name,
          folder: true,
          fullPath,
          isOpen: false,
          metadata: {},
          name,
          parentPath: repo.fullPath,
          ...repoParams,
        });
      },
    },
    {
      id: OperateName.UPLOAD,
      name: OperateName.UPLOAD,
      message: 'uploadSuccess',
      show: permission.write && !isPipelineRepo.value,
    },
  ];
  return operations.filter(opertaion => opertaion.show);
}

export function useOperation<T>() {
  const store = useStore();
  async function activeOperation(operation: Operation, artifact: T, done?: PromiseOr) {
    const next = await operation.before?.(artifact);
    if (next !== false) {
      store.commit(ACTIVE_OPERATION, {
        isShow: operation.id !== OperateName.DETAIL,
        operation,
        artifact,
        done,
      });
    }
  }

  function closeOperation() {
    store.commit(CLOSE_OPERATION);
  }

  return {
    activeOperation,
    closeOperation,
  };
}

export function useRepoOperation(repo: RepoItem) {
  const router = useRouter();
  const routeParams = useRouteParams();
  const { t } = useI18n();
  const store = useStore();
  const operationList: Operation[] = [{
    id: OperateName.SETTING,
    name: OperateName.SETTING,
    show: true,
    callback() {
      router.push({
        name: 'repoConfig',
        params: {
          repoType: repo.type.toLowerCase(),
        },
        query: {
          repoName: repo.name,
        },
      });
    },
  }, {
    id: OperateName.DELETE,
    name: OperateName.DELETE,
    message: 'deleteSuccess',
    confirmMessage: t('deleteRepoTitle', [repo.displayName]),
    show: repo.isGenericRepo ?? false,
    callback: async () => store.dispatch(DELETE_REPO, {
      projectId: routeParams.value.projectId,
      name: repo.name,
    }),
  }];
  return operationList.filter(operation => operation.show);
}

export function usePackageOperation(repoType: string, packageName: string, version: any): Operation[] {
  const { t } = useI18n();
  const store = useStore();
  const routeParams = useRouteParams();
  const { stageTag = [], name } = version;
  const downloadPackage = useDownloadPackage();

  const packageOperations = [{
    id: OperateName.UPGRADE,
    name: t(OperateName.UPGRADE),
    show: store.state.permission.edit,
    message: t('artifactUpgrade') + t('success'),
    disabled: stageTag.includes(LEVEL_ENUM.RELEASE),
    callback: (model: any) => store.dispatch(PACKAGE_UPGRADE, {
      ...routeParams.value,
      packageKey: routeParams.value.package,
      version: name,
      tag: model.tag,
    }),
  }, {
    id: OperateName.SECSCAN,
    name: t(OperateName.SECSCAN),
    show: repoType === MAVEN_REPO,
    message: t('QueueSecScanSuccess'),
    callback: async (model: any) => store.dispatch(SCAN, {
      id: model.scanType,
      version: name,
      isPackage: true,
      packageKey: routeParams.value.package,
      projectId: routeParams.value.projectId,
      repoName: routeParams.value.repoName,
    }),
  }, {
    id: OperateName.DOWNLOAD,
    name: t(OperateName.DOWNLOAD),
    show: true,
    before: downloadPackage,
  }, {
    id: OperateName.DELETE,
    name: t(OperateName.DELETE),
    show: store.state.permission.delete,
    message: 'deleteSuccess',
    confirmMessage: t('deleteVersionTitle', [name]),
    callback: async () => store.dispatch(DELETE_PACKAGE_VERSION, {
      ...routeParams.value,
      packageKey: routeParams.value.package,
      version: name,
    }),
  }].filter(opertion => opertion.show);

  return packageOperations;
}

export function useDownloadPackage() {
  const routeParams = useRouteParams();
  const { t } = useI18n();
  const store = useStore();

  async function downloadPackage(version: any) {
    try {
      const url = await store.dispatch(GET_ARTIFACTORY_URL, {
        ...routeParams.value,
        packageKey: routeParams.value.package,
        version: version.name,
      });
      window.open(`/web${url}`, '_self');
    } catch (e: any) {
      const message = t(e.status === 403 ? 'fileDownloadError' : 'fileError');
      Message({
        theme: 'error',
        message,
      });
    }
  }
  return downloadPackage;
}
