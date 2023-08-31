<template>
    <div class="pipeline-model-arrangement">
        <pipeline
            :editable="false"
            :show-header="false"
            v-bind="$props"
            v-on="$listeners"
        >
        </pipeline>
        <aside
            :class="['pipeline-params-aside', {
                'params-aside-visible': isParamsAsideVisible
            }]"
            v-bk-clickoutside="hidePipelineParamsAside"
        >
            <span @click="toggleParamsAside" class="pipeline-params-entry">
                <i class="devops-icon icon-angle-double-right"></i>
                {{$t('var')}}
            </span>
            <bk-tab class="pipeline-params-aside-content" type="unborder-card">
                <bk-tab-panel
                    name="build-params"
                    :label="$t('template.pipelineVar')"
                >
                    <bk-alert
                        type="info"
                        :title="$t('refParamsTips')"
                    />

                    <p class="param-type-desc">
                        <span class="param-indicator">
                            <i class="visible-param-dot" />
                            {{ $t('buildParams') }}
                        </span>
                        <span class="param-indicator">
                            <i class="required-param-dot" />
                            {{ $t('requiredParams') }}
                        </span>
                        <span class="param-indicator">
                            <i class="readonly-param-dot" />
                            {{ $t('readonlyParams') }}
                        </span>
                    </p>
                    <ul v-if="params.length > 0" class="pipeline-params-list">
                        <li v-for="item in params" :key="item.id">
                            <p>
                                <label v-bk-tooltips="item.descTips" :class="{
                                    'has-param-desc': item.desc
                                }">
                                    {{ item.id }}
                                </label>
                                <span>
                                    {{ item.defaultValue || '--' }}
                                </span>
                            </p>
                            <span>
                                <i class="visible-param-dot" />
                                <i v-if="item.required" class="required-param-dot" />
                                <i v-if="item.readOnly" class="readonly-param-dot" />
                            </span>
                        </li>
                    </ul>
                </bk-tab-panel>
                <bk-tab-panel
                    name="build-version"
                    :label="$t('history.tableMap.recommendVersion')"
                >
                    <bk-alert
                        type="info"
                        :title="$t('refVersionTips')"
                    />
                    <ul class="pipeline-reccomend-version-conf">
                        <li v-for="(item, index) in versionConf" :key="index">
                            <label>
                                {{ item.label }}
                                <span v-if="item.labelDesc">
                                    {{ item.labelDesc }}
                                </span>
                            </label>
                            <component :is="item.component" v-bind="item.props" v-on="item.listeners" />
                        </li>
                    </ul>
                </bk-tab-panel>
            </bk-tab>
        </aside>
    </div>
</template>

<script>
    import { mapGetters } from 'vuex'
    import Pipeline from '@/components/Pipeline'
    import BuildVersion from '@/components/BuildVersion'
    import EnumInput from '@/components/atomFormField/EnumInput'
    export default {
        components: {
            Pipeline,
            BuildVersion,
            EnumInput
        },
        props: {
            pipeline: {
                type: Object,
                required: true
            }
        },
        data () {
            return {
                isParamsAsideVisible: false
            }
        },
        computed: {
            ...mapGetters('atom', [
                'buildNoRules',
                'curPipelineParams',
                'curPipelineBuildNoConfig'
            ]),
            params () {
                return this.curPipelineParams.map(item => ({
                    ...item,
                    descTips: {
                        content: item.desc,
                        delay: [300, 0],
                        allowHTML: false
                    }
                }))
            },
            versionConf () {
                return [{
                    label: this.$t('versionNum'),
                    labelDesc: this.$t('mainMinorPatch'),
                    component: 'BuildVersion',
                    props: {
                        disabled: true,
                        value: this.curPipelineBuildNoConfig?.semver
                    }
                }, {
                    label: this.$t('buildNum'),
                    component: 'bk-input',
                    props: {
                        disabled: true,
                        value: this.curPipelineBuildNoConfig?.buildNo ?? ''
                    }
                }, {
                    component: 'enum-input',
                    props: {
                        disabled: true,
                        list: this.buildNoRules,
                        value: this.curPipelineBuildNoConfig?.buildNoType
                    }
                }]
            }
        },
        methods: {
            toggleParamsAside () {
                this.isParamsAsideVisible = !this.isParamsAsideVisible
            },
            hidePipelineParamsAside () {
                this.isParamsAsideVisible = false
            }
        }
    }
</script>

<style lang="scss">
    @import "@/scss/mixins/ellipsis";
    .pipeline-model-arrangement {
        height: 100%;
        overflow: hidden;
    }
    .pipeline-params-aside {
        position: absolute;
        top: 0;
        right: 0;
        width: 480px;
        height: 100%;
        font-size: 12px;
        transform: translateX(100%);
        z-index: 999;
        border-left: 1px solid #DCDEE5;
        transition: all .3s ease;
        &.params-aside-visible {
            transform: translateX(0);
            .pipeline-params-entry > i {
                transform: rotate(0);
            }
        }
        .pipeline-params-entry {
            width: 24px;
            position: absolute;
            background: #C4C6CC;
            border-radius: 4px 0 0 4px;
            color: white;
            padding: 4px;
            font-size: 12px;
            text-align: center;
            left: -24px;
            top: 24px;
            cursor: pointer;
            > i {
                transition: all .3s ease;
                display: block;
                transform: rotate(180deg);
            }
        }
        .pipeline-params-aside-content {
            height: 100%;
            background: white;
            .param-type-desc {
                display: flex;
                grid-gap: 12px;
                justify-content: flex-end;
                margin: 24px 0;
            }
            .param-indicator {
                display: flex;
                align-items: center;
                grid-gap: 8px;
            }
            .visible-param-dot,
            .required-param-dot,
            .readonly-param-dot {
                display: inline-block;
                width: 10px;
                height: 10px;
                border-radius: 50%;
                &.visible-param-dot {
                    background: #2DCB9D;
                }
                &.required-param-dot {
                    background: #FF5656;
                }
                &.readonly-param-dot {
                    background: #C4C6CC;
                }
            }
            .pipeline-params-list {
                border: 1px solid #DCDEE5;
                > li {
                    border-bottom: 1px solid #DCDEE5;
                    padding: 10px 16px;
                    display: grid;
                    grid-gap: 24px;
                    grid-auto-flow: column;
                    grid-template-columns: 1fr auto;
                    align-items: center;
                    &:last-child {
                        border-bottom: none;
                    }
                    > p {
                        display: flex;
                        flex-direction: column;
                        grid-gap: 6px;
                        overflow: hidden;
                        > label {
                            margin-right: auto;
                            @include ellipsis();
                            max-width: 100%;
                            &.has-param-desc {
                                border-bottom: 1px dashed #979BA5;
                            }
                        }
                        > span {
                            color: #979BA5;
                            @include ellipsis();
                        }
                    }
                    > span {
                        display: flex;
                        grid-gap: 8px;
                    }
                }
            }
            .pipeline-reccomend-version-conf {
                display: flex;
                flex-direction: column;
                grid-gap: 16px;
                margin-top: 24px;
                > li {
                    display: flex;
                    flex-direction: column;
                    grid-gap: 6px;
                    font-size: 12px;
                    > label {
                        line-height: 20px;
                        > span {
                            color: #979BA5;
                        }
                    }
                }
            }
        }
    }
</style>
