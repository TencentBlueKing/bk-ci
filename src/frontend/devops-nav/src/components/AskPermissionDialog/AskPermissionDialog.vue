<template>
    <bk-dialog
        v-model="showDialog"
        class="devops-ask-permission-dialog"
        :width="width"
        theme="primary"
        :ok-text="$t('goApply')"
        @confirm="toApplyPermission"
    >
        <div class="ask-permission-header">
            <span class="header-icon">
                <icon name="lock-radius" size="80" />
            </span>
            <h3>{{ i18nTitle }}</h3>
        </div>
        <main class="ask-permission-table">
            <bk-table
                v-if="showDialog"
                :max-height="300"
                :data="permissionTableData"
                :empty-text="$t('emptyData')"
            >
                <bk-table-column
                    :label="$t('permissionActionLabel')"
                    prop="action"
                />
                <bk-table-column
                    :label="$t('permissionResourceLabel')"
                    prop="resource"
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
    import { isProjectResource, resourceTypeMap, resourceAliasMap, actionAliasMap } from '../../../../common-lib/permission-conf'

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
        @Action getPermRedirectUrl
        
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
                Object.keys(props).forEach(prop => {
                    this[prop] = props[prop]
                })
                this.showDialog = true
            })
        }

        getPermissionBody ({ actionId, resourceId, instanceId, projectId }) {
            const dealInstanceIds = instanceId.map(instance => ({
                id: instance.id,
                type: resourceTypeMap[resourceId]
            }))
            if (!isProjectResource(resourceId) && projectId) {
                dealInstanceIds.unshift({
                  id: projectId,
                  type: resourceTypeMap.PROJECT
                })
            }
            return {
                actionId,
                resourceId,
                instanceId: dealInstanceIds
            }
        }

        get permissionTableData () {
          return this.noPermissionList.map(item => {
              const instanceNames = item.instanceId.map(instance => instance.name).join(',')
              return {
                resource: `${resourceAliasMap[item.resourceId]}${item.instanceId.length ? ': ' + instanceNames : ''}`,
                action: actionAliasMap[item.actionId] ? actionAliasMap[item.actionId].alias : '--'
              }
          })
        }

        async toApplyPermission () {
          try {
              // const body = this.noPermissionList.map(perm => this.getPermissionBody(perm))
              // console.log('permBody', body)
              // const redirectUrl = await this.getPermRedirectUrl(body)
              window.open(this.applyPermissionUrl, '_blank')
              this.showDialog = false
              this.$bkInfo({
                  title: this.$t('permissionRefreshtitle'),
                  subTitle: this.$t('permissionRefreshSubtitle'),
                  okText: this.$t('permissionRefreshOkText'),
                  cancelText: this.$t('close'),
                  confirmFn: () => {
                      location.reload()
                  }
              })
          } catch (e) {
              console.error(e)
          }
        }
    }
</script>

<style lang="scss">
    .devops-ask-permission-dialog {
      .ask-permission-header {
        text-align: center;
        .header-icon {
          height: 100px;
          width: 120px;
          padding: 10px 0;
          display: inline-block;
        }
        h3 {
          margin: 6px 0 24px;
          color: #63656e;
          font-size: 20px;
          font-weight: normal;
          line-height: 1;
        }
      }
      .ask-permission-table {
          width: 100%;
      }
    }
</style>
