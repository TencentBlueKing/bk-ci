import http from '@/http/api'
import { defineComponent, ref, onMounted, h, resolveDirective, withDirectives } from 'vue';
import { timeFormatter } from '@/common/util';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { Tag, InfoBox, Message } from 'bkui-vue';
import { Plus } from 'bkui-vue/lib/icon';
import PlatformHeader from '@/components/platform-header';
import { storeToRefs } from 'pinia';
import useRepoConfigTable from "./useRepoConfigTable";

export default defineComponent({
  setup() {
    const { t } = useI18n();
    const router = useRouter();
    const repoConfigStore = useRepoConfigTable();
    const {
      pagination,
      isLoading,
      repoConfigList,
      borderConfig,
    } = storeToRefs(repoConfigStore)
    const {
      setCurConfig,
      getRepoConfigList,
      handlePageLimitChange,
      handlePageValueChange
    } = useRepoConfigTable();
    const enabledStatus = ['OK', 'SUCCESS', 'DEPLOYING'];
    const bkTooltips = resolveDirective('bk-tooltips');

    const columns = ref([
      {
        'label': t('代码源名称'),
        'field': 'name',
        disabled: true,
        render({ cell, row }) {
          return h('p', {
            class: 'flex items-center'
          }, [
            h('img', {
              src: row.logoUrl,
              class: 'w-[17px] mr-[2px]'
            }),
            h('span', {
              class: `text-[#3A84FF] text-[12px] cursor-pointer`,
              onClick() {
                handleToConfigDetail(row)
              }
            }, row.name)
          ])
        }
      },
      {
        'label': t('代码源标识'),
        'field': 'scmCode',
        disabled: true,
      },
      {
        'label': t('代码源域名'),
        'field': 'hosts',
      },
      {
        'label': 'Webhook',
        'field': 'webhookEnabled',
        width: 120,
        render({ cell, row }) {
          return h(Tag, {
            theme: row.webhookEnabled ? 'success' : '',
          }, {
            default: () => row.webhookEnabled ? t('启用') : t('未启用')
          })
        }
      },
      {
        'label': 'PAC',
        'field': 'pacEnabled',
        width: 120,
        render({ cell, row }) {
          return h(Tag, {
            theme: row.pacEnabled ? 'success' : ''
          }, {
            default: () => row.pacEnabled ? t('启用') : t('未启用')
          })
        }
      },
      {
        'label': t('创建人'),
        'field': 'creator',
      },
      {
        'label': t('创建时间'),
        'field': 'createTime',
        render({ cell, row }) {
          return h('span', [timeFormatter(cell)])
        }
      },
      {
        'label': t('最近修改人'),
        'field': 'updater',
      },
      {
        'label': t('最近修改时间'),
        'field': 'updateTime',
        render({ cell, row }) {
          return h('span', [timeFormatter(cell)])
        }
      },
      {
        label: t('操作'),
        field: 'operation',
        disabled: true,
        showOverflowTooltip: true,
        render({ cell, row }) {
          return h('p', {
            style: {
              display: 'flex'
            }
          }, [
            h('span', {
              class: 'text-[#3A84FF] text-[12px] mr-[8px] cursor-pointer',
              onClick() {
                handleToConfigDetail(row)
              }
            }, t('编辑')),
            h('span', {
              class: 'text-[#3A84FF] text-[12px] mr-[8px] cursor-pointer',
              onClick() {
                handleToggleConfigStatus(row)
              }
            }, enabledStatus.includes(row.status) ? t('停用') : t('启用')),
            ...(
              [withDirectives(h('p', {
                class: `text-[#3A84FF] text-[12px] ${!row.canDelete ? 'text-[#C4C6CC] cursor-not-allowed' : 'cursor-pointer'}`,
                style: { 'margin-left': '5px' },
                onClick() {
                  if (!row.canDelete) return
                  handleDeleteConfig(row)
                }
              }, t('删除')), !row.canDelete ? [[bkTooltips, t('已关联代码库，不能删除')]] : [])] 
            ),
          ])
        }
      }
    ]);

    const goCreateCodeSource = () => {
      router.push({
        name: 'ConfigForm',
        query: {
          action: 'create'
        }
      })
    };

    const handleToggleConfigStatus = (row: any) => {
      const { status, name, scmCode } = row;
      const tip = enabledStatus.includes(status) ? t('停用') : t('启用');
      InfoBox({
        confirmText: tip,
        cancelText: t('取消'),
        confirmButtonTheme: 'danger',
        title: t('是否X该代码源？', [t(tip)]),
        content: h('div', {
          class: 'text-[14px] text-[#4D4F56]'
        }, [
          h('span', `${t('代码源名称')}：`),
          h('span', { class: 'text-[#313238]' }, name)
        ]),
        onConfirm: async () => {
          try {
            const type = enabledStatus.includes(status) ? 'disable' : 'enable'
            const res = await http.toggleEnableRepoConfig(scmCode, type)
            if (res) {
              Message({
                theme: 'success',
                message: t(`${tip}成功`)
              })
              getRepoConfigList()
            }
          } catch (e) {
            console.error(e)
          }
        },
      });
    } 

    const handleDeleteConfig = (row: any) => {
      const { name, scmCode } = row
      InfoBox({
        confirmText: t('删除'),
        cancelText: t('取消'),
        confirmButtonTheme: 'danger',
        title: t('是否X该代码源？', [t('删除')]),
        content: h('div', {
          class: 'text-[14px] text-[#4D4F56]'
        }, [
          h('span', `${t('代码源名称')}：`),
          h('span', { class: 'text-[#313238]' }, name)
        ]),
        onConfirm: async () => {
          try {
            const res = await http.deleteRepoConfig(scmCode)
            if (res) {
              Message({
                theme: 'success',
                message: t('删除成功')
              })
              getRepoConfigList()
            }
          } catch (e) {
            console.error(e)
          }
        },
      });
    };

    const handleToConfigDetail = (row) => {
      setCurConfig(row)
      router.push({
        name: 'ConfigForm',
        query: {
          action: 'detail'
        }
      })
    }
    const settings = {
      checked: columns.value.map(i => i.field)
    }
    onMounted(() => {
      getRepoConfigList();
    });

    return () => (
      <>
        <PlatformHeader title={t('代码源管理')} />
        <div class="p-[24px] h-mainHeight">
          <bk-button theme="primary" class="mb-[16px]" onClick={goCreateCodeSource}>
            <Plus class="text-[22px]" />
            {t('新增代码源')}
          </bk-button>
          <bk-loading loading={isLoading.value}>
            <bk-table
              class="bg-white !h-tableHeight"
              border={borderConfig.value}
              settings={settings}
              show-overflow-tooltip
              columns={columns.value}
              data={repoConfigList.value}
              pagination={pagination.value}
              remote-pagination
              onPageLimitChange={handlePageLimitChange}
              onPageValueChange={handlePageValueChange}
            />
          </bk-loading>
        </div>
      </>
    );
  },
});
