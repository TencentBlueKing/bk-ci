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
import { useRouteParams } from '@/hooks';
import router from '@/router';
import { useStore } from '@/store';
import { ADD_META_DATA, DELETE_META_DATA, GET_ARTIFACTORY_INFO } from '@/store/constants';
import { asyncAction, formatDate, formatSize } from '@/utils';
import { MetaData } from '@/utils/vue-ts';
import { Loading, Tab, Table, Button, Input, Form } from 'bkui-vue';
import { computed, defineComponent, onBeforeMount, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import CodeBox from './CodeBox';
import Icon from './Icon';
import SectionBox from './SectionBox';
import TokenDialog from './TokenDialog';

const  { TabPanel } = Tab;
const  { FormItem } = Form;
export default defineComponent({
  props: {
    projectId: {
      type: String,
      required: true,
    },
    name: {
      type: String,
    },
    repoName: {
      type: String,
    },
    fullPath: {
      type: String,
    },
    isFolder: Boolean,
  },
  setup(props) {
    const { t } = useI18n();
    const store = useStore();
    const activeTab = ref('baseInfo');
    const info = ref();
    const loading = ref(false);
    const adding = ref(false);
    const deleting = ref(false);
    const isCreateTokenDialogShow = ref(false);
    const showMetaDataPopup = ref(false);
    const repoParams = useRouteParams();
    const metaData = ref<MetaData>({
      key: '',
      value: '',
    });
    const tableData = ref<MetaData[]>([]);
    const wgetUrl = computed(() => [
      `wget --user=${repoParams.value.userName} --password=<PERSONAL_ACCESS_TOKEN> "${location.origin}/generic/${props.projectId}/${props.repoName}${props.fullPath}"`,
    ]);

    const columns = [
      {
        label: t('key'),
        field: 'key',
        width: 120,
      },
      {
        label: t('value'),
        field: 'value',
        render: ({ cell }: any) => <span class="text-overflow">{cell}</span>,
      },
      {
        label: () => (
          <p class="align-center link" onClick={toggleMetaDataPopup}>
            <Icon size="20" name="add" />
          </p>
        ),
        field: 'key',
        width: 60,
        alignContent: 'center',
        render: ({ cell }: any) => (
          <p class="meta-delete-icon link" onClick={() => handleDeleteMetaData(cell)}>
            <Icon name="delete" size="14" />
          </p>
        ),
      },
    ];
    const activeIndex = [0, 1];
    const panels = computed(() => {
      const infoBlock = [
        {
          name: t('baseInfo'),
          content: [
            'path',
            'size',
            'createdBy',
            'createdDate',
            'lastModifiedBy',
            'lastModifiedDate',
          ],
        },
        {
          name: 'Checksums',
          content: [
            'sha256',
            'md5',
          ],
        },
      ];
      const metaBlock = [{
        name: t('metaData'),
        content: [],
      }];
      if (props.isFolder) {
        infoBlock.pop();
      }
      const panels = [
        {
          name: 'baseInfo',
          label: 'baseInfo',
          content: () => (
            <>
              <SectionBox
                blockList={infoBlock}
                activeIndex={activeIndex}
              >
                {{
                  default: (item: any) => (
                    <section class="bk-repo-block">
                      {
                        item.content
                          .filter((field: string) => !props.isFolder || (props.isFolder && field !== 'size'))
                          .map((field: string) => (
                            <div key={info.value[field]} class="bk-repo-block-info-row">
                              <label>{t(field)}：</label>
                              <p title={info.value[field]} class="text-overflow">{info.value[field]}</p>
                            </div>
                          ))
                      }
                    </section>
                  ),
                }}
              </SectionBox>
              {
                !props.isFolder && <SectionBox
                  blockList={[{
                    name: 'cmdDownload',
                    content: '',
                  }]}
                  activeIndex={activeIndex}
                >
                  {{
                    default: () => (
                      <>
                        <p class="artifactory-token-tips">
                          <Button text theme='primary' onClick={toggleTokenDialog}>{t('createToken')}</Button>
                          {t('tokenSubTitle')}
                          <Button text theme='primary' onClick={goToken}>{t('token')}</Button>
                        </p>
                        <CodeBox codeList={wgetUrl.value} />
                      </>
                    ),
                  }}
                </SectionBox>
              }
            </>

          ),
        },
      ];
      if (!props.isFolder) {
        panels.push({
          name: 'metaData',
          label: 'metaData',
          content: () => (
            <SectionBox
              blockList={metaBlock}
              activeIndex={activeIndex}
            >
              {{
                default: () => (
                  <Table
                    class="metadata-table"
                    data={tableData.value}
                    columns={columns}
                  >
                    {{
                      empty: () => (
                        <div class="empty-meta-data-placeholder">
                          <Icon name="empty-data" size={22} />
                          <span>{t('noMetaData')}</span>
                          <Button text theme='primary' onClick={toggleMetaDataPopup}>{t('addNow')}</Button>
                        </div>
                      ),
                    }}
                  </Table>
                ),
              }}
            </SectionBox>
          ),
        });
      }
      return panels;
    });

    function toggleMetaDataPopup() {
      showMetaDataPopup.value = !showMetaDataPopup.value;
    }

    async function handleAddMetaData() {
      if (adding.value) return;
      const action = asyncAction(async () => {
        await store.dispatch(ADD_META_DATA, {
          projectId: props.projectId,
          repoName: props.repoName,
          fullPath: encodeURIComponent(props.fullPath!),
          body: {
            metadata: {
              [metaData.value.key]: metaData.value.value,
            },
          },
        });
        tableData.value = [
          ...tableData.value,
          metaData.value,
        ];
      }, t('addSuccess'));
      adding.value = true;
      await action();
      metaData.value = {
        key: '',
        value: '',
      };
      showMetaDataPopup.value = false;
      adding.value = false;
    }

    async function handleDeleteMetaData(key: string | number): Promise<void> {
      if (deleting.value) return;
      const action = asyncAction(async () => {
        await store.dispatch(DELETE_META_DATA, {
          projectId: props.projectId,
          repoName: props.repoName,
          fullPath: encodeURIComponent(props.fullPath!),
          data: {
            keyList: [key],
          },
        });

        tableData.value = tableData.value?.filter((meta: MetaData) => meta.key !== key);
      }, t('deleteSuccess'));
      deleting.value = true;
      await action();
      deleting.value = false;
    }

    function toggleTokenDialog() {
      isCreateTokenDialogShow.value = !isCreateTokenDialogShow.value;
    }
    function goToken() {
      router.push({
        name: 'repoToken',
      });
    }

    onBeforeMount(async () => {
      try {
        loading.value = true;
        const res = await store.dispatch(GET_ARTIFACTORY_INFO, {
          projectId: props.projectId,
          repoName: props.repoName,
          fullPath: encodeURIComponent(props.fullPath!),
        });
        const creator = store.state.userMap[res.createdBy as string];
        const lastModifier = store.state.userMap[res.lastModifiedBy as string];
        info.value = {
          ...res,
          size: formatSize(res.size),
          createdBy: creator ?? res.createdBy,
          createdDate: formatDate(res.createdDate),
          lastModifiedBy: lastModifier ?? res.lastModifiedBy,
          lastModifiedDate: formatDate(res.lastModifiedDate),
          path: res.fullPath,
        };
        tableData.value = Object.keys(res.metadata).map(key => ({
          key,
          value: res.metadata[key],
        }));
      } catch (error) {
        console.trace(error);
      } finally {
        loading.value = false;
      }
    });

    return () => (
      <>
        <Tab
          class="meta-data-tab"
          type='unborder-card'
          addable={false}
          v-model={[activeTab.value, 'active']}
        >
            {
              loading.value
                ? <Loading class="metadata-loading" loading />
                : panels.value.map(item => (
                  <TabPanel
                    key={item.name}
                    name={item.name}
                    label={t(item.label)}
                  >
                    {item.content()}
                  </TabPanel>
                ))
            }
        </Tab>
        <TokenDialog
          projectId={props.projectId}
          isShow={isCreateTokenDialogShow.value}
          onClose={toggleTokenDialog}
        />
        {
          showMetaDataPopup.value && (
            <div class="add-meta-data-popup">
              <Form class="meta-data-form" labelWidth={80}>
                {
                  Object.keys(metaData.value).map((key: string) => (
                    <FormItem label={t(key)} required>
                      <Input v-model={metaData.value[key as 'key' | 'value']}  />
                    </FormItem>
                  ))
                }
              </Form>
              <div class="meta-data-form-footer">
                <Button onClick={toggleMetaDataPopup}>{t('cancel')}</Button>
                <Button
                  theme='primary'
                  loadingMode='spin'
                  loading={adding.value}
                  disabled={adding.value}
                  onClick={handleAddMetaData}
                >
                  {t('confirm')}
                </Button>
              </div>
            </div>
          )

        }
      </>
    );
  },
});
