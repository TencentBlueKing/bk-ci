<template>
    <bk-dialog
        v-model="value"
        width="1080"
        ext-class="install-template-dialog"
        header-position="left"
        :title="$t('template.installOrImportTemplate')"
        :position="dialogPosition"
        :ok-text="confirmBtnText"
        :confirm-fn="handleConfirmInstall"
        :on-close="handleCancelInstall"
    >
        <bk-tab
            class="install-type-tab"
            :active.sync="activeTab"
            type="unborder-card"
        >
            <bk-tab-panel
                v-for="(panel, index) in tabTypeMap"
                v-bind="panel"
                :key="index"
            >
            </bk-tab-panel>
        </bk-tab>
        <component
            v-if="!!value"
            :is="componentName"
            @change="handleSubChange"
        />
    </bk-dialog>
</template>

<script>
    import { defineComponent, ref, toRefs, computed } from '@vue/composition-api'
    import UseInstance from '@/hook/useInstance'
    import StoreTemplateList from '@/components/template/install/StoreTemplateList'
    import Repository from '@/components/template/install/Repository'
    import LocalFile from '@/components/template/install/LocalFile'
    import {
        INSTALL_TYPE_STORE,
        INSTALL_TYPE_REPOSITORY,
        INSTALL_TYPE_LOCAL
    } from '@/store/modules/templates/constants'

    export default defineComponent({
        name: 'InstallTemplateDialog',
        components: {
            StoreTemplateList,
            Repository,
            LocalFile
        },
        props: {
            value: {
                type: Boolean,
                default: false
            }
        },
        setup (props, { root, emit }) {
            if (!root) return
            const { i18n, store, route } = UseInstance(root)
            const { value } = toRefs(props)
            const storeTemplateInfo = ref({})
            const activeTab = ref(INSTALL_TYPE_STORE)
            const dialogPosition = {
                top: '120'
            }
            const projectId = computed(() => route.params.projectId)
            const confirmBtnText = computed(() => activeTab.value === INSTALL_TYPE_STORE ? i18n.t('template.install') : i18n.t('template.import'))
            const tabTypeMap = computed(() => {
                return [
                    {
                        name: INSTALL_TYPE_STORE,
                        label: i18n.t('template.store')
                    },
                    {
                        name: INSTALL_TYPE_REPOSITORY,
                        label: i18n.t('template.repository')
                    },
                    {
                        name: INSTALL_TYPE_LOCAL,
                        label: i18n.t('template.localFile')
                    }
                ]
            })
            const componentName = computed(() => {
                const components = {
                    INSTALL_TYPE_STORE: StoreTemplateList,
                    INSTALL_TYPE_REPOSITORY: Repository,
                    INSTALL_TYPE_LOCAL: LocalFile
                }
                return components[activeTab.value]
            })

            function handleCancelInstall () {
                emit('update:value', false)
            }
            
            function handleConfirmInstall () {
                switch (activeTab.value) {
                    case INSTALL_TYPE_STORE:
                        importTemplateFromStore()
                        break
                    case INSTALL_TYPE_REPOSITORY:
                        console.log(INSTALL_TYPE_REPOSITORY)
                        break
                    case INSTALL_TYPE_LOCAL:
                        console.log(INSTALL_TYPE_LOCAL)
                        break
                }
                emit('confirm')
            }
            function handleSubChange (val) {
                switch (activeTab.value) {
                    case INSTALL_TYPE_STORE:
                        storeTemplateInfo.value = val
                        break
                    case INSTALL_TYPE_REPOSITORY:
                        console.log(INSTALL_TYPE_REPOSITORY)
                        break
                    case INSTALL_TYPE_LOCAL:
                        console.log(INSTALL_TYPE_LOCAL)
                        break
                }
            }

            function importTemplateFromStore () {
                try {
                    const res = store.dispatch('templates/importTemplateFromStore', {
                        projectId: projectId.value,
                        params: {
                            marketTemplateId: storeTemplateInfo.value.code,
                            marketTemplateProjectId: storeTemplateInfo.value.srcProjectId,
                            marketTemplateVersion: storeTemplateInfo.value.version
                        }
                    })
                    console.log(res)
                } catch (e) {
                    console.error(e)
                }
            }
            return {
                value,
                activeTab,
                dialogPosition,
                confirmBtnText,
                tabTypeMap,
                componentName,
                handleSubChange,
                handleCancelInstall,
                handleConfirmInstall
            }
        }
    })
</script>

<style lang="scss">
    .install-type-tab {
        .bk-tab-header {
            display: flex;
            justify-content: center;
        }
    }
    .install-type-tab {
        .bk-tab-section {
            padding: 0;
        }
    }
</style>
