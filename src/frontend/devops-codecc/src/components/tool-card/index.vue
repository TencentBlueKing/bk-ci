<template>
    <div :class="['tool-card', { picked, disabled }]" :source="source" :type="type" @click="handleClick">
        <div class="tool-logo">
            <div class="tool-img"><img :src="toolData.logo"></div>
            <div class="tool-name"><em :title="$t(`${toolData.displayName}`)">{{$t(`${toolData.displayName}`)}}</em></div>
        </div>
        <div class="tool-desc">
            <div class="desc">
                <div class="tool-lang" :title="supportLangs">{{$t('支持语言')}} {{supportLangs}}</div>
                <div class="tool-summary" :title="toolData.description">{{toolData.description}}</div>

            </div>
        </div>
        <!-- <corner :text="$t('推荐')" theme="warning" v-if="toolData.recommend" /> -->
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    // import Corner from '@/components/corner'

    export default {
        name: 'tool-card',
        components: {
            // Corner
        },
        props: {
            tool: {
                type: Object,
                default () {
                    return {}
                }
            },
            picked: {
                type: Boolean,
                default: false
            },
            type: {
                type: String,
                default: 'pick',
                validator (value) {
                    if (['pick', 'manage'].indexOf(value) === -1) {
                        console.error(`type property is not valid: '${value}'`)
                        return false
                    }
                    return true
                }
            },
            source: {
                type: String,
                default: 'new'
            }
        },
        data () {
            return {
                statusEnabled: !this.disabled
            }
        },
        computed: {
            ...mapState([
                'toolMeta'
            ]),
            ...mapState('task', {
                taskDetail: 'detail'
            }),
            ...mapState('tool', {
                toolMap: 'mapList'
            }),
            toolData () {
                // 兼容已有工具和更多工具数据结构，已有工具的数据结构中没有logo等字段值
                // 因此在这里统一成原始工具列表中的工具数据
                return this.toolMap[this.tool.toolName || this.tool.name] || {}
            },
            supportLangs () {
                const toolLang = this.toolData.lang
                const names = this.toolMeta.LANG.map(lang => {
                    if (lang.key & toolLang) {
                        return lang.name
                    }
                }).filter(name => name)
                return names.join('、')
            },
            isAdded () {
                return this.tool.taskId > 0
            },
            hasRules () {
                let hasRules = false
                hasRules = this.tool.toolName !== 'DUPC' && this.tool.toolName !== 'CCN'
                return hasRules
            },
            disabled () {
                // 已停用工具
                this.statusEnabled = !(this.isAdded && this.tool.followStatus === 6)
                return this.isAdded && this.tool.followStatus === 6
            },
            statusSwitcherDisabled () {
                return this.statusEnabled && this.taskDetail.enableToolList.length === 1
            },
            statusSwitcherTitle () {
                let title = this.statusEnabled ? this.$t('停用') : this.$t('启用')
                if (this.statusSwitcherDisabled) {
                    title = this.$t('不能停用所有工具')
                }
                return title
            }
        },
        methods: {
            handleClick (e) {
                if (this.type === 'pick') {
                    this.picked = !this.picked
                }
                this.$emit('click', e, {
                    name: this.toolData.name,
                    picked: this.picked,
                    disabled: this.disabled,
                    source: this.source
                })
            },
            toRules () {
                if (this.statusEnabled) {
                    const params = this.$route.params
                    params.toolId = this.toolData.name
                    this.$router.push({
                        name: 'task-settings-checkerset',
                        params
                    })
                }
            }
        }
    }
</script>

<style lang="postcss">
    @import '../../css/mixins.css';

    .tool-card {
        width: 485px;
        height: 174px;
        border-radius: 2px;
        border:1px solid #d1e5f2;
        float: left;
        position: relative;

        &[type="pick"] {
            cursor: pointer;
        }

        &.picked {
            border-color: #3a84ff;
            @mixin triangle-check-bottom-right 11, #3a84ff;
            background: #fbfdff;
        }

        .tool-logo {
            float: left;
            width: 126px;
            padding: 40px 5px;
            text-align: center;

            .tool-img {
                line-height: 0;
                height: 64px;
                img {
                    max-width: 64px;
                }
            }
            .tool-name {
                text-align: center;
                color: #46c2c7;
                overflow: hidden;
                white-space: nowrap;
                text-overflow: ellipsis;
                em {
                    font-style: normal;
                    font-size: 18px;
                }
            }
        }
        .tool-desc {
            overflow: hidden;
            font-size: 14px;
            padding: 20px 21px 20px 0;

            .desc {
                height: 115px;
            }
            .tool-lang {
                color: #63656e;
                font-weight: 600;
                margin-top: 4px;
                padding: 4px;
                background: #e8f5fd;
                @mixin text-ellipsis 2;
            }
            .tool-summary {
                color: #63656e;
                padding-top: 16px;
                line-height: 26px;
                @mixin text-ellipsis 3;
            }
            .action-bar {
                text-align: right;
                .is-checked {
                    background-color: #3a84ff;
                }
                .disabled {
                    color: #b2c2dc;
                    cursor: no-drop;
                }
            }
        }
    }
</style>
