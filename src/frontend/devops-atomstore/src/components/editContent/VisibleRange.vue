<template>
    <div
        class="container"
        ref="visibleError"
    >
        <p class="form-title">{{ $t('store.可见范围') }}</p>
        <div class="form-item-container">
            <bk-radio-group
                v-model="templateForm.fullScopeVisible"
                @change="radioChange"
            >
                <bk-radio
                    :value="true"
                    class="mr20"
                >
                    {{ $t('store.全部可见') }}
                </bk-radio>
                <bk-radio
                    :value="false"
                >
                    {{ $t('store.部分可见') }}
                </bk-radio>
            </bk-radio-group>
            <div
                v-if="visibleError"
                class="error-tips"
            >
                {{ $t('store.可见范围不能为空') }}
            </div>
        </div>
    
        <div v-if="!templateForm.fullScopeVisible">
            <div class="btn-group">
                <bk-button
                    theme="primary"
                    outline
                    @click="handleAdd"
                >
                    {{ $t('store.添加') }}
                </bk-button>
                <bk-button
                    class="ml10"
                    outline
                    @click="handleBeathDelete"
                >
                    {{ $t('store.批量删除') }}
                </bk-button>
            </div>

            <bk-table
                :data="visibleList"
                @select="select"
                @select-all="selectAll"
            >
                <bk-table-column
                    type="selection"
                    :min-width="30"
                    width="30"
                    align="center"
                />
                <bk-table-column
                    :label="$t('store.可见对象')"
                    prop="deptName"
                />
                <bk-table-column
                    :label="$t('store.操作')"
                    :width="200"
                >
                    <template #default="{ row }">
                        <bk-button
                            text
                            theme="primary"
                            @click="handleDelete(row)"
                        >
                            {{ $t('store.删除') }}
                        </bk-button>
                    </template>
                </bk-table-column>
            </bk-table>
            <VisibleRangeDialog
                ref="visibleRef"
                :show-dialog="showDialog"
                :is-loading="false"
                :select-data="visibleList"
                @saveHandle="saveHandle"
                @cancelHandle="cancelHandle"
            >
            </VisibleRangeDialog>
        </div>
    </div>
</template>

<script>
    import VisibleRangeDialog from '@/components/VisibleRangeDialog'
    export default {
        name: 'VisibleRange',
        components: { VisibleRangeDialog },
        props: {
            templateForm: {
                type: Object,
                required: true,
                default: () => {}
            },
            type: String
        },
        data () {
            return {
                showDialog: false,
                isLoading: true,
                visibleError: false,
                visibleList: []
            }
        },
        watch: {
            visibleList (newVal) {
                this.$emit('updateTemplateForm', {
                    deptInfos: this.visibleList
                })
                if (newVal.length) {
                    this.visibleError = false
                }
            }
        },
        mounted () {
            this.visibleList = this.templateForm.deptInfos.map(item => ({
                ...item,
                selected: false
            }))
        },
        methods: {
            async checkValid () {
                if (!this.templateForm.fullScopeVisible && !this.visibleList.length) {
                    this.visibleError = true
                    const errorEle = this.$refs.visibleError
                    if (errorEle) errorEle.scrollIntoView()
                    return false
                }
                return true
            },
            radioChange () {
                this.visibleError = false
            },
            handleAdd () {
                this.showDialog = true
            },
            handleBeathDelete () {
                const target = this.visibleList.filter(val => {
                    if (val.selected) {
                        return val.deptId || val.deptId === 0
                    }
                    return false
                })
                if (!target.length) {
                    this.$bkMessage({
                        message: this.$t('store.请至少选择一个可见对象'),
                        theme: 'error',
                        limit: 1
                    })
                    return
                }
                this.$bkInfo({
                    title: this.$t('store.删除'),
                    subTitle:this.$t('store.确定删除选中的可见对象？'),
                    confirmFn: () => {
                        this.visibleList = this.visibleList.filter(item => !target.some(targetItem => targetItem.deptId === item.deptId))

                        const ids = target.map(item => String(item.deptId))
                        this.$refs.visibleRef.clearChecked(ids, false)
                    }
                })
            },
            handleDelete (row) {
                this.$bkInfo({
                    title: this.$t('store.删除'),
                    subTitle:this.$t('store.确定删除选中的可见对象？'),
                    confirmFn: () => {
                        this.visibleList = this.visibleList.filter(item => item.deptId !== row.deptId)
                        this.$refs.visibleRef.clearChecked(row.deptId, false)
                    }
                })
            },
            saveHandle (params) {
                params.deptInfos.forEach(item => {
                    item.selected = false
                })
                this.visibleList = params.deptInfos
                this.showDialog = false
            },
            cancelHandle () {
                this.showDialog = false
            },
            select (_, row) {
                row.selected = !row.selected
            },
            selectAll (selection) {
                this.visibleList.forEach((item) => {
                    const isSelected = selection.findIndex((x) => x.deptId === item.deptId) > -1
                    item.selected = isSelected
                })
            }
        }

    }
</script>

<style lang="scss" scoped>
.btn-group {
  margin: 12px 0;
  padding-top: 12px;
  border-top: 1px solid #edeef2;
}
</style>