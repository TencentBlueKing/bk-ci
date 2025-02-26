<template>
    <bk-dialog
        v-model="value"
        width="1080"
        ext-class="install-template-dialog"
        header-position="left"
        :title="$t('template.installOrImportTemplate')"
        :position="dialogPosition"
        :ok-text="confirmBtnText"
        @confirm="handleConfirmInstall"
        @cancel="handleCancelInstall"
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
        />
    </bk-dialog>
</template>

<script>
    import StoreTemplateList from '@/components/template/install/StoreTemplateList'
    import Repository from '@/components/template/install/Repository'
    import LocalFile from '@/components/template/install/LocalFile'
    export default {
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
        data () {
            return {
                activeTab: 'store',
                dialogPosition: {
                    top: '120'
                }
            }
        },
        computed: {
            confirmBtnText () {
                return this.activeTab === 'store' ? this.$t('template.install') : this.$t('template.import')
            },
            tabTypeMap () {
                return [
                    {
                        name: 'store',
                        label: this.$t('template.store')
                    },
                    {
                        name: 'repository',
                        label: this.$t('template.repository')
                    },
                    {
                        name: 'local',
                        label: this.$t('template.localFile')
                    }
                ]
            },
            componentName () {
                const components = {
                    store: StoreTemplateList,
                    repository: Repository,
                    local: LocalFile
                }
                return components[this.activeTab]
            }
        },
        methods: {
            handleCancelInstall () {
                this.$emit('update:value', false)
            }
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
