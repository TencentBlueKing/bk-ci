<template>
    <div class="system-var-container">
        <div class="operate-row">
            <bk-input
                v-model="searchStr"
                :clearable="true"
                :placeholder="$t('newui.searchSysVar')"
                :right-icon="'bk-icon icon-search'"
            />
        </div>
        <div class="var-con-container">
            <param-group
                v-for="(group) in renderSysParamList"
                :key="group.name"
                :title="group.name"
                :tips="group.tips"
                :item-num="group.params.length"
                :show-content="group.isOpen === true"
            >
                <section slot="content">
                    <template v-for="env in group.params">
                        <env-item
                            :key="env.name"
                            :name="env.name"
                            :desc="env.desc"
                            :remark="env.remark"
                            :editable="editable"
                        />
                    </template>
                </section>
            </param-group>
        </div>
    </div>
</template>

<script>
    import { mapState, mapActions } from 'vuex'
    import ParamGroup from './children/param-group'
    import EnvItem from './children/env-item'
    export default {
        components: {
            ParamGroup,
            EnvItem
        },
        props: {
            container: {
                type: Object,
                default: () => ({})
            },
            editable: {
                type: Boolean,
                default: true
            }
        },
        data () {
            return {
                isLoading: false,
                searchStr: '',
                triggerParamList: [],
                sysParamList: [],
                noParamsTrigger: ['manualTrigger', 'remoteTrigger', 'timerTrigger']
            }
        },
        computed: {
            ...mapState('atom', [
                'commonParams'
            ]),
            atomCodeList () {
                const triggerList = (this.container?.elements || []).map(item => item.atomCode)
                return triggerList.filter(item => !this.noParamsTrigger.includes(item))
            },
            renderSysParamList () {
                if (!this.searchStr) {
                    return this.sysParamList
                } else {
                    const renderList = []
                    this.sysParamList.forEach((group, index) => {
                        const searchRes = group.params.filter(item => item.name.includes(this.searchStr) || item.desc.includes(this.searchStr))
                        renderList.push({
                            ...group,
                            params: searchRes,
                            isOpen: searchRes.length > 0
                        })
                    })
                    return renderList
                }
            }
        },
        watch: {
            atomCodeList () {
                this.$nextTick(this.initData)
            }
        },
        created () {
            this.initData()
        },
        methods: {
            ...mapActions('atom', [
                'requestTriggerParams'
            ]),
            async initData () {
                this.sysParamList = []
                this.sysParamList.splice(0, 0, ...this.commonParams)

                if (this.atomCodeList.length) {
                    this.triggerParams = await this.requestTriggerParams(this.atomCodeList)
                    this.sysParamList.splice(1, 0, ...this.triggerParams)
                }
                this.sysParamList[0] && (this.sysParamList[0].isOpen = true)
            }
        }
    }
</script>

<style lang="scss">
    .operate-row {
        margin: 16px 0;
    }
    .var-con-container {
        width: 100%;
        border-top: 1px solid #DCDEE5;
        .variable-item {
            position: relative;
            height: 40px;
            background: #fff;
            border: 1px solid #DCDEE5;
            border-top: none;
            padding-left: 24px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            cursor: pointer;
            &:hover {
                border-color: #C4C6CC;
                .var-con {
                    .var-names {
                        max-width: 362px;
                    }
                    .var-operate {
                        display: inline-block;
                    }
                }
            }
            .var-con {
                width: 100%;
                display: flex;
                align-items: center;
                justify-content: space-between;
                .var-names {
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                    max-width: 392px;
                    color: #313238;
                    font-size: 12px;
                    letter-spacing: 0;
                    line-height: 20px;
                }
                .desc-param {
                    border-bottom: 1px dashed #979BA5;
                }
                .var-operate {
                    display: none;
                    i {
                        margin-right: 16px;
                        cursor: pointer;
                        font-size: 14px;
                        color: #63656E;
                    }
                }
            }
        }
    }
</style>
