import { useI18n } from 'vue-i18n';
import { defineComponent, onMounted, ref, h } from 'vue';
import { Plus } from 'bkui-vue/lib/icon';
import PaltformHeader from '@/components/paltform-header';
// import './index.less'; 
// import useUserStore from '@/store/user';

export default defineComponent({
  setup() {
    const { t } = useI18n();
    // const userStore = useUserStore();
    // onMounted(() => {
    //   userStore.fetchUserInfo();
    // });
    const currentTap = ref('manage');
    const search = ref('');
    const tableData = ref([{ ip: '123' }, { ip: '123' }]);
    const columns = ref([
      {
        "label": "代码源名称",
        "field": "ip",
        render({ cell, row }) {
          // return h()
        }
      },
      {
        "label": "代码源标识",
        "field": "ip",
      },
      {
        "label": "代码源域名",
        "field": "ip",
      },
      {
        "label": "Webhook",
        "field": "ip",
      },
      {
        "label": "PAC",
        "field": "ip",
      },
      {
        "label": "创建人",
        "field": "ip",
      },
      {
        "label": "创建时间",
        "field": "ip",
      },
      {
        "label": "最近修改人",
        "field": "ip",
      },
      {
        "label": "最近修改时间",
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
            h('span', '续期'),
            h('span', '删除')
          ])
        }
      }
    ]);
    const pagination = ref({ count: 0, current: 1, limit: 20 });
    const typeMap = ref([
      {
        label: t('管理员'),
        value: 'manage',
        num: 10
      },
      {
        label: t('观察者'),
        value: 'observer',
        num: 4
      }
    ])

    onMounted(() => {
      fetchData();
    });

    function fetchData() {
      pagination.value.count = 10;
    };

    function changeTap(tapValue: string) {
      currentTap.value = tapValue;
    };

    function handlePageLimitChange(limit: number) {
      pagination.value.limit = limit;
    };

    function handlePageValueChange(value: number) {
      pagination.value.current = value;
    };

    function handleSearch(value: string) {
      console.log(value, '搜索参数');
    };

    function handleClear() {
      search.value = ''
    };

    return () => (
      <>
        <PaltformHeader title={t('系统管理员')} />
        <div class="p-[24px] h-mainHeight">
          <div class="flex">
            <div class="flex-1">
              <bk-button theme="primary" class="mb-[16px] mr-[12px]">
                <Plus width={14} class="mr-[6px]" />
                {t('新增')}
              </bk-button>
              <div class="inline-flex">
                {
                  typeMap.value.map(item => (
                    <p
                      onClick={() => changeTap(item.value)}
                      class={`${item.value === currentTap.value ? 'border-[#3A84FF] bg-[#E1ECFF] text-[#3A84FF]' : 'border-[#C4C6CC] bg-white text-[#4D4F56]'} border text-[14px] h-[32px] leading-[32px] px-[16px] cursor-pointer`}
                    >
                      <span>{item.label}</span>
                      <span>（{item.num}）</span>
                    </p>
                  ))
                }
              </div>
            </div>
            <bk-input
              class="flex-1"
              v-model={search.value}
              type="search"
              placeholder={t('请输入用户名搜索')}
              onEnter={handleSearch}
              onChange={handleSearch}
              onClear={handleClear}
            />
          </div>

          <bk-table
            class="bg-white !h-tableHeight"
            border={['outer', 'row']}
            columns={columns.value}
            data={tableData.value}
            pagination={pagination.value}
            remote-pagination
            settings={true}
            onPageLimitChange={handlePageLimitChange}
            onPageValueChange={handlePageValueChange}
          />
        </div>
      </>
    );
  },
});
