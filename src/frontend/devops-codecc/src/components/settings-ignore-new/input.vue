<template>
    <bk-dialog
        class="system-default"
        v-model="inputVisible"
        :theme="'primary'"
        :width="579"
        :title="$t('设置默认路径')"
        @confirm="save"
        @cancel="cancel"
        :position="{ top: 100, left: 5 }">
        <bk-transfer
            :title="title"
            :source-list="list"
            :target-list="selected"
            :display-key="key"
            :setting-key="key"
            @change="change">
        </bk-transfer>
    </bk-dialog>
</template>
<script>
    import { mapState } from 'vuex'

    export default {
        props: {
            visible: Boolean,
            list: Array,
            selected: Array
        },
        data () {
            return {
                selectedList: {
                    taskId: '',
                    defaultFilterPath: [],
                    pathType: 'DEFAULT'
                },
                title: [
                    this.$t('待添加路径'),
                    this.$t('已添加路径')
                ]
            }
        },
        computed: {
            inputVisible: {
                get () {
                    return this.visible
                },
                set (value) {
                    this.$emit('visibleChange', value)
                }
            },
            ...mapState([
                'taskId'
            ])
        },
        methods: {
            change (sourceList, targetList, tagetValueList) {
                this.sourceLength = sourceList.length
                this.targetLength = targetList.length
                this.selectedList.defaultFilterPath = targetList
            },
            save () {
                this.selectedList.taskId = this.taskId
                this.$store.dispatch('task/createIgnore', this.selectedList).then(res => {
                    if (res === true) {
                        this.$bkMessage({ theme: 'success', message: this.$t('路径添加成功') })
                        this.$store.dispatch('task/ignore', this.taskId)
                    }
                }).catch(e => {
                    this.$bkMessage({ theme: 'error', message: this.$t('路径添加失败') })
                })
                this.inputVisible = false
            },
            cancel () {
                this.inputVisible = false
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .system-default {
        .bk-dialog-header-inner {
            text-align: left;
        }
    }
</style>
