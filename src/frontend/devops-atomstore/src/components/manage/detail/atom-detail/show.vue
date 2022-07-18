<template>
    <article>
        <section class="show-detail">
            <img :src="detail.logoUrl || defaultPic" class="detail-img">
            <ul class="detail-items" ref="detail">
                <li class="detail-item">
                    <span class="item-name">{{ detail.name }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.标识') }}：</span>
                    <span>{{ detail.atomCode || '--' }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.范畴') }}：</span>
                    <span>{{ categoryMap[detail.category] || '--' }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.分类') }}：</span>
                    <span>{{ detail.classifyName || '--' }}</span>
                </li>
                <li class="detail-item" v-if="isEnterprise">
                    <span class="detail-label">{{ $t('store.适用机器类型') }}：</span>
                    <div v-if="detail.os">{{ jobTypeMap[detail.jobType] }}
                        <span v-if="detail.jobType === 'AGENT'">（
                            <i class="devops-icon icon-linux-view" v-if="detail.os.indexOf('LINUX') !== -1"></i>
                            <i class="devops-icon icon-windows" v-if="detail.os.indexOf('WINDOWS') !== -1"></i>
                            <i class="devops-icon icon-macos" v-if="detail.os.indexOf('MACOS') !== -1"></i>）
                        </span>
                    </div>
                </li>
                <li class="detail-item" v-else>
                    <span class="detail-label">{{ $t('store.适用Job类型') }}：</span>
                    <div v-if="detail.os">{{ jobTypeMap[detail.jobType] }}
                        <span v-if="detail.jobType === 'AGENT'">（
                            <i class="devops-icon icon-linux-view" v-if="detail.os.indexOf('LINUX') !== -1"></i>
                            <i class="devops-icon icon-windows" v-if="detail.os.indexOf('WINDOWS') !== -1"></i>
                            <i class="devops-icon icon-macos" v-if="detail.os.indexOf('MACOS') !== -1"></i>）
                        </span>
                    </div>
                </li>
                <li class="detail-item" v-if="!isEnterprise">
                    <span class="detail-label">{{ $t('store.是否开源') }}：</span>
                    <span>{{ detail.visibilityLevel | levelFilter }}</span>
                </li>
                <li class="detail-item" v-if="isEnterprise">
                    <span class="detail-label">{{ $t('store.发布包') }}：</span>
                    <span>{{ detail.pkgName || '--' }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.功能标签') }}：</span>
                    <label-list :label-list="detail.labelList.map(x => x.labelName)"></label-list>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.简介') }}：</span>
                    <span>{{ detail.summary || '--' }}</span>
                </li>
                <li class="detail-item">
                    <span class="detail-label">{{ $t('store.详细描述') }}：</span>
                    <mavon-editor
                        :editable="false"
                        default-open="preview"
                        :subfield="false"
                        :toolbars-flag="false"
                        :external-link="false"
                        :box-shadow="false"
                        preview-background="#fff"
                        v-model="detail.description"
                    />
                </li>
                <slot></slot>
            </ul>
        </section>
    </article>
</template>

<script>
    import labelList from '../../../labelList'
    import defaultPic from '../../../../images/defaultPic.svg'

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
                categoryMap: {
                    TASK: this.$t('store.流水线插件'),
                    TRIGGER: this.$t('store.流水线触发器')
                },
                jobTypeMap: {
                    AGENT: this.$t('store.编译环境'),
                    AGENT_LESS: this.$t('store.无编译环境')
                }
            }
        },

        computed: {
            isEnterprise () {
                return VERSION_TYPE === 'ee'
            }
        }
    }
</script>
