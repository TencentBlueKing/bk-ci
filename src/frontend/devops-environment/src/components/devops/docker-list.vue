<template>
    <div class="docker-list-content">
        <bk-tab size="small" :active="currentTab"
            type="unborder-card"
            @tab-change="tabChanged">
            <bk-tab-panel
                v-for="(panel, index) in tabList"
                v-bind="panel"
                :key="index">
            </bk-tab-panel>
        </bk-tab>
        <div class="docker-radio-list">
            <bk-radio-group v-model="docker" @change="handleChange">
                <bk-radio v-for="(entry, index) in renderList" :key="index" :value="`${entry.repo}-${entry.tag}`">
                    <span class="bk-radio-text">{{ getInfoName(entry) }}</span>
                    <p class="docker-detail">
                        <span>{{ entry.createdBy }}</span>&nbsp;
                        <span>{{ entry.modified }}</span>&nbsp;
                        <span :title="entry.desc">{{ entry.desc }}</span>
                    </p>
                </bk-radio>
            </bk-radio-group>
        </div>
        <div class="footer-handle">
            <bk-button theme="primary" :disabled="!docker" @click="save">{{ $t('environment.save') }}</bk-button>
            <bk-button theme="defalut" @click="cancelShow">{{ $t('environment.cancel') }}</bk-button>
        </div>
    </div>
</template>

<script>
    import { mapState } from 'vuex'

    export default {
        props: {
            isShow: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                isInit: false,
                currentTab: 'public',
                tabList: [
                    { name: 'public', label: this.$t('environment.publicImage') },
                    { name: 'customize', label: this.$t('environment.customizeImage') }
                ],
                docker: '',
                dockerType: '',
                targetDocker: {}
            }
        },
        computed: {
            ...mapState('environment', [
                'publicDockerList',
                'customizeDockerList'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            renderList () {
                return this.currentTab === 'public' ? this.publicDockerList : this.customizeDockerList
            }
        },
        watch: {
            isShow (newVal, oldVal) {
                if (newVal) {
                    this.isInit = true
                    this.requestDockers()
                }
            }
        },
        methods: {
            getInfoName (data) {
                let repoLab
                if (this.currentTab === 'public') {
                    repoLab = /^(devcloud\/public\/).*$/.test(data.repo) ? data.repo.substr(16, data.repo.length) : data.repo
                } else {
                    repoLab = new RegExp('^(devcloud\\\/+' + this.projectId + '\\\/).*$').test(data.repo) ? data.repo.substr(10 + this.projectId.length, data.repo.length) : data.repo
                }
                return `${repoLab}:${data.tag}`
            },
            tabChanged (tab) {
                this.currentTab = tab
                if (this.isInit) {
                    this.requestDockers()
                    this.isInit = false
                }
            },
            handleChange (image) {
                this.targetDocker = this.renderList.filter(item => `${item.repo}-${item.tag}` === image)[0]
                this.dockerType = this.currentTab
            },
            async requestDockers () {
                try {
                    const res = await this.$store.dispatch('environment/requestPublicDockers', {
                        projectId: this.projectId,
                        isPublic: this.currentTab === 'public'
                    })
                    if (this.currentTab === 'public') {
                        this.$store.commit('environment/updatePublicImages', { list: res })
                    } else {
                        this.$store.commit('environment/updateCustomizeImages', { list: res })
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            cancelShow () {
                this.$emit('cancelShow')
            },
            save () {
                this.$emit('updateCurImage', this.targetDocker, this.dockerType)
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';
    .docker-list-content {
        padding: 10px 20px;
        height: calc(100% - 60px);
        .bk-tab-section {
            display: none;
        }
        .docker-radio-list {
            margin-top: 20px;
            padding: 10px 20px 20px;
            height: calc(100% - 40px);
            overflow: auto;
            border: 1px solid $borderColor;
            .bk-form-radio {
                display: inherit;
                max-width: 100%;
                margin-right: 20px;
                margin-bottom: 18px;
            }
            .bk-radio-text {
                position: relative;
                top: 6px;
                left: 2px;
                width: 95%;
                font-size: 16px;
                font-weight: bold;
            }
            .docker-detail {
                position: relative;
                top: 6px;
                margin-top: 6px;
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
                font-weight: normal;
                font-size: 14px;
                cursor: pointer;
                &>span:last-child {
                    display: inline-block;
                }
            }
        }
        .footer-handle {
            position: absolute;
            bottom: 10px;
        }
    }
</style>
