import { defineComponent, ref, computed, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { Dialog, Input, Tree, Link, Message, Loading } from 'bkui-vue'
import { SvgIcon } from '../SvgIcon'
import { useOutputs } from '@/hooks/useOutputs'
import { type Output, type CustomDirTreeNode } from '@/api/outputs'
import styles from './CopyToCustomRepoDialog.module.css'

export default defineComponent({
  name: 'CopyToCustomRepoDialog',
  props: {
    artifact: {
      type: Object as PropType<Output>,
      required: true,
    },
  },
  emits: ['close'],
  setup(props, { emit, expose }) {
    const { t } = useI18n()
    const route = useRoute()
    const currentTab = computed(() => route.params.type as string)
    const { requestCustomFolder, requestCopyArtifactories } = useOutputs(
      currentTab,
    )

    const isCopyDialogShow = ref(false)
    const keyPath = ref('')
    const activeFolder = ref<string | null>(null)
    const customRepoTree = ref()

    const customRootFolder = ref<CustomDirTreeNode>({
      fullPath: '/',
      name: t('flow.execute.customRepo'),
      children: [],
      isOpen: true,
      loading: false,
      leaf: false,
    })

    const title = computed(() => t('flow.execute.copyArtifactTo', [props.artifact?.name ?? '--']))
    const customFolders = computed(() => [customRootFolder.value])

    // 初始化加载根目录的子节点
    async function initRootFolder() {
      try {
        customRootFolder.value.loading = true
        const res = await requestCustomFolder('/')
        if (res?.children) {
          customRootFolder.value.children = res.children.map((item: any) => ({
            ...item,
            isOpen: false,
            loading: false,
            leaf: false,
          }))
          customRootFolder.value.leaf = res.children.length === 0
        }
      } catch (error) {
        console.error('Failed to load root folder:', error)
      } finally {
        customRootFolder.value.loading = false
      }
    }

    function show() {
      isCopyDialogShow.value = true
      initRootFolder()
    }

    function hide() {
      isCopyDialogShow.value = false
      keyPath.value = ''
      activeFolder.value = null
      customRootFolder.value = {
        fullPath: '/',
        name: t('flow.execute.customRepo'),
        children: [],
        isOpen: true,
        loading: false,
        leaf: false,
      }
      emit('close')
    }

    async function handleNodeExpand(node: CustomDirTreeNode) {
      console.log('Node expand triggered:', node)

      // 如果已经加载过子节点，直接切换展开状态
      if (node.children && node.children.length > 0) {
        node.isOpen = !node.isOpen
        return
      }

      try {
        node.loading = true
        const res = await requestCustomFolder(node.fullPath)
        if (res?.children) {
          // 更新节点的 children
          node.children = res.children.map((item: any) => ({
            ...item,
            isOpen: false,
            loading: false,
            leaf: false,
          }))
          node.isOpen = true
          node.leaf = res.children.length === 0
        } else {
          node.children = []
          node.leaf = true
        }
      } catch (error) {
        console.error('Failed to load folder:', error)
      } finally {
        node.loading = false
      }
    }

    function search(keyword: string) {
      customRepoTree.value?.filter(keyword)
    }

    function handleNodeCollapse(node: CustomDirTreeNode) {
      console.log('Node collapsed:', node)
      node.isOpen = false
    }

    function handleNodeClick(node: CustomDirTreeNode) {
      console.log('Node clicked:', node)
      activeFolder.value = node.fullPath
    }

    async function copyToCustom() {
      // 校验是否选择了目标文件夹
      if (!activeFolder.value) {
        Message({
          message: t('flow.execute.pleaseSelectFolder'),
          theme: 'warning',
          delay: 3000,
        })
        return
      }
      let message: any
      let theme: 'success' | 'error' = 'success'

      try {
        const success = await requestCopyArtifactories({
          projectId: route.params.projectId as string,
          srcArtifactoryType: props.artifact.artifactoryType,
          srcFileFullPaths: [props.artifact.fullPath],
          dstArtifactoryType: 'CUSTOM_DIR',
          dstDirFullPath: activeFolder.value,
        })

        if (success) {
          const repoUrl = `${(window as any).WEB_URL_PREFIX}/repo/${route.params.projectId}/generic?repoName=custom&path=${encodeURIComponent(`${activeFolder.value}/default`)}`

          message = (
            <p>
              {t('flow.execute.copyToCustomSuc', [props.artifact.name, activeFolder.value])}
              <Link
                target="_blank"
                theme="primary"
                href={repoUrl}
                class={styles.copyToCustomSucMessage}
              >
                <span class={styles.goDistLink}>
                  <SvgIcon name="jump" size={12} class={styles.copyToCustomSucIcon} />
                  {t('flow.execute.goDistFolder')}
                </span>
              </Link>
            </p>
          )
          theme = 'success'
          hide() // 成功后关闭对话框
        } else {
          message = t('flow.execute.copyToCustomFail')
          theme = 'error'
        }
      } catch (err: any) {
        message = err.message || err || t('flow.execute.copyToCustomFail')
        theme = 'error'
      } finally {
        Message({
          message,
          theme,
          delay: 50000,
          limit: 1,
        })
      }
    }

    // 暴露方法给父组件
    expose({
      show,
      hide,
    })

    return () => (
      <Dialog
        is-show={isCopyDialogShow.value}
        class={styles.copyToCustomDialog}
        title={t('flow.execute.copyTo')}
        closeIcon={false}
        escClose={false}
        quickClose={false}
        width={600}
        onConfirm={copyToCustom}
        onClosed={hide}
      >
        <p class={styles.copyToCustomRepoDialogHeader} v-html={title.value}></p>
        <Input
          class={styles.copyToCustomRepoSearch}
          v-model={keyPath.value}
          placeholder={t('flow.execute.serachCustomRepo')}
          onEnter={search}
        >
          {{
            suffix: () => <SvgIcon name="search" size={16} onClick={() => search(keyPath.value)} style={{ cursor: 'pointer' }} />,
          }}
        </Input>
        <section class={styles.copyCustomRepoTree}>
          {isCopyDialogShow.value && (
            <Tree
              ref={customRepoTree}
              data={customFolders.value}
              node-key="fullPath"
              label="name"
              selectable
              height={400}
              node-content-action={['selected', 'click', 'expand', 'collapse']}
              expand-all
              show-node-type-icon={false}
              onNodeExpand={handleNodeExpand}
              onNodeCollapse={handleNodeCollapse}
              onNodeClick={handleNodeClick}
              search={keyPath.value}
            >
              {{
                nodeAction: ({ isOpen, loading, leaf }: any) => (
                  <span class="flex">
                    {!leaf &&
                      (loading ? (
                        <Loading mode="spin" size="mini" loading />
                      ) : (
                        <SvgIcon name="angle-down" class={isOpen ? '' : styles.rotate90} />
                      ))}
                  </span>
                ),
                node: (node: any) => {
                  const isActive = activeFolder.value === node.fullPath
                  return (
                    <div
                      class={[
                        styles.customRepoTreeNode,
                        isActive ? styles.customRepoTreeNodeActive : '',
                      ]}
                    >
                      <span class={styles.folderName}>{node.name}</span>
                    </div>
                  )
                },
              }}
            </Tree>
          )}
        </section>
      </Dialog>
    )
  },
})
