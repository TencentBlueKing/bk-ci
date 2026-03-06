import { computed, defineComponent, h } from 'vue'
import { FlowGroupAside } from '@/components/FlowGroupAside'
import { Tab } from 'bkui-vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { FlowTable } from '@/components/FlowTable'
import { ROUTE_NAMES } from '@/constants/routes'
import styles from './index.module.css'
import layoutStyles from '@/styles/layout.module.css'

export default defineComponent({
  name: 'FlowList',
  props: {
    groupId: {
      type: String,
      required: true,
    },
  },
  setup(props) {
    const router = useRouter()
    const route = useRoute()
    const { t } = useI18n()

    const activeTab = computed(() => {
      return (route.name ?? ROUTE_NAMES.FLOW_LIST) as string
    })

    const handleTabChange = (name: string) => {
      if (name === activeTab.value) return
      router.push({ name })
    }

    return () => (
      <div class={layoutStyles.page}>
        <div class={styles.header}>
          <div class={styles.headerLeft}>
            <img
              src={`${import.meta.env.BASE_URL}devops-flow-logo.svg`}
              alt="flow"
              class={styles.logo}
            />
            <span class={styles.title}>{t('flow.title')}</span>
          </div>
          <Tab
            active={activeTab.value}
            type="unborder-card"
            class={styles.navTabs}
            onChange={handleTabChange}
          >
            <Tab.TabPanel name={ROUTE_NAMES.FLOW_LIST} label={t('flow.title')}></Tab.TabPanel>
            {/* <Tab.TabPanel
              name={ROUTE_NAMES.TEMPLATE}
              label={t('flow.tabs.template')}
            ></Tab.TabPanel> */}
          </Tab>
        </div>
        <div class={layoutStyles.content}>
          <div class={styles.sidebar}>
            <FlowGroupAside />
          </div>
          <div class={styles.content}>
            <FlowTable groupId={props.groupId} />
          </div>
        </div>
      </div>
    )
  },
})
