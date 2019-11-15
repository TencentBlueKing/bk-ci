<template>
    <div :class="['tool-card', { picked, disabled }]" :type="type" @click="handleClick">
        <div class="tool-logo">
            <div class="tool-img"><img :src="toolData.logo" :alt="$t(`toolName.${toolData.displayName}`)"></div>
            <div class="tool-name"><em>{{$t(`toolName.${toolData.displayName}`)}}</em></div>
        </div>
        <div class="tool-desc">
            <div class="tool-summary" :title="toolData.description">{{toolDesc}}</div>
            <div class="tool-lang" :title="supportLangs">{{$t('tools.支持')}}: {{supportLangs}}</div>
            <div class="action-bar" v-if="type === 'manage'">
                <a v-if="isAdded && hasRules" @click="toRules"><i class="bk-iconcool bk-icon-shezhi" :class="!statusEnabled ? 'disabled' : ''"></i></a>
                <bk-switcher
                    :key="+new Date()"
                    v-if="isAdded"
                    v-model="statusEnabled"
                    size="small"
                    class="bk-switcher-xsmall"
                    :disabled="statusSwitcherDisabled"
                    :title="statusSwitcherTitle"
                    @click.native="handleStatusChange(statusEnabled)"
                >
                </bk-switcher>
                <bk-button
                    v-if="!isAdded"
                    theme="primary"
                    size="small"
                    class="bk-button-xsmall"
                    @click="handleAddButtonClick"
                >
                    {{$t('op.添加')}}
                </bk-button>
            </div>
        </div>
        <corner :text="$t('short.推荐')" theme="warning" v-if="toolData.recommend" />
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import Corner from '@/components/corner'

    export default {
        name: 'tool-card',
        components: {
            Corner
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
            toolDesc () {
                const description = this.toolData.description || ''
                const cnCharLengthHalf = (description.match(/([\u4e00-\u9fa5]+)/g) || []).join('').length / 2
                if (description.length + cnCharLengthHalf > 70) {
                    return `${description.substr(0, 70 - cnCharLengthHalf)}....`
                }
                return description
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
                let title = this.statusEnabled ? this.$t('suspend.停用') : this.$t('suspend.启用')
                if (this.statusSwitcherDisabled) {
                    title = this.$t('suspend.不能停用所有工具')
                }
                return title
            }
        },
        methods: {
            handleAddButtonClick () {
                this.$emit('add-click', this.toolData.name)
            },
            handleStatusChange (statusEnabled) {
                this.statusEnabled = !statusEnabled
                this.$emit('status-change', statusEnabled, this.toolData.name)
            },
            handleClick (e) {
                if (this.type === 'pick') {
                    this.picked = !this.picked
                }
                this.$emit('click', e, {
                    name: this.toolData.name,
                    picked: this.picked,
                    disabled: this.disabled
                })
            },
            toRules () {
                if (this.statusEnabled) {
                    const params = this.$route.params
                    params.toolId = this.toolData.name
                    this.$router.push({
                        name: 'tool-rules',
                        params
                    })
                }
            }
        }
    }
</script>

<style lang="postcss">
    @import '../../css/mixins.css';
    @import '../../assets/bk_icon_font/style.css';

    .tool-card {
        width: 320px;
        min-height: 110px;
        border-radius: 2px;
        box-shadow:0px 1px 3px 0px rgba(0, 0, 0, 0.08);
        border:1px solid #cee0ff;
        float: left;
        padding: 8px;
        position: relative;

        &[type="pick"] {
            cursor: pointer;
        }

        &.picked {
            border-color: #3a84ff;
            @mixin triangle-check-bottom-right 44, 30, #3a84ff;
        }

        .tool-logo {
            float: left;
            width: 95px;
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
                color: #b2c2dc;
                em {
                    font-style: normal;
                    font-weight: 700;
                    font-size: 12px;
                }
            }
        }
        .tool-desc {
            overflow: hidden;

            .tool-lang {
                font-size: 12px;
                color: #bac9de;
                margin-top: 4px;
                @mixin ellipsis;
            }
            .tool-summary {
                font-size: 12px;
                color: #7b93b9;
                height: 48px;
            }
            .action-bar {
                text-align: right;
                .is-checked {
                    background-color: #3a84ff;
                }
                .bk-icon-shezhi {
                    cursor: pointer;
                }
                .disabled {
                    color: #b2c2dc;
                    cursor: no-drop;
                }
            }
        }
    }
</style>
