<template>
    <bk-dialog class="organization-dialog"
        v-model="showDialog"
        :width="width"
        :padding="0"
        :close-icon="organizationConf.closeIcon"
        :quick-close="organizationConf.quickClose"
        ok-text="保存"
        @confirm="toConfirmLogo"
        @cancel="toCloseDialog"
    >
        <main class="organization-select-content" v-bkloading="{ isLoading: organizationConf.isLoading }">
            <div class="organization-content">
                <div class="organization-card organization-tree">
                    <div class="info-header">全部组织架构</div>
                    <div class="tree-content">
                        <tree
                            ref="organizationTree"
                            :data="treeList"
                            :multiple="true"
                            :node-key="'id'"
                            :has-border="false"
                            @async-load-nodes="loadNodes"
                            @on-click="handleChange">
                        </tree>
                    </div>
                </div>
                <div class="organization-card organization-selected">
                    <div class="info-header">已选组织架构</div>
                    <div class="selected-content">
                        <div class="selected-item" v-for="(row, index) in selectedList" :key="index">{{ row.displayName }}</div>
                    </div>
                </div>
            </div>
        </main>
    </bk-dialog>
</template>

<script>
    import tree from '@/components/common/tree'

    export default {
        components: {
            tree
        },
        props: {
            showDialog: Boolean
        },
        data () {
            return {
                width: 715,
                treeList: [{ id: 0, name: '腾讯公司', async: true }],
                selectedList: [],
                organizationConf: {
                    hasHeader: false,
                    hasFooter: false,
                    closeIcon: false,
                    quickClose: false,
                    isLoading: false
                }
            }
        },
        computed: {
            routeName () {
                return this.$route.name
            },
            atomCode () {
                return this.$route.params.atomCode
            },
            templateCode () {
                return this.$route.params.templateCode
            }
        },
        watch: {
            showDialog (val) {
                if (!val) {
                    this.selectedList = []
                    this.treeList = [{ id: 0, name: '腾讯公司', async: true }]
                }
            }
        },
        methods: {
            handleChange (node, $event) {
                if ($event.target.className === 'checkbox-input') {
                    if ($event.target.checked) {
                        if (!node.parent || !node.parent.type) {
                            node.displayName = node.name
                        } else if (node.type === 'dept') {
                            node.displayName = `${node.parent.name}/${node.name}`
                        } else if (node.type === 'center') {
                            node.displayName = `${node.parent.parent.name}/${node.parent.name}/${node.name}`
                        }
                        this.selectedList.push(node)
                    } else {
                        this.selectedList = this.selectedList.filter(val => val.id !== node.id)
                    }
                }
            },
            async loadNodes (node) {
                const curType = node.type ? node.type === 'bg' ? 'dept' : 'center' : 'bg'
                this.$set(node, 'loading', true)

                try {
                    const res = await this.$store.dispatch('store/requestOrganizations', {
                        type: curType,
                        id: node.id
                    })

                    if (res.length) {
                        res.forEach(el => {
                            if (!node.hasOwnProperty('children')) {
                                this.$set(node, 'children', [])
                            }
                            el.async = node.type !== 'dept'
                            el.type = curType
                            node.children.push(el)
                        })
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    this.$set(node, 'loading', false)
                }
            },
            toCloseDialog () {
                this.$emit('cancelHandle')
            },
            async toConfirmLogo () {
                if (!this.selectedList.length) {
                    this.$bkMessage({
                        message: '请选择部门',
                        theme: 'error'
                    })
                } else {
                    let message, theme
                    const deptInfos = []

                    this.selectedList.map(item => {
                        deptInfos.push({
                            deptId: item.id,
                            deptName: item.displayName
                        })
                    })
                    
                    const params = {
                        deptInfos: deptInfos
                    }

                    if (this.routeName === 'visible') {
                        params.atomCode = this.atomCode
                    } else {
                        params.templateCode = this.templateCode
                    }

                    this.organizationConf.isLoading = true

                    try {
                        if (this.routeName === 'visible') {
                            await this.$store.dispatch('store/setVisableDept', { params })
                        } else {
                            await this.$store.dispatch('store/setTplVisableDept', { params })
                        }

                        message = '保存成功'
                        theme = 'success'
                    } catch (err) {
                        message = err.message ? err.message : err
                        theme = 'error'
                    } finally {
                        this.$bkMessage({
                            message,
                            theme
                        })

                        this.organizationConf.isLoading = false
                        this.$emit('saveHandle')
                    }
                }
            }
        }
    }
</script>

<style lang="scss">
    @import '../assets/scss/conf';
    .organization-dialog {
        .organization-content {
            display: flex;
            justify-content: space-between;
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
            padding: 10px 20px;
            border: 1px solid #DDE4EB;
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
