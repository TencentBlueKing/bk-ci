import { defineComponent, ref, onMounted, h } from 'vue';
import { useI18n } from 'vue-i18n';
import { useRouter } from 'vue-router';
import { Tag, InfoBox } from 'bkui-vue';
import { Plus } from 'bkui-vue/lib/icon';
import PlatformHeader from '@/components/platform-header';
import GitIcon from '@/css/image/git.png';
import http from '@/http/api'

export default defineComponent({
  setup() {
    const { t } = useI18n();
    const router = useRouter();
    const borderConfig = ['outer', 'row']
    const tableData = ref([
      {
        ip: '123',
        name: '工蜂',
        webhook: true,
        pac: false,
        state: 1
      }, {
        ip: '123',
        name: '工蜂',
        webhook: false,
        pac: true,
        state: 2
      }
    ]);
    const columns = ref([
      {
        "label": t('代码源名称'),
        "field": "ip",
        render({ cell, row }) {
          return h('p', {
            class: 'flex items-center'
          }, [
            h('img', {
              src: GitIcon,
              class: 'w-[17px] mr-[2px]'
            }),
            h('span', {}, row.name)
          ])
        }
      },
      {
        "label": t('代码源标识'),
        "field": "ip",
      },
      {
        "label": t('代码源域名'),
        "field": "ip",
      },
      {
        "label": "Webhook",
        "field": "webhook",
        render({ cell, row }) {
          return h(Tag, {
            theme: row.webhook ? 'success' : '',
          }, {
            default: () => row.webhook ? t('启用') : t('未启用')
          })
        }
      },
      {
        "label": "PAC",
        "field": "pac",
        render({ cell, row }) {
          return h(Tag, {
            theme: row.pac ? 'success' : ''
          }, {
            default: () => row.pac ? t('启用') : t('未启用')
          })
        }
      },
      {
        "label": t('创建人'),
        "field": "ip",
      },
      {
        "label": t('创建时间'),
        "field": "ip",
      },
      {
        "label": t('最近修改人'),
        "field": "ip",
      },
      {
        "label": t('最近修改时间'),
        "field": "ip",
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
              class: 'text-[#3A84FF] text-[12px] mr-[5px] cursor-pointer',
              onClick() {
                getInfoBox(row.state, row.name)
              }
            }, row.state === 1 ? t('启用') : t('停用')),
            h('span', {
              class: `text-[#3A84FF] text-[12px] mr-[5px] cursor-pointer ${row.state === 2 ? 'text-[#C4C6CC]' : ''}`,
              onClick() {
                getInfoBox(row.state, row.name)
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
        const res = await http.fetchRepoConfigList()
        console.log(res, 123)
      } catch (e) {
        console.error(e)
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

    const getInfoBox = (state, name: string) => {
      const tip = state === 1 ? t('停用') : t('删除');
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
