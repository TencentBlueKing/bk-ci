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
import { defineComponent, ref, computed, watch, PropType } from 'vue';
import { Loading, Tab, Table, Tag, Button } from 'bkui-vue';
import { useI18n } from 'vue-i18n';

import { useStore } from '@/store';
import { FETCH_PACKAGE_DETAIL } from '@/store/constants';
import { useInstallGuide, useRouteParams, useDownloadPackage, useDomain } from '@/hooks';
import SectionBox from './SectionBox';
import CodeBox from './CodeBox';
import OperationMenu from './OperationMenu';
import { Operation } from '@/utils/vue-ts';


const { TabPanel } = Tab;

export default defineComponent({
  props: {
    operations: {
      type: Array as PropType<Operation[]>,
      required: true,
    },
    handleOperation: {
      type: Function,
      required: true,
    },
  },
  setup(props) {
    const store = useStore();
    const { t } = useI18n();
    const activeTab = ref('baseInfo');
    const routeParams = useRouteParams();
    const loading = ref(true);
    const packageDetail = ref();
    const domain = useDomain();
    console.log(domain);

    const basicGridInfo = computed(() => [
      { name: 'version', label: t('version') },
      { name: 'os', label: 'OS/ARCH' },
      { name: 'fullPath', label: t('path') },
      { name: 'size', label: t('size') },
      { name: 'downloadCount', label: t('downloads') },
      { name: 'downloads', label: t('downloads') },
      { name: 'lastModifiedBy', label: t('lastModifiedBy') },
      { name: 'lastModifiedDate', label: t('lastModifiedDate') },
    ].filter(({ name }) => packageDetail.value?.basic
        && Object.prototype.hasOwnProperty.call(packageDetail.value?.basic, name))
      .map(item => ({ ...item, value: packageDetail.value?.basic?.[item.name] })));

    const metadata = computed(() => Object.entries(packageDetail.value?.metadata ?? {}).map(item => ({
      key: item[0],
      value: item[1],
    })));
    const currentVersion = computed(() => ({
      name: routeParams.value.version,
      stageTag: packageDetail.value?.basic?.stageTag,
    }));

    const downloadPackage = useDownloadPackage();

    const columns = [
      {
        label: t('key'),
        field: 'key',
      },
      {
        label: t('value'),
        field: 'value',
      },
    ];

    watch(() => routeParams.value.version, () => {
      fetchPackageDetail();
    }, { immediate: true });

    async function fetchPackageDetail() {
      try {
        loading.value = true;
        const detail = await store.dispatch(FETCH_PACKAGE_DETAIL, {
          ...routeParams.value,
          packageKey: routeParams.value.package,
        });
        packageDetail.value = detail;
      } catch (error) {
        console.trace(error);
      } finally {
        loading.value = false;
      }
    }
    const blockList = computed(() => {
      const guideSteps = useInstallGuide(packageDetail);
      return [{
        name: t('baseInfo'),
        content: () => (
          <div class="version-basic-info">
            <div class="package-name grid-item">
                <label>制品名称</label>
                <span>
                    { routeParams.value.package }
                    {packageDetail.value?.basic?.groupId && <span class="ml5 repo-tag"> { packageDetail.value?.basic?.groupId } </span>}
                </span>
            </div>
            {
              basicGridInfo.value.map((item: any) => (
                <div class="grid-item" key={item.name}>
                  <label>{ item.label }</label>
                  <span class="text-overflow" title={item.value}>
                    <span>{ item.value }</span>
                      {
                        item.name === 'version' && packageDetail.value?.basic?.stageTag?.map((tag: any) => (
                            <Tag key={tag}>{ tag }</Tag>
                        ))
                      }
                  </span>
                </div>
              ))
            }
            <div class="package-description grid-item">
              <label>描述</label>
              <span
                class="text-overflow"
                title={packageDetail.value?.basic?.description}
              >
                { packageDetail.value?.basic?.description ?? '--' }
              </span>
            </div>
          </div>
        ),
      }, {
        name: t('useTips'),
        content: () => (
          <div class="package-use-tip-container">
            {
              guideSteps.map(step => (
              <>
                <h5>{step.subTitle}</h5>
                <CodeBox class="use-tip-code-area" codeList={step.codeList} />
              </>
              ))
            }
          </div>
        ),
      }, {
        name: 'CheckSums',
        content: () => (
          <div class="bk-repo-block">
            {
              ['sha256', 'md5'].filter(item => packageDetail.value?.basic?.[item]).map(item => (
                <p class="bk-repo-block-info-row">
                  <label>{ item.toUpperCase() } </label>
                  <span>{packageDetail.value?.basic?.[item]}</span>
                </p>
              ))
            }
          </div>
        ),
      }];
    });

    const panels = [
      {
        label: 'baseInfo',
        name: 'baseInfo',
        content: () => !loading.value && (
          <SectionBox
            blockList={blockList.value}
            activeIndex={[0, 1, 2]}
          >
            {{
              default: (item: any) => item.content(),
            }}
          </SectionBox>
        ),
      },
      {
        label: 'metaData',
        name: 'metaData',
        content: () => !loading.value && (
          <Table
            height="100%"
            data={metadata.value}
            columns={columns}
          >
          </Table>
        ),
      },
    ];

    function switchTab(tabName: string) {
      activeTab.value = tabName;
    }

    function download() {
      downloadPackage({
        name: packageDetail.value?.basic?.version,
      });
    }

    return () => (
      <Loading class="package-detail-box" loading={loading.value}>
        <Tab
          class="package-detail-box"
          type='unborder-card'
          addable={false}
          active={activeTab.value}
          onChange={switchTab}
        >
              {
               {
                 default: () => (
                   panels.map(item => (
                    <TabPanel
                      key={item.name}
                      name={item.name}
                      label={t(item.label)}
                    >
                        {item.content()}
                    </TabPanel>
                   ))
                 ),
                 setting: () => (
                   <div class="package-operation">
                     <Button onClick={download}>
                       {t('download')}
                     </Button>
                     <Button class="package-more-opration">
                      <OperationMenu
                          operationList={props.operations}
                          handleOperation={
                            (e: MouseEvent, operation: Operation) => props.handleOperation(
                              operation,
                              currentVersion.value,
                            )
                          }
                        >
                        </OperationMenu>
                     </Button>
                   </div>
                 ),
               }
              }
        </Tab>
      </Loading>
    );
  },
});
