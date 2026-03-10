<template>
    <article>
        <section class="show-detail">
            <img
                :src="detail.logoUrl || defaultPic"
                class="detail-img"
            >
            <ul
                class="detail-items"
                ref="detail"
            >
                <li class="detail-item">
                    <span class="item-name">{{ detail.name }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.标识：') }}</span>
                    <span>{{ detail.atomCode || '--' }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.适用范畴：') }}</span>
                    <div
                        class="category-scope-container"
                        v-if="detail.serviceScopeDetails && detail.serviceScopeDetails.length > 0"
                    >
                        <div
                            class="category-scope-item"
                            v-for="scopeConfig in detail.serviceScopeDetails"
                            :key="scopeConfig.serviceScope"
                        >
                            <div class="scope-name">{{ scopeNameMap[scopeConfig.serviceScope] }}</div>
                            <div class="scope-info">
                                <div class="scope-info-item">
                                    <span class="scope-label">{{ $t('store.分类：') }}</span>
                                    <span>{{ scopeConfig.classifyName || '--' }}</span>
                                </div>
                                <div class="scope-info-item">
                                    <span class="scope-label">{{ $t('store.适用Job类型：') }}</span>
                                    <span>
                                        {{ getJobTypeNames(scopeConfig.jobTypes) }}
                                    </span>
                                </div>
                                <div class="scope-info-item">
                                    <span class="scope-label">{{ $t('store.功能标签：') }}</span>
                                    <label-list :label-list="getScopeLabelNames(scopeConfig)"></label-list>
                                </div>
                            </div>
                        </div>
                    </div>
                    <span v-else>--</span>
                </li>
                <li
                    class="detail-item"
                    v-if="!isEnterprise"
                >
                    <span class="detail-label">{{ $t('store.是否开源：') }}</span>
                    <span>{{ detail.visibilityLevel | levelFilter }}</span>
                </li>
                <li
                    class="detail-item"
                    v-if="isEnterprise"
                >
                    <span class="detail-label">{{ $t('store.发布包：') }}</span>
                    <span>{{ detail.pkgName || '--' }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.简介：') }}</span>
                    <span>{{ detail.summary || '--' }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.详细描述：') }}</span>
                    <mavon-editor
                        :editable="false"
                        default-open="preview"
                        :subfield="false"
                        :toolbars-flag="false"
                        :external-link="false"
                        :box-shadow="false"
                        preview-background="#fff"
                        :language="mavenLang"
                        v-model="detail.description"
                    />
                </li>
                <slot></slot>
            </ul>
        </section>
    </article>
</template>

<script>
    import defaultPic from '../../../../images/defaultPic.svg'
    import labelList from '../../../labelList'

    export default {
        filters: {
            levelFilter (val = 'LOGIN_PUBLIC') {
                const bkLocale = window.devops || {}
                if (val === 'LOGIN_PUBLIC') return bkLocale.$t('store.是')
                else return bkLocale.$t('store.否')
            }
        },

        components: {
            labelList
        },

        props: {
            detail: Object
        },

        data () {
            return {
                defaultPic,
                scopeNameMap: {
                    PIPELINE: this.$t('store.CI流水线'),
                    CREATIVE_STREAM: this.$t('store.CP创作流')
                },
                jobTypeMap: {
                    AGENT: this.$t('store.编译环境'),
                    AGENT_LESS: this.$t('store.无编译环境'),
                    CREATIVE_STREAM: this.$t('store.创作环境'),
                    CLOUD_TASK: this.$t('store.云任务环境')
                }
            }
        },
        computed: {
            isEnterprise () {
                return VERSION_TYPE === 'ee'
            },
            mavenLang () {
                return this.$i18n.locale === 'en-US' ? 'en' : this.$i18n.locale
            }
        },
        methods: {
            getJobTypeNames (jobTypes) {
                if (!jobTypes || jobTypes.length === 0) return '--'
                return jobTypes.map(type => this.jobTypeMap[type] || type).join('、')
            },
            getScopeLabelNames (scopeConfig) {
                if (!scopeConfig.labelList || scopeConfig.labelList.length === 0) return []
                return scopeConfig.labelList.map(item => item.labelName)
            }
        }
    }
</script>

<style lang="scss" scoped>
.category-scope-container {
    width: 100%;

    .category-scope-item {
        position: relative;
        padding: 30px 16px 12px;
        margin-bottom: 8px;
        background-color: #F5F7FA;
        border-radius: 2px;

        &:last-child {
            margin-bottom: 0;
        }

        .scope-name {
            position: absolute;
            left: 0;
            top: 0;
            margin-bottom: 8px;
            padding: 4px 8px;
            font-size: 12px;
            color: #1768EF;
            background-color: #E1ECFF;
            border-radius: 2px 0 8px 0;
        }

        .scope-info {
            .scope-info-item {
                display: flex;
                align-items: center;
                font-size: 12px;
                line-height: 20px;
                margin-bottom: 4px;

                &:last-child {
                    margin-bottom: 0;
                }

                .scope-label {
                    display: inline-block;
                    width: 110px;
                    text-align: right;
                    color: #979BA5;
                    margin-right: 4px;
                }

                .devops-icon {
                    font-size: 14px;
                    margin: 0 2px;
                }
            }
        }
    }
}
</style>
