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
import { defineComponent, ref, onBeforeMount } from 'vue';
import { Button, Loading, Tab } from 'bkui-vue';
import { useI18n } from 'vue-i18n';
import RepoForm from '@/components/RepoForm';
import { RepoParams } from '@/utils/vue-ts';
import { asyncAction, getRepoAdress } from '@/utils';
import { useStore } from '@/store';
import { FETCH_REPO_INFO, UPDATE_REPO_INFO } from '@/store/constants';
import { useRouteParams } from '@/hooks';

const { TabPanel } = Tab;
export default defineComponent({
  setup() {
    const store = useStore();
    const { t } = useI18n();
    const activeTab = ref('repoBaseInfo');
    const loading = ref(true);
    const submitting = ref(false);
    const repo = ref<RepoParams>();
    const repoParams = useRouteParams();

    onBeforeMount(() => {
      fetchRepoConfig();
    });

    async function fetchRepoConfig() {
      try {
        loading.value = true;
        const { projectId } = repoParams.value;
        const repoInfo = await store.dispatch(FETCH_REPO_INFO, repoParams.value);
        const { configuration, ...restInfo } = repoInfo;
        const { settings: { interceptors, system } } = configuration;
        let downloadInfo = {
          mobile: {
            enable: false,
            filename: '',
            metadata: '',
          },
          web: {
            enable: false,
            filename: '',
            metadata: '',
          },
        };

        if (Array.isArray(interceptors)) {
          downloadInfo = interceptors.reduce((acc, i) => {
            acc[i.type.toLowerCase()] = {
              enable: true,
              ...i.rules,
            };
            return acc;
          }, downloadInfo);
        }

        repo.value = {
          ...restInfo,
          ...downloadInfo,
          system,
          address: getRepoAdress(repoInfo, projectId as string),
          repoType: repoInfo.type.toLowerCase(),
        };

        console.log(repo.value);
      } catch (error) {
        console.trace(error);
      } finally {
        loading.value = false;
      }
    }

    const handleSubmit = asyncAction(async () => {
      submitting.value = true;
      await store.dispatch(UPDATE_REPO_INFO, {
        ...repoParams.value,
        repoFormData: repo.value,
      });
      submitting.value = false;
    }, t('saveSuccess'));

    const panels = [
      {
        label: 'repoBaseInfo',
        name: 'repoBaseInfo',
        content: () => !loading.value && (
          <>
            <RepoForm
              v-model={repo.value}
              isEdit
            />
            <Button
              theme="primary"
              loading={submitting.value}
              disabled={submitting.value}
              onClick={handleSubmit}
            >
              {t('save')}
            </Button>
          </>
        ),
      },
    ];
    return () => (

        <Tab
          class='repo-config-tab'
          type='unborder-card'
          addable={false}
          active={activeTab.value}
        >
              {
                panels.map(item => (
                  <TabPanel
                    key={item.name}
                    name={item.name}
                    label={t(item.label)}
                  >
                    <Loading loading={loading.value}>
                      {item.content()}
                    </Loading>
                  </TabPanel>
                ))
              }
        </Tab>

    );
  },
});
