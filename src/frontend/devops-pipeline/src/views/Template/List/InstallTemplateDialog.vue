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

<script setup name='InstallTemplateDialog'>
    import LocalFile from '@/components/Template/Install/LocalFile'
    import Repository from '@/components/Template/Install/Repository'
    import StoreTemplateList from '@/components/Template/Install/StoreTemplateList'
    import UseInstance from '@/hook/useInstance'
    import {
        INSTALL_TYPE_LOCAL,
        INSTALL_TYPE_REPOSITORY,
        INSTALL_TYPE_STORE
    } from '@/store/modules/templates/constants'
    import { computed, defineEmits, defineProps, ref } from 'vue'

    const { proxy, i18n, bkMessage } = UseInstance()
    defineProps({
        value: {
            type: Boolean,
            default: false
        }
    })
    const emit = defineEmits(['update:value', 'confirm'])

    const storeTemplateInfo = ref({})
    const activeTab = ref(INSTALL_TYPE_STORE)
    const dialogPosition = {
        top: '120'
    }
    const projectId = computed(() => proxy.$route.params.projectId)
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

    async function importTemplateFromStore () {
        try {
            await proxy.$store?.dispatch('templates/importTemplateFromStore', {
                        projectId: projectId.value,
                        params: {
                            marketTemplateId: storeTemplateInfo.value.code,
                            marketTemplateProjectId: storeTemplateInfo.value.srcProjectId,
                            marketTemplateVersion: storeTemplateInfo.value.version
                        }
                    })
        } catch (e) {
            bkMessage({
                theme: 'error',
                message: e.message || e
            })
        }
    }
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
