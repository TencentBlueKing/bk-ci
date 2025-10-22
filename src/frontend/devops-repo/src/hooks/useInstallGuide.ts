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
import { DOCKER_REPO, GIT_REPO, HELM_REPO, MAVEN_REPO, NPM_REPO } from '@/utils/conf';
import { GuideStep } from '@/utils/vue-ts';
import { useI18n } from 'vue-i18n';
import useRouteParams from './useRouteParam';

export function useInstallGuide(packageDetail: any): GuideStep[] {
  const routeParams = useRouteParams();
  switch (routeParams.value.repoType?.toLowerCase()) {
    case HELM_REPO:
      return useHelmInstallGuide();
    case GIT_REPO:
      return useGitInstallGuide();
    case DOCKER_REPO:
      return useDockerInstallGuide();
    case NPM_REPO:
      return useNpmInstallGuide();
    case MAVEN_REPO:
      return useMavenInstallGuide(packageDetail);
    default:
      return [];
  }
}

export function useHelmInstallGuide() {
  const routeParams = useRouteParams();
  const { t } = useI18n();
  return [
    {
      subTitle: t('manualConfig'),
      codeList: [
        `helm repo add --username ${routeParams.value.userName} --password <PERSONAL_ACCESS_TOKEN> ${routeParams.value.repoName} "${routeParams.value.repoUrl}"`,
      ],
    },
    {
      subTitle: t('updateRepo'),
      codeList: [
        'helm repo update',
      ],
    },
    {
      subTitle: t('pull'),
      codeList: [
        `helm fetch ${routeParams.value.repoName}/${routeParams.value.packageDisplayName}`,
      ],
    },
  ];
}

export function useGitInstallGuide() {
  return [];
}

export function useDockerInstallGuide() {
  const store = useStore();
  const { t } = useI18n();
  const routeParams = useRouteParams();
  return [
    {
      subTitle: t('useSubTips'),
      codeList: [
        `docker pull ${store.state.domain.docker}/${routeParams.value.projectId}/${routeParams.value.repoName}/${routeParams.value.packageDisplayName}:${routeParams.value.versionLabel}`,
      ],
    },
  ];
}

export function useNpmInstallGuide() {
  const { t } = useI18n();
  const store = useStore();
  const routeParams = useRouteParams();
  return [
    {
      subTitle: t('pullArtifact'),
      codeList: [
        `npm install ${routeParams.value.packageDisplayName}@${routeParams.value.versionLabel}`,
      ],
    },
    {
      subTitle: t('setRegistryPull'),
      codeList: [
        `npm install ${routeParams.value.packageDisplayName}@${routeParams.value.versionLabel} --registry ${store.state.domain.npm}/${routeParams.value.projectId}/${routeParams.value.repoName}/`,
      ],
    },
  ];
}

export function useMavenInstallGuide(packageDetail: any) {
  const { t } = useI18n();
  const routeParams = useRouteParams();

  return [
    {
      subTitle: t('ApacheMaven'),
      codeList: [
        `<dependency>
  <groupId>${packageDetail.value?.basic.groupId}</groupId>
  <artifactId>${packageDetail.value?.basic.artifactId}</artifactId>
  <version>${routeParams.value.versionLabel}</version>
</dependency>`,
      ],
    },
    {
      subTitle: t('gradleGroovyDSL'),
      codeList: [
        `implementation '${packageDetail.value?.basic.groupId}:${packageDetail.value?.basic.artifactId}:${routeParams.value.versionLabel}'`,
      ],
    },
    {
      subTitle: t('gradleKotlinDSL'),
      codeList: [
        `implementation("${packageDetail.value?.basic.groupId}:${packageDetail.value?.basic.artifactId}:${routeParams.value.versionLabel}")`,
      ],
    },
  ];
}

