<template>
    <bk-container flex :col="12">
        <bk-row>
            <bk-col :span="9">
                <bk-row>
                    <bk-col :span="4.5" class="g-progress-item">
                        <span class="g-progress-label">{{ $t('store.名称') }} :</span>
                        <span class="g-progress-content">{{detail.imageName}}</span>
                    </bk-col>
                    <bk-col :span="4.5" class="g-progress-item">
                        <span class="g-progress-label">{{ $t('store.标识') }} :</span>
                        <span class="g-progress-content">{{detail.imageCode}}</span>
                    </bk-col>
                </bk-row>
                <bk-row>
                    <bk-col :span="4.5" class="g-progress-item">
                        <span class="g-progress-label">{{ $t('store.范畴') }} :</span>
                        <span class="g-progress-content">{{detail.categoryName}}</span>
                    </bk-col>
                    
                    <bk-col :span="4.5" class="g-progress-item">
                        <span class="g-progress-label">{{ $t('store.分类') }} :</span>
                        <span class="g-progress-content">{{detail.classifyName}}</span>
                    </bk-col>
                </bk-row>
                <bk-row>
                    <bk-col :span="9" class="g-progress-item">
                        <span class="g-progress-label">{{ $t('store.功能标签') }} :</span>
                        <section class="g-progress-content label-list">
                            <span class="label-card" v-for="(label, index) in detail.labelList" :key="index">{{ label.labelName }}</span>
                        </section>
                    </bk-col>
                </bk-row>
            </bk-col>
            <bk-col :span="3">
                <img v-if="detail.logoUrl" :src="detail.logoUrl" class="g-progress-image">
            </bk-col>
        </bk-row>
        <bk-row>
            <bk-col :span="12" class="g-progress-item">
                <span class="g-progress-label">{{ $t('store.适用机器') }} :</span>
                <section class="g-progress-content label-list">
                    <span class="label-card" v-for="(agent, index) in filterAgents" :key="index">{{ agent }}</span>
                </section>
            </bk-col>
        </bk-row>
        <bk-row>
            <bk-col :span="12" class="g-progress-item">
                <span class="g-progress-label">{{ $t('store.简介') }} :</span>
                <span class="g-progress-content">{{detail.summary}}</span>
            </bk-col>
        </bk-row>
        <bk-row>
            <bk-col :span="12" class="g-progress-item">
                <span class="g-progress-label">{{ $t('store.详细描述') }} :</span>
                <section class="g-progress-content">
                    <p :class="{ 'overflow': !isDropdownShow }" ref="edit">
                        <mavon-editor class="image-remark-input"
                            ref="mdHook"
                            v-model="detail.description"
                            :editable="false"
                            :toolbars-flag="false"
                            default-open="preview"
                            :box-shadow="false"
                            :subfield="false"
                            :language="mavenLang"
                            preview-back-ground="#fafbfd"
                        />
                    </p>
                    <span class="toggle-btn" v-if="isOverflow" @click="isDropdownShow = !isDropdownShow">{{ isDropdownShow ? $t('store.收起') : $t('store.展开') }}
                        <i :class="['devops-icon icon-angle-down', { 'icon-flip': isDropdownShow }]"></i>
                    </span>
                </section>
            </bk-col>
        </bk-row>
        <bk-row>
            <bk-col :span="12" class="g-progress-item">
                <span class="g-progress-label">{{ $t('store.镜像') }} :</span>
                <span class="g-progress-content">{{(detail.imageRepoUrl ? detail.imageRepoUrl + '/' : '') + detail.imageRepoName + ':' + detail.imageTag}}</span>
            </bk-col>
        </bk-row>
        <bk-row>
            <bk-col :span="12" class="g-progress-item">
                <span class="g-progress-label">{{ $t('store.镜像凭证') }} :</span>
                <span class="g-progress-content">{{detail.ticketId}}</span>
            </bk-col>
        </bk-row>
        <bk-row>
            <bk-col :span="12" class="g-progress-item">
                <span class="g-progress-label">{{ $t('store.发布者') }} :</span>
                <span class="g-progress-content">{{detail.publisher}}</span>
            </bk-col>
        </bk-row>
        <bk-row>
            <bk-col :span="12" class="g-progress-item">
                <span class="g-progress-label">{{ $t('store.发布类型') }} :</span>
                <span class="g-progress-content">{{detail.releaseType|releaseFilter}}</span>
            </bk-col>
        </bk-row>
        <bk-row>
            <bk-col :span="12" class="g-progress-item">
                <span class="g-progress-label">{{ $t('store.版本') }} :</span>
                <span class="g-progress-content">{{detail.version}}</span>
            </bk-col>
        </bk-row>
        <bk-row>
            <bk-col :span="12" class="g-progress-item">
                <span class="g-progress-label">{{ $t('store.发布描述') }} :</span>
                <span class="g-progress-content">
                    <mavon-editor class="image-remark-input"
                        ref="mdHook"
                        v-model="detail.versionContent"
                        :editable="false"
                        :toolbars-flag="false"
                        default-open="preview"
                        :box-shadow="false"
                        :subfield="false"
                        :language="mavenLang"
                        preview-back-ground="#fafbfd"
                    />
                </span>
            </bk-col>
        </bk-row>
    </bk-container>
</template>

<script>
    import { mapActions } from 'vuex'
    export default {
        filters: {
            releaseFilter (value) {
                const local = window.devops || {}
                let res = ''
                switch (value) {
                    case 'NEW':
                        res = local.$t('store.初始化')
                        break
                    case 'INCOMPATIBILITY_UPGRADE':
                        res = local.$t('store.非兼容升级')
                        break
                    case 'COMPATIBILITY_UPGRADE':
                        res = local.$t('store.兼容式功能更新')
                        break
                    case 'COMPATIBILITY_FIX':
                        res = local.$t('store.兼容式问题修正')
                        break
                }
                return res
            }
        },

        props: {
            detail: {
                type: Object,
                require: true
            }
        },

        data () {
            return {
                isOverflow: false,
                isDropdownShow: false,
                agentTypes: []
            }
        },
        computed: {
            filterAgents () {
                const AgentNames = []
                this.detail.agentTypeScope.forEach(item => {
                    this.agentTypes.forEach(agent => {
                        if (item === agent.code) {
                            AgentNames.push(agent.name)
                        }
                    })
                })
                return AgentNames
            },
            mavenLang () {
                return this.$i18n.locale === 'en-US' ? 'en' : this.$i18n.locale
            }
        },
        mounted () {
            setTimeout(() => {
                this.isOverflow = this.$refs.edit.scrollHeight > 180
            }, 1000)
            this.fetchAgentTypes().then(res => {
                this.agentTypes = res
            })
        },
        methods: {
            ...mapActions('store', [
                'fetchAgentTypes'
            ])
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';

    .toggle-btn {
        font-size: 12px;
        color: $primaryColor;
        text-align: right;
        cursor: pointer;
        .devops-icon {
            display: inline-block;
            margin-left: 2px;
            transition: all ease 0.2s;
            &.icon-flip {
                transform: rotate(180deg);
            }
        }
    }

    .g-progress-content {
        max-width: calc(100% - 100px);
    }

    .label-card {
        float: left;
        margin-bottom: 4px;
        margin-right: 4PX;
        padding: 2px 7px;
        font-size: 12px;
        border: 1px solid $borderWeightColor;
        background-color: #F0F1F3;
        color: $fontColor;
    }

    .overflow {
        max-height: 180px;
        overflow: hidden;
    }
</style>
