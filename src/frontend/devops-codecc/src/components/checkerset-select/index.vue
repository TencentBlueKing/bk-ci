<template>
    <div>
        <bk-form :label-width="130" ref="checkerset">
            <bk-form-item v-for="(value, key) in data" :key="key" :label="key" :property="key">
                <bk-select class="checker-select"
                    multiple
                    searchable
                    v-model="selectSets[key]"
                    @change="handleChange">
                    <bk-option-group
                        v-for="(v, k) in value"
                        :name="k"
                        :key="k">
                        <bk-option v-for="option in v"
                            class="checker-option-content"
                            :key="option.checkerSetId"
                            :id="option.checkerSetId"
                            :name="option.checkerSetName">
                            <slot>
                                <span class="checker-name checker-option dib" :title="option.checkerSetName">{{option.checkerSetName}}</span>
                                <span class="checker-option dib">{{option.checkerCount || 0}}{{$t('条工具')}}</span>
                                <span class="checker-option dib">{{(option.toolList && option.toolList.length) || 0}}{{$t('个工具')}}</span>
                                <span class="checker-option dib">
                                    <span class="tag" v-for="item in option.catagories" :key="item.enName">{{item.cnName}}</span>
                                </span>
                                <i class="bk-option-icon bk-icon icon-check-1"
                                    v-if="selectSets[key] && selectSets[key].includes(option.checkerSetId)">
                                </i>
                            </slot>
                        </bk-option>
                    </bk-option-group>
                    <div slot="extension" @click="hanldeToCheckset" class="bk-selector-create-item">
                        <a>
                            <i class="bk-icon icon-plus-circle" />
                            {{$t('更多规则集')}}
                        </a>
                    </div>
                </bk-select>
            </bk-form-item>
        </bk-form>
    </div>
</template>

<script>
    export default {
        name: 'checkerset-select',
        components: {
        },
        props: {
            data: {
                type: Object,
                default: {}
            }
        },
        data () {
            return {
                selectSets: {}
            }
        },
        watch: {
            data (data) {
                const selectSets = this.selectSets
                let toolList = []
                for (const key in data) {
                    if (!selectSets[key] || !selectSets[key].length) {
                        const list = []
                        for (const k in data[key]) {
                            data[key][k].map(item => {
                                if (item.defaultCheckerSet) {
                                    list.push(item.checkerSetId)
                                    toolList = toolList.concat(item.toolList)
                                }
                            })
                        }
                        selectSets[key] = list
                    }
                }
                this.handleChange()
                this.selectSets = { ...selectSets }
            }
        },
        methods: {
            handleChange (value, options) {
                const checkerset = this.getCheckerset()
                const toolStr = checkerset.map(item => item.toolList).join()
                const toolList = Array.from(new Set(toolStr.split(','))).filter(item => item)
                this.$emit('handleToolChange', toolList)
            },
            getCheckerset () {
                let checkerset = []
                for (const key in this.selectSets) {
                    for (const k in this.data[key]) {
                        const value = this.data[key][k].filter(item => this.selectSets[key].includes(item.checkerSetId))
                        checkerset = checkerset.concat(value)
                    }
                }
                return checkerset
            },
            handleValidate () {
                let hasError = false
                const formItems = this.$refs.checkerset.formItems
                const selectSets = this.selectSets
                for (const key in this.data) {
                    const formItem = formItems.find(item => item.label === key)
                    if (!selectSets[key] || !selectSets[key].length) {
                        formItem.validator.state = 'error'
                        formItem.validator.content = '必填项'
                        hasError = true
                    }
                }
                return !hasError
            },
            hanldeToCheckset () {
                const projectId = this.$route.params.projectId
                window.open(`${window.JUMP_SITE_URL}/codecc/${projectId}/checkerset/list`, '_blank')
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .checker-label {
        font-size: 12px;
        width: 116px;
        text-align: right;
        padding-right: 16px;
        display: inline-block;
        line-height: 32px;
        position: relative;
        top: -10px;
    }
    .checker-select {
        display: inline-block;
        width: 567px;
    }
    .dib {
        display: inline-block;
    }
    .checker-option-content {
        height: 32px;
    }
    .checker-option {
        width: 100px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        &.checker-name {
            width: 190px;
        }
    }
    >>> .bk-form .bk-label {
        font-weight: 400 !important;
        font-size: 12px;
    }
    .tag {
        background: #c9dffa;
        border-radius: 2px;
        padding: 2px 8px;
        display: inline-block;
        line-height: 18px;
    }
    .bk-selector-create-item {
        cursor: pointer;
    }
</style>
