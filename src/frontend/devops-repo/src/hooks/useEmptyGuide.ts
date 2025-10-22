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
import { EmptyGuide } from '@/utils/vue-ts';
import { useI18n } from 'vue-i18n';
import useRouteParam from './useRouteParam';

export function useEmptyGuide(): EmptyGuide[] {
  const routeParams = useRouteParam();
  switch (routeParams.value.repoType?.toLowerCase()) {
    case HELM_REPO:
      return useHelmEmptyGuide();
    case GIT_REPO:
      return useGitEmptyGuide();
    case NPM_REPO:
      return useNpmEmptyGuide();
    case MAVEN_REPO:
      return useMavenEmptyGuide();
    case DOCKER_REPO:
      return useDockerEmptyGuide();
    default:
      return [];
  }
}

export function useHelmEmptyGuide() {
  const { t } = useI18n();
  const routeParams = useRouteParam();
  return [
    {
      title: t('setCredential'),
      main: [
        {
          subTitle: t('execCmdConfigure'),
          codeList: [
            `helm repo add --username ${routeParams.value.userName} --password <PERSONAL_ACCESS_TOKEN> ${routeParams.value.repoName} "${routeParams.value.repoUrl}"`,
          ],
        },
        {
          subTitle: t('updateRepoInfo'),
          codeList: [
            'helm repo update',
          ],
        },
      ],
    },
    {
      title: t('push'),
      main: [
        {
          subTitle: t('curlChart'),
          codeList: [
            `curl -F "chart=@<FILE_NAME>" -u ${routeParams.value.userName}: <PERSONAL_ACCESS_TOKEN> ${location.origin}/${routeParams.value.repoType}/api/${routeParams.value.projectId}/${routeParams.value.repoName}/charts`,
          ],
        },
        {
          subTitle: t('curlChartPe'),
          codeList: [
            `curl -F "prov=@<PROV_FILE_NAME>" -u ${routeParams.value.userName}:<PERSONAL_ACCESS_TOKEN> ${location.origin}/${routeParams.value.repoType}/api/${routeParams.value.projectId}/${routeParams.value.repoName}/charts`,
          ],
        },
      ],
    },
    {
      title: t('pull'),
      main: [
        {
          subTitle: t('pull'),
          codeList: [
            `helm install ${routeParams.value.repoName}/${routeParams.value.packageDisplayName}`,
          ],
        },
      ],
    },
  ];
}

export function useGitEmptyGuide() {
  return [];
}


export function useDockerEmptyGuide() {
  const { t } = useI18n();
  const store = useStore();
  const routeParams = useRouteParam();
  return [
    {
      title: t('setCredential'),
      main: [
        {
          subTitle: t('loginRepo'),
          codeList: [`docker login -u ${routeParams.value.userName} -p <PERSONAL_ACCESS_TOKEN> ${store.state.domain.docker}`],
        },
      ],
    },
    {
      title: t('push'),
      main: [
        {
          subTitle: t('cmdTag'),
          codeList: [`docker tag <LOCAL_IMAGE_TAG> ${store.state.domain.docker}/${routeParams.value.projectId}/${routeParams.value.repoName}/${routeParams.value.packageDisplayName}`],
        },
        {
          subTitle: t('cmdPush'),
          codeList: [`docker push ${store.state.domain.docker}/${routeParams.value.projectId}/${routeParams.value.repoName}/${routeParams.value.packageDisplayName}`],
        },
      ],
    },
    {
      title: t('download'),
      main: [
        {
          subTitle: t('cmdPull'),
          codeList: [`docker pull ${store.state.domain.docker}/${routeParams.value.projectId}/${routeParams.value.repoName}/${routeParams.value.packageDisplayName}`],
        },
      ],
    },
  ];
}

export function useNpmEmptyGuide() {
  const { t } = useI18n();
  const store = useStore();
  const routeParams = useRouteParam();
  return [
    {
      title: t('setCredential'),
      main: [
        {
          subTitle: t('usePersonalToken'),
        },
        {
          subTitle: t('setNpmrc'),
          codeList: [
            `registry=${store.state.domain.npm}/${routeParams.value.projectId}/${routeParams.value.repoName}/`,
            'always-auth=true',
            `//${store.state.domain.npm.split('//')[1]}/${routeParams.value.projectId}/${routeParams.value.repoName}/:username=${routeParams.value.userName}`,
            `//${store.state.domain.npm.split('//')[1]}/${routeParams.value.projectId}/${routeParams.value.repoName}/:_password=<BASE64_ENCODE_PERSONAL_ACCESS_TOKEN>`,
            `//${store.state.domain.npm.split('//')[1]}/${routeParams.value.projectId}/${routeParams.value.repoName}/:email=<EMAIL>`,
          ],
        },
        {
          subTitle: t('generateBase64'),
        },
        {
          subTitle: t('execShell'),
          codeList: [
            'node -e "require(\'readline\') .createInterface({input:process.stdin,output:process.stdout,historySize:0}) .question(\'PAT> \',p => { b64=Buffer.from(p.trim()).toString(\'base64\');console.log(b64);process.exit(); })"',
          ],
        },
        {
          subTitle: t('copyToken'),
        },
        {
          subTitle: t('replaceToken'),
        },
        {
          subTitle: t('cmdSetCredentail'),
        },
        {
          subTitle: t('setNpmRegistry'),
          codeList: [
            `npm config set registry ${store.state.domain.npm}/${routeParams.value.projectId}/${routeParams.value.repoName}/`,
          ],
        },
        {
          subTitle: t('useCreLogin'),
          codeList: [
            'npm login',
          ],
        },
      ],
    },
    {
      title: t('push'),
      main: [
        {
          subTitle: t('cmdCopy'),
          codeList: ['npm publish'],
        },
      ],
    },
    {
      title: t('download'),
      main: [
        {
          subTitle: t('pullArtifact'),
          codeList: [`npm install ${routeParams.value.packageDisplayName}`],
        },
        {
          subTitle: t('setRegistryPull'),
          codeList: [`npm install ${routeParams.value.packageDisplayName} --registry ${store.state.domain.npm}/${routeParams.value.projectId}/${routeParams.value.repoName}/`],
        },
      ],
    },
  ];
}

export function useMavenEmptyGuide() {
  const { t } = useI18n();
  const routeParams = useRouteParam();
  return [
    {
      title: t('setCredential'),
      main: [
        {
          subTitle: t('setXmlPriority'),
          codeList: [
            '<servers>',
            '       <server>',
            `               <id>${routeParams.value.projectId}-${routeParams.value.repoName}</id>`,
            `               <username>${routeParams.value.userName}</username>`,
            '               <password><PERSONAL_ACCESS_TOKEN></password>',
            '       </server>',
            '</servers>',
          ],
        },
        {
          subTitle: t('setGradleProperties'),
          codeList: [
            `cpackUrl=${routeParams.value.repoUrl}`,
            `cpackUsername=${routeParams.value.userName}`,
            'cpackPassword=<PERSONAL_ACCESS_TOKEN>',
          ],
        },
      ],
    },
    {
      title: t('setSourceUrl'),
      main: [
        {
          subTitle: t('setConfSettingFile'),
          codeList: [
            '<mirror>',
            `       <id>${routeParams.value.projectId}-${routeParams.value.repoName}</id>`,
            `       <name>${routeParams.value.repoName}</name>`,
            `       <url>${routeParams.value.repoUrl}/</url>`,
            '       <mirrorOf>central</mirrorOf>',
            '</mirror>',
          ],
        },
        {
          subTitle: t('setPomFile'),
          codeList: [
            '<repository>',
            `       <id>${routeParams.value.projectId}-${routeParams.value.repoName}</id>`,
            `       <url>${routeParams.value.repoUrl}/</url>`,
            '</repository>',
          ],
        },
      ],
    },
    {
      title: t('push'),
      main: [
        {
          subTitle: t('addPomFile'),
          codeList: [
            '<distributionManagement>',
            '       <repository>',
            '               <!--id值与配置的server id 一致-->',
            `               <id>${routeParams.value.projectId}-${routeParams.value.repoName}</id>`,
            `               <name>${routeParams.value.repoName}</name>`,
            `               <url>${routeParams.value.repoUrl}/</url>`,
            '       </repository>',
            '</distributionManagement>',
          ],
        },
        {
          subTitle: t('useXmlPush'),
          codeList: [
            'mvn clean deploy',
          ],
        },
        {
          subTitle: t('setBuildeGradle'),
          codeList: [
            'plugins {',
            '    id "maven-publish"',
            '}',
            'publishing {',
            '    publications {',
            '        maven(MavenPublication) {',
            '            groupId = "com.company.group"',
            '            version = "1.0"',
            '            from components.java',
            '        }',
            '    }',
            '    repositories {',
            '        maven {',
            '            url = "${cpackUrl}"',
            '            credentials {',
            '                username = "${cpackUsername}"',
            '                password = "${cpackPassword}"',
            '            }',
            '        }',
            '    }',
            '}',
          ],
        },
        {
          subTitle: t('setBuildeGradelePush'),
          codeList: [
            'gradle publish',
          ],
        },
        {
          subTitle: t('addDSL'),
          codeList: [
            'plugins {',
            '    `maven-publish`',
            '}',
            'publishing {',
            '    publications {',
            '        create<MavenPublication>("maven") {',
            '            groupId = "com.company.group"',
            '            version = "1.0"',
            '            from(components["java"])',
            '        }',
            '    }',
            '    repositories {',
            '        maven {',
            '            val cpackUrl: String by project',
            '            val cpackUsername: String by project',
            '            val cpackPassword: String by project',
            '            url = uri(cpackUrl)',
            '            credentials {',
            '                username = cpackUsername',
            '                password = cpackPassword',
            '            }',
            '        }',
            '    }',
            '}',
          ],
        },
        {
          subTitle: t('addDSLPush'),
          codeList: [
            'gradle publish',
          ],
        },
      ],
    },
    {
      title: t('pull'),
      main: [
        {
          subTitle: t('setXmlConf'),
          codeList: [
            '<profiles>',
            '       <profile>',
            '               <id>repository proxy</id>',
            '               <activation>',
            '                       <activeByDefault>true</activeByDefault>',
            '               </activation>',
            '               <repositories>',
            '                       <repository>',
            `                               <id>${routeParams.value.projectId}-${routeParams.value.repoName}</id>`,
            `                               <name>${routeParams.value.repoName}</name>`,
            `                               <url>${routeParams.value.repoUrl}/</url>`,
            '                               <releases>',
            '                                       <enabled>true</enabled>',
            '                               </releases>',
            '                               <snapshots>',
            '                                       <enabled>true</enabled>',
            '                               </snapshots>',
            '                       </repository>',
            '               </repositories>',
            '       </profile>',
            '</profiles>',
          ],
        },
        {
          subTitle: t('useXmlPull'),
          codeList: [
            'mvn clean package',
          ],
        },
        {
          subTitle: t('useGroovy'),
          codeList: [
            'repositories {',
            '    maven {',
            '        url = "${cpackUrl}"',
            '        credentials {',
            '            username = "${cpackUsername}"',
            '            password = "${cpackPassword}"',
            '        }',
            '    }',
            '}',
          ],
        },
        {
          subTitle: t('useGroovyPull'),
          codeList: [
            'gradle dependencies',
          ],
        },
        {
          subTitle: t('useDSLBuildeGradle'),
          codeList: [
            'repositories {',
            '    maven {',
            '        val cpackUrl: String by project',
            '        val cpackUsername: String by project',
            '        val cpackPassword: String by project',
            '        url = uri(cpackUrl)',
            '        credentials {',
            '            username = cpackUsername',
            '            password = cpackPassword',
            '        }',
            '    }',
            '}',
          ],
        },
        {
          subTitle: t('useDSLBuildeGradlePull'),
          codeList: [
            'gradle dependencies',
          ],
        },
      ],
    },
  ];
}
