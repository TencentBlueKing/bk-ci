<template>
    <bk-dialog
        v-model="showDialog"
        class="devops-ask-permission-dialog"
        :width="width"
        theme="primary"
        :title="i18nTitle"
        :ok-text="$t('goApply')"
        @confirm="toApplyPermission"
    >
        <main class="ask-permission-table">
            <bk-table
                :max-height="300"
                :data="noPermissionList"
                :empty-text="$t('emptyData')"
            >
                <bk-table-column
                    :label="$t('name')"
                    prop="resource"
                />
                <bk-table-column
                    :label="$t('name')"
                    prop="option"
                />
            </bk-table>
        </main>
    </bk-dialog>
</template>

<script lang='ts'>
    import Vue from 'vue'
    import { Component, Prop } from 'vue-property-decorator'
    import { State, Action } from 'vuex-class'
    import eventBus from '../../utils/eventBus'

    @Component
    export default class AskPermissionDialog extends Vue {
        @Prop({ default: 640 })
        width: number | string

        @Prop()
        title: any = ''

        @Prop({ default: [] })
        noPermissionList: Permission[]

        @Prop({ default: '/console/perm/apply-perm' })
        applyPermissionUrl: string

        @State isPermissionDialogShow
        @Action togglePermissionDialog
        
        get showDialog (): boolean {
            return this.isPermissionDialogShow
        }

        set showDialog (v) {
            this.togglePermissionDialog(v)
        }

        get i18nTitle () {
            return this.title || this.$t('accessDeny.noOperateAccess')
        }

        created () {
            eventBus.$on('update-permission-props', props => {
                Object.keys(props).map(prop => {
                    this[prop] = props[prop]
                })
                this.showDialog = true
            })
        }

        toApplyPermission (done) {
            window.open(this.applyPermissionUrl, '_blank')
            done()
            this.showDialog = false
        }
    }
</script>

<style lang="scss">
    .ask-permission-table {
        .devops-table {
            width: 100%;
        }
    }
</style>
