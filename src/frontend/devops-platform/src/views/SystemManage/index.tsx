import { useI18n } from 'vue-i18n';
import { defineComponent, onMounted, ref, h } from 'vue';
import { InfoBox } from 'bkui-vue';
import Plus from '@/css/svg/plus.svg';
// import AddFill from '@/css/svg/add-fill.svg';
// import CloseSamll from '@/css/svg/close-samll.svg';
// import EditLine from '@/css/svg/edit-line.svg';
import ArrowsRight from '@/css/svg/arrows-right.svg'
import PlatformHeader from '@/components/platform-header';
import TimeLimit from '@/components/time-limit'
import AddDialog from './components/AddDialog';
import RenewalDialog from './components/RenewalDialog';

export default defineComponent({
  setup() {
    const { t } = useI18n();
    const currentTap = ref('manage');
    const search = ref('');
    const isShowAddDialog = ref(false);
    const isShowRenewalDialog = ref(false);
    const formData = ref({
      name: '',
      role: '观察者',
      expiredAt: 30,
      scope: []
    });
    const renewal = ref();
    const tableData = ref([{ ip: '123' }, { ip: '123' }]);
    const columns = ref([
      {
        "label": t('用户'),
        "field": "ip",
      },
      {
        "label": t('有效期'),
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
            h('span', {
              class: 'text-[#3A84FF] text-[12px] mr-[5px] cursor-pointer',
              onClick() {
                isShowRenewalDialog.value = true;
              }
            }, t('续期')),
            h('span', {
              class: `text-[#3A84FF] text-[12px] mr-[5px] cursor-pointer ${row.state === 2 ? 'text-[#C4C6CC]' : ''}`,
              onClick() {
                InfoBox({
                  confirmText: t('删除'),
                  cancelText: t('取消'),
                  confirmButtonTheme: 'danger',
                  title: t('是否删除该系统管理员?'),
                  content: h('div', {
                    class: 'text-[14px] text-[#4D4F56]'
                  }, [
                    h('span', `${t('管理员名称')}：`),
                    h('span', { class: 'text-[#313238]' }, 'xxx')
                  ]),
                  onConfirm() {
                    console.log('---');
                  },
                });
              }
            }, t('删除'))
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

    function handleAdd() {
      isShowAddDialog.value = true
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
      search.value = '';
    };

    function handleSelected(flag:string) {
      formData.value.role = flag;
    };
    /**
     * 授权期限选择
     */
    function handleChangeTime(value, type) {
      if (type === 'add') {
        formData.value.expiredAt = Number(value);
      } else if (type === 'renewal') {
        renewal.value = Number(value)
      }
    };

    function handleAddConfirm() {
      console.log(formData.value);
      isShowAddDialog.value = false;
    };

    function handleAddClosed() {
      isShowAddDialog.value = false;
    };

    function handleRemoveItem(id) {};

    function handleRenewalConfirm() {
      console.log(renewal.value );
      isShowRenewalDialog.value = false;
    };

    function handleRenewalClosed() {
      isShowRenewalDialog.value = false;
    };


    return () => (
      <>
        <PlatformHeader title={t('系统管理员')} />
        <div class="p-[24px] h-mainHeight">
          <div class="flex">
            <div class="flex-1">
              <bk-button theme="primary" class="mb-[16px] mr-[12px]" onClick={handleAdd}>
                <img src={Plus} alt="" class="w-[12px] mr-[6px] align-middle" />
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

        <AddDialog
          v-model:isShow={isShowAddDialog.value}
          formData={formData.value}
          onConfirm={handleAddConfirm}
          onClosed={handleAddClosed}
          onSelected={handleSelected}
          onRemoveItem={handleRemoveItem}
          onChangeTime={handleChangeTime}
        />

        <RenewalDialog
          v-model:isShow={isShowRenewalDialog.value}
          onConfirm={handleRenewalConfirm}
          onRemoveItem={handleRenewalClosed}
          onChangeTime={handleChangeTime}
        />
      </>
    );
  },
});
