import { computed, defineComponent, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { Table, Link } from 'bkui-vue'
import { SvgIcon } from '../SvgIcon'
import styles from './ThirdPartyReport.module.css'

interface Report {
  name: string
  indexFileUrl: string
  [key: string]: any
}

export default defineComponent({
  name: 'ThirdPartyReport',
  props: {
    reportList: {
      type: Array as PropType<Report[]>,
      default: () => [],
    },
  },
  setup(props) {
    const { t } = useI18n()

    const columns = computed(() => [
      {
        label: t('flow.dialog.copyCreation.name'),
        field: 'name',
        render: ({ row }: { row: Report }) => (
          <Link class={styles.reportTdName} target="_blank" theme="primary" href={row.indexFileUrl}>
            {row.name}
            <SvgIcon name="tiaozhuan" size={16} class={styles.jumpIcon} />
          </Link>
        ),
      },
    ])

    return () => (
      <Table data={props.reportList} border="outer" columns={columns.value as any}>
        {{
          empty: () => <div class={styles.emptyState}>{t('flow.common.noData')}</div>,
        }}
      </Table>
    )
  },
})
