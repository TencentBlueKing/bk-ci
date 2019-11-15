<template>
    <bk-dialog
        v-model="showDialog"
        class="devops-ask-permission-dialog"
        :width="width"
        theme="primary"
        :title="title"
        ok-text="去申请"
        @confirm="toApplyPermission"
        @cancel="handleClose"
    >
        <main class="ask-permission-table">
            <bk-table
                :max-height="300"
                :data="noPermissionList"
                empty-text="暂无数据"
            >
                <bk-table-column
                    label="名称"
                    prop="resource"
                />
                <bk-table-column
                    label="名称"
                    prop="option"
                />
            </bk-table>
        </main>
    </bk-dialog>
</template>

<script lang='ts'>
    import Vue from 'vue'
    import { Component, Prop } from 'vue-property-decorator'
    import eventBus from '../../utils/eventBus'

    @Component
    export default class AskPermissionDialog extends Vue {
        @Prop({ default: 640 })
        width: number | string

        @Prop({ default: '无权限操作' })
        title: string

        @Prop({ default: [] })
        noPermissionList: Permission[]

        @Prop({ default: '/console/perm/apply-perm' })
        applyPermissionUrl: string

        showDialog: boolean = false

        created () {
          eventBus.$on('update-permission-props', props => {
            Object.keys(props).map(prop => {
              this[prop] = props[prop]
            })
            this.showDialog = true
          })
        }

        handleClose (done) {
          done()
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
