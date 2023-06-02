<template>
    <bk-select
        ref="select"
        :value="value"
    >
        <bk-big-tree
            :data="treeData"
            class="tree-select"
            enable-title-tip
            selectable
            ref="tree"
            :options="{ folderKey: 'hasNext' }"
            :lazy-method="expandNode"
            :default-selected-node="value"
            @select-change="handleSelectChange"
        >
        </bk-big-tree>
    </bk-select>
</template>

<script>
    import { mapActions } from 'vuex'
    export default {
        props: {
            value: {
                type: String,
                default: ''
            }
        },
        data () {
            return {
                treeData: []
            }
        },
        watch: {
            value (val) {
                console.log('watch value', val)
            }
        },
        created () {
            this.init()
        },
        methods: {
            ...mapActions('experience', [
                'requestOrgs'
            ]),
            async fetchOrgs (id = '0') {
                const res = await this.requestOrgs(id)
                return res.map(item => ({
                    ...item,
                    parentId: id,
                    hasNext: true,
                    children: []
                }))
            },
            async init () {
                this.treeData = await this.fetchOrgs()
            },
            async expandNode (node) {
                const children = await this.fetchOrgs(node.id)
                // TODO: HACK
                this.$refs.tree.registryOptions(children)
                return {
                    data: children
                }
            },
            handleSelectChange (node) {
                this.$emit('input', node)
            }
        }
        
    }
</script>
<style lang="scss">
    
</style>
