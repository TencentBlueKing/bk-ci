<template>
    <bk-dialog class="organization-dialog"
        v-model="showDialog"
        :width="width"
        :padding="0"
        :close-icon="organizationConf.closeIcon"
        :quick-close="organizationConf.quickClose"
        :show-footer="false"
        :ok-text="$t('store.保存')"
        :confirm-fn="toConfirmLogo"
        @cancel="toCloseDialog"
    >
        <main class="organization-select-content">
            <div class="organization-content">
                <div class="organization-card organization-tree">
                    <div class="info-header"> {{ $t('store.全部组织架构') }} </div>
                    <div class="tree-content">
                        <bk-big-tree show-checkbox
                            expand-on-click
                            ref="organizationTree"
                            :data="treeList"
                            :check-strictly="false"
                            :lazy-method="loadNodes"
                            @check-change="handleChange">
                        </bk-big-tree>
                    </div>
                </div>
                <div class="organization-card organization-selected">
                    <div class="info-header"> {{ $t('store.已选组织架构') }} </div>
                    <div class="selected-content">
                        <div class="selected-item" v-for="(row, index) in selectedList" :key="index">{{ row.displayName }}</div>
                    </div>
                </div>
            </div>
            <section class="handle-footer">
                <bk-button theme="primary" @click="toConfirmLogo" :loading="isLoading">{{ $t('store.保存') }}</bk-button>
                <bk-button @click="$emit('cancelHandle')" :disabled="isLoading">{{ $t('store.取消') }}</bk-button>
            </section>
        </main>
    </bk-dialog>
</template>

<script>
    export default {
        props: {
            showDialog: Boolean,
            isLoading: Boolean
        },
        data () {
            return {
                width: 715,
                treeList: [{ id: 0, name: this.$t('store.腾讯公司') }],
                selectedList: [],
                organizationConf: {
                    hasHeader: false,
                    hasFooter: false,
                    closeIcon: false,
                    quickClose: false
                }
            }
        },
        watch: {
            showDialog (val) {
                if (!val) {
                    this.selectedList = []
                    this.treeList = [{ id: 0, name: this.$t('store.腾讯公司') }]
                }
            }
        },
        methods: {
            handleChange (ids) {
                this.selectedList = []
                ids.forEach((id) => {
                    const node = this.$refs.organizationTree.getNodeById(id)
                    node.displayName = node.name
                    let parentNode = node.parent
                    while (parentNode) {
                        node.displayName = `${parentNode.name}/${node.displayName}`
                        parentNode = parentNode.parent
                    }
                    this.selectedList.push(node)
                })
            },

            async loadNodes (node) {
                let curType = ''
                switch (node.level) {
                    case 0:
                        curType = 'bg'
                        break
                    case 1:
                        curType = 'dept'
                        break
                    default:
                        curType = 'center'
                        break
                }
                try {
                    const res = await this.$store.dispatch('store/requestOrganizations', {
                        type: curType,
                        id: node.id
                    })
                    const data = []
                    const leaf = []
                    res.forEach(x => {
                        x.type = curType
                        data.push(x)
                        if (node.level === 2) leaf.push(x.id)
                    })
                    return { data, leaf }
                } catch (err) {
                    this.$bkMessage({ message: err.message || err, theme: 'error' })
                }
            },
            toConfirmLogo () {
                if (this.isLoading) return
                if (!this.selectedList.length) {
                    this.$bkMessage({
                        message: this.$t('store.请选择部门'),
                        theme: 'error'
                    })
                } else {
                    const deptInfos = []

                    this.selectedList.forEach(item => {
                        deptInfos.push({
                            deptId: item.id,
                            deptName: item.displayName
                        })
                    })
                    
                    const params = {
                        deptInfos
                    }

                    this.$emit('saveHandle', params)
                }
            }
        }
    }
</script>

<style lang="scss">
    @import '../assets/scss/conf';
    .organization-dialog {
        ::v-deep .bk-dialog-body {
            padding: 0;
        }
        .organization-content {
            display: flex;
            justify-content: space-between;
            padding: 0 24px;
        }
        .organization-card {
            width: 320px;
            height: 344px;
            border: 1px solid #C3CDD7;
            .info-header {
                padding: 10px;
                background: #FAFBFD;
                border-bottom: 1px solid #C3CDD7;
            }
            .tree-content,
            .selected-content {
                height: calc(100% - 42px);
                padding: 10px;
                overflow: auto;
            }
            .selected-content {
                padding: 0;
            }
            .selected-item {
                padding: 8px;
                border-bottom: 1px solid #C3CDD7;
            }
            .tree-drag-node {
                white-space: nowrap;
            }
        }
        .handle-footer {
            margin-top: 15px;
            padding: 12px 24px;
            border-top: 1px solid #DDE4EB;
            background-color: #fafbfd;
            text-align: right;
            button {
                margin-top: 0;
                margin-right: 0;
                width: 70px;
                min-width: 70px;
            }
        }
    }
</style>
