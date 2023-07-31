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
                变量
            </span>
            <div class="pipeline-params-aside-content">
                <div v-if="params.length > 0">
                    <h6>
                        {{ $t('流水线变量') }}
                        <i class="devops-icon icon-question-circle-shape"></i>
                    </h6>
                    <ul class="pipeline-params-list">
                        <li v-for="item in params" :key="item.id">
                            <label>
                                {{ item.id }}
                                <i
                                    v-if="item.desc"
                                    v-bk-tooltips="item.descTips"
                                    class="devops-icon icon-info-circle"
                                />
                            </label>
                            <span>
                                {{ $t(item.type) }}
                            </span>
                            <span>
                                <bk-tag theme="success">{{ $t('入参') }}</bk-tag>
                                <bk-tag v-if="item.required" theme="danger">{{ $t('必填') }}</bk-tag>
                                <bk-tag v-if="item.readOnly">{{ $t('只读') }}</bk-tag>
                            </span>
                        </li>
                    </ul>
                </div>
                <h6>
                    {{ $t('推荐版本号') }}
                    <i class="devops-icon icon-question-circle-shape"></i>
                </h6>
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
            </div>
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
            ...mapGetters('pipelines', [
                'curPipelineParams',
                'curPipelineBuildNoConfig'
            ]),
            ...mapGetters('atom', [
                'buildNoRules'
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
                    label: '版本号',
                    labelDesc: ' (主版本.特性版本.修正版本)',
                    component: 'BuildVersion',
                    props: {
                        disabled: true,
                        value: this.curPipelineBuildNoConfig?.semver
                    }
                }, {
                    label: '构建号',
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
        z-index: 9999;
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
            padding: 8px 24px;
            background: white;
            > h6 {
                display: flex;
                align-items: center;
                grid-gap: 8px;
                padding: 8px 0;
                margin: 0 0 16px 0;
                color: #313238;
                font-size: 14px;
                font-weight: 700;
                border-bottom: 1px solid #DCDEE5;
                > i {
                    color: #979BA5;
                }
            }
            .pipeline-params-list {
                border: 1px solid #DCDEE5;
                margin-bottom: 24px;
                > li {
                    border-bottom: 1px solid #DCDEE5;
                    height: 42px;
                    padding: 0 16px;
                    display: grid;
                    grid-gap: 16px;
                    grid-auto-flow: column;
                    grid-template-columns: 1fr auto;
                    align-items: center;
                    &:last-child {
                        border-bottom: none;
                    }
                }
            }
            .pipeline-reccomend-version-conf {
                display: flex;
                flex-direction: column;
                grid-gap: 16px;
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
