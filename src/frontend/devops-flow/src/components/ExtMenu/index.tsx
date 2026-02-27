import { defineComponent, ref, type PropType, computed } from 'vue'
import { Popover } from 'bkui-vue'
import { SvgIcon } from '@/components/SvgIcon'
import { handleFlowNoPermission } from '@/utils/permission'
import styles from './ExtMenu.module.css'
import { type MenuItem } from '@/api/flowContentList'

export default defineComponent({
  name: 'ExtMenu',
  components: {
    SvgIcon,
  },
  props: {
    data: {
      type: Object,
      default: () => ({}),
    },
    config: {
      type: Array as PropType<MenuItem[]>,
      default: () => [],
    },
  },

  setup(props) {
    const dotMenuRef = ref()
    const hasShow = ref(false)
    const bodyEle = computed(() => document.getElementsByTagName('body')[0])

    function getTooltips(item: MenuItem) {
      if (item.hasPermission === false) {
        return {
          content: '没有操作权限',
          disabled: false,
          allowHTML: false,
        }
      }
      return {
        content: item?.tooltips,
        disabled: !item?.tooltips,
        allowHTML: false,
      }
    }

    function clickMenuItem(item: MenuItem) {
      if (item.disable) return

      if (item.hasPermission === false) {
        hasShow.value = false
        if (item.permissionData) {
          handleFlowNoPermission(item.permissionData)
        }
        return
      }

      hasShow.value = false
      item.handler(props.data, item)
    }

    function handleShowMenu() {
      hasShow.value = true
    }

    function handleHideMenu() {
      hasShow.value = false
    }

    return () => (
      <Popover
        disabled={props.config.length === 0}
        placement="bottom"
        isShow={hasShow.value}
        ref={dotMenuRef.value}
        theme="light"
        trigger="click"
        arrow={false}
        clickContentAutoHide
        boundary={bodyEle.value}
        extCls="ext-menu-popover"
        onAfterShow={handleShowMenu}
        onAfterHidden={handleHideMenu}
      >
        {{
          default: () => (
            <span class={styles.moreTrigger}>
              <SvgIcon name="more-fill" class={styles.moreFill} />
            </span>
          ),
          content: () =>
            props.config.length > 0 && (
              <ul class={styles.dotMenuList}>
                {props.config.map((item, index) => (
                  <li
                    class={`${styles.dotMenuItem} ${item.disable ? styles.isDisable : ''} ${item.hasPermission === false ? styles.isDisable : ''}`}
                    v-bk-tooltips={getTooltips(item)}
                    key={index}
                    onClick={(e: Event) => {
                      e.stopPropagation()
                      clickMenuItem(item)
                    }}
                  >
                    {item.text}
                  </li>
                ))}
              </ul>
            ),
        }}
      </Popover>
    )
  },
})
