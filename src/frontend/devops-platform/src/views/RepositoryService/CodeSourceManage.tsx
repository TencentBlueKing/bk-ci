import http from '@/http/api'
import { defineComponent, ref, onMounted, h } from 'vue';
import { timeFormatter } from '@/common/util';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { Tag, InfoBox } from 'bkui-vue';
import { Plus } from 'bkui-vue/lib/icon';
import PlatformHeader from '@/components/platform-header';

export default defineComponent({
  setup() {
    const { t } = useI18n();
    const router = useRouter();
    const borderConfig = ['outer', 'row'];
    const isLoading = ref(false);
    const tableData = ref([]);
    const columns = ref([
      {
        'label': t('代码源名称'),
        'field': 'name',
        render({ cell, row }) {
          return h('p', {
            class: 'flex items-center'
          }, [
            h('img', {
              src: row.logoUrl,
              class: 'w-[17px] mr-[2px]'
            }),
            h('span', {}, row.name)
          ])
        }
      },
      {
        'label': t('代码源标识'),
        'field': 'scmCode',
      },
      {
        'label': t('代码源域名'),
        'field': 'hosts',
      },
      {
        'label': 'Webhook',
        'field': 'webhookEnabled',
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
                handleToggleConfigStatus(row.state, row.name)
              }
            }, row.state === 1 ? t('启用') : t('停用')),
            h('span', {
              class: `text-[#3A84FF] text-[12px] cursor-pointer ${row.state === 2 ? 'text-[#C4C6CC]' : ''}`,
              onClick() {
                handleDeleteConfig(row.state, row.name)
              }
            }, t('删除'))
          ])
        }
      }
    ]);
    const pagination = ref({ count: 0, current: 1, limit: 20 });

    onMounted(() => {
      // fetchData();
      getRepoConfigList();
    });

    const fetchData = () => {
      pagination.value.count = 10;
    };

    const getRepoConfigList = async () => {
      try {
        isLoading.value = true;
        const res = await http.fetchRepoConfigList();
        tableData.value = res.records;
        pagination.value.count = res.count;
      } catch (e) {
        console.error(e)
      } finally {
        isLoading.value = false;
      }
    }

    const handlePageLimitChange = (limit: number) => {
      pagination.value.limit = limit;
    };

    const handlePageValueChange = (value: number) => {
      pagination.value.current = value;
    };

    const goCreateCodeSource = () => {
      router.push({
        name: 'CreateCodeSource'
      })
    };

    const handleToggleConfigStatus = (state, name: string) => {
      const tip = state === 10 ? t('停用') : t('启用');
      InfoBox({
        confirmText: tip,
        cancelText: t('取消'),
        confirmButtonTheme: 'danger',
        title: t('是否X该代码源？', [tip]),
        content: h('div', {
          class: 'text-[14px] text-[#4D4F56]'
        }, [
          h('span', `${t('代码源名称')}：`),
          h('span', { class: 'text-[#313238]' }, name)
        ]),
        onConfirm() {
          console.log('---');
        },
      });
    } 

    const handleDeleteConfig = (state, name: string) => {
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
        onConfirm() {
          console.log('---');
        },
      });
    };

    return () => (
      <>
        <PlatformHeader title={t('代码源管理')} />
        <div class="p-[24px] h-mainHeight">
          <bk-button theme="primary" class="mb-[16px]" onClick={goCreateCodeSource}>
            <Plus class="text-[22px]" />
            {t('新增代码源')}
          </bk-button>

          <bk-table
            class="bg-white !h-tableHeight"
            border={borderConfig}
            settings={true}
            columns={columns.value}
            data={tableData.value}
            pagination={pagination.value}
            remote-pagination
            onPageLimitChange={handlePageLimitChange}
            onPageValueChange={handlePageValueChange}
          />
        </div>
      </>
    );
  },
});
