<template>
    <bk-container flex :col="12">
        <bk-row>
            <bk-col :span="9">
                <bk-row>
                    <bk-col :span="4.5" class="progress-item">
                        <span class="progress-label">{{ $t('名称') }} :</span>
                        <span class="progress-content">{{detail.imageName}}</span>
                    </bk-col>
                    <bk-col :span="4.5" class="progress-item">
                        <span class="progress-label">{{ $t('标识') }} :</span>
                        <span class="progress-content">{{detail.imageCode}}</span>
                    </bk-col>
                </bk-row>
                <bk-row>
                    <bk-col :span="4.5" class="progress-item">
                        <span class="progress-label">{{ $t('范畴') }} :</span>
                        <span class="progress-content">{{detail.categoryName}}</span>
                    </bk-col>
                    
                    <bk-col :span="4.5" class="progress-item">
                        <span class="progress-label">{{ $t('分类') }} :</span>
                        <span class="progress-content">{{detail.classifyName}}</span>
                    </bk-col>
                </bk-row>
                <bk-row>
                    <bk-col :span="9" class="progress-item">
                        <span class="progress-label">{{ $t('功能标签') }} :</span>
                        <section class="progress-content label-list">
                            <span class="label-card" v-for="(label, index) in detail.labelList" :key="index">{{ label.labelName }}</span>
                        </section>
                    </bk-col>
                </bk-row>
            </bk-col>
            <bk-col :span="3">
                <img :src="detail.logoUrl" class="progress-image">
            </bk-col>
        </bk-row>
        <bk-row>
            <bk-col :span="12" class="progress-item">
                <span class="progress-label">{{ $t('适用机器') }} :</span>
                <section class="progress-content label-list">
                    <span class="label-card" v-for="(agent, index) in detail.agentTypeScope" :key="index">{{ agent | agentFilter }}</span>
                </section>
            </bk-col>
        </bk-row>
        <bk-row>
            <bk-col :span="12" class="progress-item">
                <span class="progress-label">{{ $t('简介') }} :</span>
                <span class="progress-content">{{detail.summary}}</span>
            </bk-col>
        </bk-row>
        <bk-row>
            <bk-col :span="12" class="progress-item">
                <span class="progress-label">{{ $t('详细描述') }} :</span>
                <section class="progress-content">
                    <p :class="{ 'overflow': !isDropdownShow }" ref="edit">
                        <mavon-editor class="image-remark-input"
                            ref="mdHook"
                            v-model="detail.description"
                            :editable="false"
                            :toolbars-flag="false"
                            default-open="preview"
                            :box-shadow="false"
                            :subfield="false"
                            preview-back-ground="#fafbfd"
                        />
                    </p>
                    <span class="toggle-btn" v-if="isOverflow" @click="isDropdownShow = !isDropdownShow">{{ isDropdownShow ? $t('收起') : $t('展开') }}
                        <i :class="['bk-icon icon-angle-down', { 'icon-flip': isDropdownShow }]"></i>
                    </span>
                </section>
            </bk-col>
        </bk-row>
        <bk-row>
            <bk-col :span="12" class="progress-item">
                <span class="progress-label">{{ $t('镜像') }} :</span>
                <span class="progress-content">{{(detail.imageRepoUrl ? detail.imageRepoUrl + '/' : '') + detail.imageRepoName + ':' + detail.imageTag}}</span>
            </bk-col>
        </bk-row>
        <bk-row>
            <bk-col :span="12" class="progress-item">
                <span class="progress-label">{{ $t('镜像凭证') }} :</span>
                <span class="progress-content">{{detail.ticketId}}</span>
            </bk-col>
        </bk-row>
        <bk-row>
            <bk-col :span="12" class="progress-item">
                <span class="progress-label">{{ $t('发布者') }} :</span>
                <span class="progress-content">{{detail.publisher}}</span>
            </bk-col>
        </bk-row>
        <bk-row>
            <bk-col :span="12" class="progress-item">
                <span class="progress-label">{{ $t('发布类型') }} :</span>
                <span class="progress-content">{{detail.releaseType|releaseFilter}}</span>
            </bk-col>
        </bk-row>
        <bk-row>
            <bk-col :span="12" class="progress-item">
                <span class="progress-label">{{ $t('版本') }} :</span>
                <span class="progress-content">{{detail.version}}</span>
            </bk-col>
        </bk-row>
        <bk-row>
            <bk-col :span="12" class="progress-item">
                <span class="progress-label">{{ $t('发布描述') }} :</span>
                <span class="progress-content">{{detail.versionContent}}</span>
            </bk-col>
        </bk-row>
    </bk-container>
</template>

<script>
    export default {
        filters: {
            agentFilter (value) {
                const local = window.devops || {}
                let res = ''
                switch (value) {
                    case 'DOCKER':
                        res = local.$t('Devnet 物理机')
                        break
                    case 'IDC':
                        res = 'IDC CVM'
                        break
                    case 'PUBLIC_DEVCLOUD':
                        res = 'DevCloud'
                        break
                }
                return res
            },

            releaseFilter (value) {
                const local = window.devops || {}
                let res = ''
                switch (value) {
                    case 'NEW':
                        res = local.$t('初始化')
                        break
                    case 'INCOMPATIBILITY_UPGRADE':
                        res = local.$t('非兼容升级')
                        break
                    case 'COMPATIBILITY_UPGRADE':
                        res = local.$t('兼容式功能更新')
                        break
                    case 'COMPATIBILITY_FIX':
                        res = local.$t('兼容式问题修正')
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
                isDropdownShow: false
            }
        },

        mounted () {
            this.$nextTick(() => (this.isOverflow = this.$refs.edit.scrollHeight > 180))
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
        .bk-icon {
            display: inline-block;
            margin-left: 2px;
            transition: all ease 0.2s;
            &.icon-flip {
                transform: rotate(180deg);
            }
        }
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

    .progress-item {
        display: flex;
        align-items: flex-start;
        margin: 10px 0;
        font-size: 14px;
        line-height: 19px;
        .progress-label {
            display: inline-block;
            width: 100px;
            margin-right: 14px;
            color: $fontWeightColor;
            text-align: right;
        }
        .progress-content {
            flex: 1;
            color: $fontBlack;
            word-break: break-all;
            /deep/ .v-note-panel {
                border: none;
                .v-show-content {
                    padding: 0 !important;
                    background: $bgHoverColor !important;
                    p {
                        margin: 0;
                    }
                }
            }
        }
    }
    .progress-image {
        float: right;
        width: 100px;
        height: 100px;
    }
</style>
