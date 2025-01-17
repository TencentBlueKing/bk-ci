
import { useI18n } from 'vue-i18n';
import { defineComponent, ref, onMounted, h } from 'vue';
import { useRouter } from 'vue-router';
import Plus from '@/css/svg/plus.svg';
import PaltformHeader from '@/components/paltform-header';

export default defineComponent({
  setup() {
    const { t } = useI18n();
    const router = useRouter();

    const tableData = ref([{ ip: '123' }, { ip: '123' }]);
    const columns = ref([
      {
        "label": t('代码源名称'),
        "field": "ip",
        render({ cell, row }) {
          // return h()
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
        "field": "ip",
      },
      {
        "label": "PAC",
        "field": "ip",
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
            h('span', '启用'),
            h('span', '删除')
          ])
        }
      }
    ]);
    const pagination = ref({ count: 0, current: 1, limit: 20 });

    onMounted(() => {
      fetchData();
    });

    function fetchData() {
      pagination.value.count = 10;
    };

    function handlePageLimitChange(limit: number) {
      pagination.value.limit = limit;
    };

    function handlePageValueChange(value: number) {
      pagination.value.current = value;
    };

    function goCreateCodeSource() {
      router.push({
        name: 'CreateCodeSource'
      })
    };

    return () => (
      <>
        <PaltformHeader title={t('代码源管理')} />
        <div class="p-[24px] h-mainHeight">
          <bk-button theme="primary" class="mb-[16px]" onClick={goCreateCodeSource}>
            <img src={Plus} alt="" class="w-[12px] mr-[6px] align-middle" />
            {t('新增代码源')}
          </bk-button>

          <bk-table
            class="bg-white !h-tableHeight"
            border={['outer', 'row']}
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
