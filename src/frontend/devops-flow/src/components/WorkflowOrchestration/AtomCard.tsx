import { defineComponent, computed, ref, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { Button, Message, Rate } from 'bkui-vue'
import { type AtomItem } from '@/api/atom'
import { installAtom } from '@/api/atom'
import { SvgIcon } from '@/components/SvgIcon'
import styles from './AtomCard.module.css'

export default defineComponent({
  name: 'AtomCard',
  props: {
    atom: {
      type: Object as PropType<AtomItem>,
      required: true,
    },
    activeAtomCode: {
      type: String,
      default: '',
    },
    currentAtomCode: {
      type: String,
      default: '',
    },
    projectCode: {
      type: String,
      required: true,
    },
  },
  emits: ['select', 'install-success', 'click'],
  setup(props, { emit }) {
    // ========== Hooks ==========
    const { t } = useI18n()

    // ========== Refs ==========
    const isInstalling = ref(false)

    // ========== Computed ==========
    const isInstalled = computed(() => props.atom.installed || props.atom.defaultFlag)

    const isSelected = computed(() => props.atom.atomCode === props.currentAtomCode)

    const isActive = computed(() => props.atom.atomCode === props.activeAtomCode)

    const isDisabled = computed(() => props.atom.disabled)

    // ========== Lifecycle Hooks ==========
    // (暂无 lifecycle hooks)

    // ========== Functions ==========
    function getShowNum(num?: number) {
      if (!num) return '0'
      if (num > 10000) {
        return Math.floor(num / 10000) + 'W+'
      }
      return num.toString()
    }

    function getIconName(atomCode: string) {
      // 可以在这里添加图标映射逻辑
      return 'placeholder'
    }

    function handleSelectAtom() {
      if (isDisabled.value || isSelected.value) return
      emit('select', props.atom.atomCode)
    }

    async function handleInstallAtom() {
      if (isInstalling.value || !props.atom.installFlag) return

      isInstalling.value = true
      try {
        await installAtom({
          atomCode: props.atom.atomCode,
          projectCode: [props.projectCode],
        })
        Message({
          theme: 'success',
          message: t('flow.orchestration.installSuccess'),
        })
        // 更新插件状态
        props.atom.installed = true
        emit('install-success', props.atom)
      } catch (error: any) {
        Message({
          theme: 'error',
          message: error.message || t('flow.orchestration.installFailed'),
        })
      } finally {
        isInstalling.value = false
      }
    }

    function handleClick() {
      emit('click', props.atom.atomCode)
    }

    function getOsTooltip() {
      const { atom } = props
      const os = atom.os || []
      if (os.length && !os.includes('NONE')) {
        const osListStr = os.join('、')
        return t('flow.orchestration.envUseTips', [osListStr])
      }
      return t('flow.orchestration.noEnvUseTips')
    }

    function handleSelectAtomClick(e: MouseEvent) {
      e.stopPropagation()
      handleSelectAtom()
    }

    function handleInstallAtomClick(e: MouseEvent) {
      e.stopPropagation()
      handleInstallAtom()
    }

    function handleDocLinkClick(e: MouseEvent) {
      e.stopPropagation()
    }

    return () => (
      <div
        class={[
          styles.atomCard,
          styles.atomItemMain,
          isActive.value && styles.active,
          isDisabled.value && styles.disabled,
        ]}
        onClick={handleClick}
        title={isDisabled.value ? getOsTooltip() : ''}
      >
        {/* 插件图标 */}
        <div class={styles.atomLogo}>
          {props.atom.logoUrl ? (
            <img src={props.atom.logoUrl} alt={props.atom.name} />
          ) : (
            <SvgIcon name={getIconName(props.atom.atomCode)} size={48} />
          )}
        </div>

        {/* 插件信息 */}
        <div class={styles.atomInfoContent}>
          <p class={styles.atomName}>
            <span
              class={[
                styles.atomNameText,
                props.atom.recommendFlag === false && styles.notRecommend,
              ]}
              title={props.atom.name}
            >
              {props.atom.name}
            </span>
            {/* TODO: 添加荣誉标签和索引信息 */}
          </p>
          <p class={styles.desc}>{props.atom.summary || t('flow.orchestration.noDesc')}</p>
          <section class={styles.atomRate}>
            <div class={styles.scoreGroup}>
              {/* 评分显示 */}
              <div class={styles.rateStars}>
                <Rate modelValue={props.atom.score} editable={false} />
              </div>
            </div>
            {/* 热度图标 */}
            <span class={styles.hotIconContainer}>
              <SvgIcon class={styles.hotIcon} name="heat-fill" />
              {getShowNum(props.atom.recentExecuteNum)}
            </span>
            <p class={styles.atomFrom}>
              {props.atom.publisher || '--'} {t('flow.orchestration.provided')}
            </p>
          </section>
        </div>

        {/* 操作区域 */}
        <div class={styles.atomOperate}>
          {isInstalled.value ? (
            <Button
              class={styles.selectAtomBtn}
              disabled={isDisabled.value || isSelected.value}
              onClick={handleSelectAtomClick}
            >
              {isSelected.value ? t('flow.orchestration.selected') : t('flow.orchestration.select')}
            </Button>
          ) : (
            <Button
              class={styles.selectAtomBtn}
              size="small"
              disabled={!props.atom.installFlag}
              loading={isInstalling.value}
              onClick={handleInstallAtomClick}
              title={props.atom.installFlag ? '' : t('flow.orchestration.noPermToInstall')}
            >
              {t('flow.orchestration.install')}
            </Button>
          )}
          {/* 文档链接 */}
          {props.atom.docsLink && (
            <a
              class={styles.atomLink}
              href={props.atom.docsLink}
              target="_blank"
              rel="noopener noreferrer"
              onClick={handleDocLinkClick}
            >
              {t('flow.orchestration.knowMore')}
            </a>
          )}
        </div>
      </div>
    )
  },
})
