<template>
    <div class="node-affirmance-wrapper">
        <h3>{{ $t('environment.nodeInfo.theApplyNodes') }}</h3>
        <div class="node-detail-container">
            <icon name="linux" size="180" style="fill:#3b3b3b" class="option-logo" />
            <div class="node-details">
                <div class="count-select-row">
                    <i class="devops-icon icon-minus" @click="handleCount('remove')"></i>
                    <input type="number" class="bk-form-input node-count-input"
                        name="nodeCount"
                        v-model="count"
                        :class="{ 'is-danger': false }"
                        :disabled="!availablcount"
                        @keyup="input($event)"
                    />
                    <i class="devops-icon icon-plus" @click="handleCount('add')"></i>
                    <p class="count-tips">{{ `${$t('environment.nodeInfo.alreadyApplied')}${devCloudVmQuta.devCloudVmUsedCount}${$t('environment.nodeInfo.set')}，${$t('environment.nodeInfo.remaining')}${availablcount}${$t('environment.nodeInfo.canApplyCount')}` }}</p>
                </div>
                <h3>{{ currentSelectedModel.moduleName }}</h3>
                <p class="info-title">{{ $t('environment.hardware') }}</p>
                <ul class="modelspecs-item">
                    <li v-for="(item, index) in currentSelectedModel.description" :key="index">- {{ item }}</li>
                </ul>
                <p class="info-title">{{ $t('environment.startMirror') }}</p>
                <div class="docker-content">
                    <p class="no-data" v-if="!selectedImage.repo">{{ $t('environment.emptyImage') }}<span @click="dockerSelectConf.show = true">{{ $t('environment.clickSelect') }}</span></p>
                    <div class="docker-main" v-else>
                        <icon name="mirrors" size="60" style="fill:#3b3b3b" class="mirror-logo" />
                        <div class="docker-detail">
                            <p class="docker-name">{{ getInfoName(selectedImage) }}</p>
                            <p class="docker-desc">{{ selectedImage.desc }}</p>
                            <bk-button theme="default" size="small" class="modify-image" @click="dockerSelectConf.show = true">{{ $t('environment.modify') }}</bk-button>
                        </div>
                    </div>
                </div>
                <p class="info-title">{{ $t('environment.area') }}</p>
                <ul class="model-area-group">
                    <li class="item-tab"
                        v-for="(entry, index) in areaList"
                        :key="index"
                        :class="{
                            'hover': !entry.isDisabled && currentArea !== entry.value,
                            'active': currentArea === entry.value,
                            'disabled': entry.isDisabled
                        }"
                        @click="changeTab(entry)">
                        {{ entry.label }}
                    </li>
                </ul>
                <div class="footer">
                    <bk-button theme="primary" size="large" :disabled="!selectedImage.repo || !count" @click="apply">{{ $t('environment.apply') }}</bk-button>
                </div>
            </div>
        </div>
        <bk-sideslider
            class="create-atom-slider"
            :is-show.sync="dockerSelectConf.show"
            :title="dockerSelectConf.title"
            :quick-close="dockerSelectConf.quickClose"
            :width="dockerSelectConf.width">
            <template slot="content">
                <docker-list
                    :is-show="dockerSelectConf.show"
                    @updateCurImage="updateCurImage"
                    @cancelShow="dockerSelectConf.show = false"
                >
                </docker-list>
            </template>
        </bk-sideslider>
    </div>
</template>

<script>
    import { mapState } from 'vuex'
    import dockerList from './docker-list'

    export default {
        components: { dockerList },
        data () {
            return {
                count: 1,
                imageType: '',
                currentArea: 'SHENZHEN',
                areaList: [
                    { label: this.$t('environment.shenzhen'), value: 'SHENZHEN', isDisabled: false },
                    { label: this.$t('environment.chengdu'), value: 'CHENGDU', isDisabled: true },
                    { label: this.$t('environment.shanghai'), value: 'SHANGHAI', isDisabled: true },
                    { label: this.$t('environment.tianjing'), value: 'TIANJING', isDisabled: true }
                ],
                selectedImage: {},
                dockerSelectConf: {
                    show: false,
                    isLoading: false,
                    title: this.$t('environment.selectStartMirror'),
                    quickClose: true,
                    width: 600
                }
            }
        },
        computed: {
            ...mapState('environment', [
                'currentSelectedModel',
                'devCloudVmQuta'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            availablcount () {
                return this.devCloudVmQuta.devCloudVmQuota - this.devCloudVmQuta.devCloudVmUsedCount
            }
        },
        methods: {
            getInfoName (data) {
                let repoLab
                if (this.imageType === 'public') {
                    repoLab = /^(devcloud\/public\/).*$/.test(data.repo) ? data.repo.substr(16, data.repo.length) : data.repo
                } else {
                    repoLab = new RegExp('^(devcloud\\\/+' + this.projectId + '\\\/).*$').test(data.repo) ? data.repo.substr(10 + this.projectId.length, data.repo.length) : data.repo
                }
                return `${repoLab}:${data.tag}`
            },
            input () {
                if (!/^\d+$/.test(this.count)) {
                    this.count = ''
                }
            },
            handleCount (type) {
                if (type === 'add') {
                    if (this.count < this.availablcount) {
                        this.count++
                    }
                } else {
                    if (this.count > 1) {
                        this.count--
                    }
                }
            },
            changeTab (val) {
                if (!val.isDisabled && this.currentArea !== val.value) {
                    this.currentArea = val.value
                }
            },
            updateCurImage (image, type) {
                this.selectedImage = image
                this.imageType = type
                this.dockerSelectConf.show = false
            },
            async applyDocker () {
                let message, theme
                try {
                    const params = {
                        imageId: `${this.selectedImage.repo}:${this.selectedImage.tag}`,
                        modelId: this.currentSelectedModel.moduleId,
                        instanceCount: this.count,
                        zone: 'SHENZHEN',
                        validity: 0
                    }
                    await this.$store.dispatch('environment/applyDocker', {
                        projectId: this.projectId,
                        params
                    })
                    this.$router.push({ name: 'nodeList' })
                    this.$store.commit('environment/modifyProcessHead', {
                        process: 'modelType',
                        current: 0
                    })
                    message = this.$t('environment.successfullySubmited')
                    theme = 'success'
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'
                } finally {
                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            apply () {
                if (this.count > this.availablcount) {
                    this.$bkMessage({
                        message: `${this.$t('environment.devcloudCountLimit')}${this.availablcount}`,
                        theme: 'error'
                    })
                } else {
                    this.applyDocker()
                }
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';
    .node-affirmance-wrapper {
        padding: 30px 0 20px 20px;
        .node-detail-container {
            display: flex;
            margin-top: 40px;
            .node-logo {
                width: 100px;
                height: 100px;
            }
            .count-select-row {
                position: absolute;
                right: 0;
                user-select: none;
                text-align: right;
                input {
                    margin: 0 4px;
                    width: 50px;
                    text-align: center;
                }
                .devops-icon {
                    font-size: 12px;
                    cursor: pointer;
                }
                .count-tips {
                    margin-top: 4px;
                    color: $dangerColor;
                    font-size: 12px;
                }
            }
            .node-details {
                position: relative;
                margin-left: 50px;
            }
            .info-title {
                margin-top: 16px;
                margin-bottom: 8px;
                color: $fontBoldColor;
                font-weight: bold;
            }
            h3 {
                margin-bottom: 30px;
                font-size: 20px;
                color: $fontBoldColor;
            }
            .modelspecs-item > li {
                padding-left: 10px;
                font-size: 14px;
                line-height: 1.5;
            }
        }
        .docker-content {
            display: flex;
            justify-content: space-between;
            margin-top: 14px;
            padding: 0 14px;
            width: 600px;
            height: 114px;
            line-height: 48px;
            border-radius: 2px;
            background-color: #FFF;
            box-shadow: 0 3px 8px 0 rgba(0,0,0,0.2), 0 0 0 1px rgba(0,0,0,0.08);
            .no-data {
                width: 100%;
                margin-top: 32px;
                text-align: center;
                > span {
                    color: $primaryColor;
                    cursor: pointer;
                }
            }
            .docker-logo {
                margin: 20px 20px 0 14px;
                width: 70px;
                height: 70px;
            }
            .docker-main {
                display: flex;
                width: 100%;
            }
            .mirror-logo {
                margin-top: 27px;
                margin-right: 14px;
            }
            .docker-detail {
                position: relative;
                width: calc(100% - 100px);
                p {
                    margin-top: 24px;
                    line-height: 16px;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    font-size: 16px;
                }
                .docker-name {
                    color: $fontBoldColor;
                }
                .docker-desc {
                    font-size: 12px;
                }
                .modify-image {
                    position: absolute;
                    bottom: 12px;
                    right: 0;
                }
            }
        }
        .model-area-group {
            margin-top: 10px;
            display: flex;
            .item-tab {
                margin-right: -1px;
                padding: 8px 30px;
                line-height: 1.5;
                border: 1px solid #DDE4EB;
                color: #333C48;
                text-align: center;
                cursor: pointer;
            }
            .model-logo {
                position: relative;
                top: 4px;
            }
            .hover:hover {
                background-color: #FFF;
                color: $primaryColor;
                z-index: 1;
                border: 1px solid $primaryColor;
            }
            .disabled {
                color: #ccc;
                border-color: #e6e6e6;
                background-color: #fafafa;
                cursor: not-allowed;
            }
            .active {
                background-color: $primaryColor;
                color: #fff;
                z-index: 1;
                border: 1px solid $primaryColor;
            }
        }
        .footer {
            border-top: 1px solid $borderWeightColor;
            margin-top: 50px;
            text-align: right;
            padding-top: 16px;
            .bk-button {
                padding: 0 50px;
                font-size: 16px;
            }
        }
        .bk-sideslider-content {
            height: calc(100% - 60px);
        }
    }
</style>
