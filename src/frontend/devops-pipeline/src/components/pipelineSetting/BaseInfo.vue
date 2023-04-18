<template>
    <div v-if="pipelineSetting" class="bkdevops-base-info-setting-tab">
        <bk-form>
            <bk-form-item :label="$t('pipelineName')" :required="true">
                <vuex-input :placeholder="$t('pipelineNameInputTips')" name="pipelineName" :value="pipelineSetting.pipelineName" v-validate.initial="'required|max:40'" max-length="40" :handle-change="handleBaseInfoChange" />
            </bk-form-item>

            <bk-form-item :required="false" :label="$t('settings.label')" v-if="tagGroupList.length">
                <div class="tag-group-row">
                    <div class="group-col" v-for="(filter, index) in tagGroupList" :key="index">
                        <label class="group-title">{{filter.name}}</label>
                        <bk-select
                            :value="labelValues[index]"
                            @selected="handleLabelSelect(index, arguments)"
                            @clear="handleLabelSelect(index, [[]])"
                            multiple>
                            <bk-option v-for="item in filter.labels" :key="item.id" :id="item.id" :name="item.name"
                            ></bk-option>
                        </bk-select>
                    </div>
                </div>
            </bk-form-item>
            <bk-form-item :label="$t('desc')" :is-error="errors.has('desc')" :error-msg="errors.first('desc')">
                <vuex-textarea name="desc" :value="pipelineSetting.desc" :placeholder="$t('pipelineDescInputTips')" v-validate.initial="'max:100'" :handle-change="handleBaseInfoChange" />
            </bk-form-item>
            <bk-form-item :label="$t('settings.buildNumberFormat')" :is-error="errors.has('buildNumRule')" :error-msg="errors.first('buildNumRule')">
                <vuex-input style="max-width: 350px;" name="buildNumRule" :value="pipelineSetting.buildNumRule" :placeholder="$t('buildDescInputTips')" v-validate.initial="{ buildNumRule: true }" max-length="256" :handle-change="handleBaseInfoChange" />
                <span @click="handleGoDocumentInfo">
                    <logo size="16" class="build-num-rule-warn" name="feedback" v-bk-tooltips="$t('buildNumRuleWarn')" />
                </span>
                <p class="error-tips"
                    v-show="errors.has('buildNumRule')">
                    {{ $t('settings.validatebuildNum') }}
                </p>
            </bk-form-item>
            <bk-form-item class="item-badge" :label="$t('settings.badge')" v-if="routeName !== 'templateSetting'">
                <img class="image-url" :src="badgeImageUrl">
                <div v-for="copyUrl in urlList" :key="copyUrl.url">
                    <label>{{copyUrl.label}}</label>
                    <p class="badge-item">
                        <bk-input readonly :value="copyUrl.url" disabled />
                        <span class="devops-icon icon-clipboard copy-icon" :data-clipboard-text="copyUrl.url"></span>
                    </p>
                </div>
            </bk-form-item>
        </bk-form>
    </div>
</template>

<script>
    import Logo from '@/components/Logo'
    import VuexTextarea from '@/components/atomFormField/VuexTextarea/index.vue'
    import VuexInput from '@/components/atomFormField/VuexInput/index.vue'
    import { mapGetters } from 'vuex'
    import Clipboard from 'clipboard'

    export default {
        name: 'bkdevops-base-info-setting-tab',
        components: {
            Logo,
            VuexTextarea,
            VuexInput
        },
        props: {
            pipelineSetting: Object,
            handleBaseInfoChange: Function
        },
        computed: {
            ...mapGetters({
                tagGroupList: 'pipelines/getTagGroupList'
            }),
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            badgeImageUrl () {
                return `${BADGE_URL_PREFIX}/process/api/external/pipelines/projects/${this.projectId}/${this.pipelineId}/badge?X-DEVOPS-PROJECT-ID=${this.projectId}`
            },
            badgeMarkdownLink () {
                return `[![BK Pipelines Status](${BADGE_URL_PREFIX}/process/api/external/pipelines/projects/${this.projectId}/${this.pipelineId}/badge?X-DEVOPS-PROJECT-ID=${this.projectId})](${location.origin}/process/api-html/user/builds/projects/${this.projectId}/pipelines/${this.pipelineId}/latestFinished?X-DEVOPS-PROJECT-ID=${this.projectId})`
            },
            urlList () {
                return [{
                    label: this.$t('settings.imgUrl'),
                    url: this.badgeImageUrl
                }, {
                    label: this.$t('settings.mdLink'),
                    url: this.badgeMarkdownLink
                }]
            },
            labelValues () {
                const labels = this.pipelineSetting.labels || []
                return this.tagGroupList.map((tag) => {
                    const currentLables = tag.labels || []
                    const value = []
                    currentLables.forEach((label) => {
                        const index = labels.findIndex((item) => (item === label.id))
                        if (index > -1) value.push(label.id)
                    })
                    return value
                })
            }
        },
        created () {
            this.clipboard = new Clipboard('.copy-icon').on('success', e => {
                this.$showTips({
                    theme: 'success',
                    message: this.$t('settings.conCopySuc')
                })
            })
            this.requestGrouptLists()
        },
        beforeDestroy () {
            this.clipboard.destroy()
        },
        methods: {
            /** *
             * 获取标签及其分组
             */
            async requestGrouptLists () {
                try {
                    const res = await this.$store.dispatch('pipelines/requestTagList', {
                        projectId: this.projectId
                    })

                    this.$store.commit('pipelines/updateGroupLists', res)
                    // this.dataList = this.tagGroupList
                } catch (err) {
                    this.$showTips({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
            },
            handleLabelSelect (index, arg) {
                let labels = []
                this.labelValues.forEach((value, valueIndex) => {
                    if (valueIndex === index) labels = labels.concat(arg[0])
                    else labels = labels.concat(value)
                })
                this.handleBaseInfoChange('labels', labels)
            },
            handleGoDocumentInfo () {
                window.open(this.$pipelineDocs.ALIAS_BUILD_NO_DOC)
            }
        }
    }
</script>

<style lang="scss">
    .bkdevops-base-info-setting-tab {
        .form-group {
            margin-bottom: 12px;
            > label {
                margin-bottom: 5px;
                display: block;
            }
            > p {
                display: flex;
                align-items: center;
                > span {
                    margin-left: 12px;
                }
            }
        }
        .tag-group-row {
            display: flex;
            justify-content: space-around;
            .group-col {
                flex: 1;
                margin-right:8px;
                .group-title {
                    font-size: 12px;
                    line-height: 34px;
                }
                &:last-child {
                    margin-right: 0;
                }
            }
        }
        .item-badge {
            font-size: 12px;
            .image-url {
                margin: 7px 0
            }
            label {
                margin: 8px 0 0 0;
                display: block;
            }
            .badge-item {
                display: flex;
                align-items: center;
                .devops-icon {
                    font-size: 14px;
                    margin-left: 10px;
                }
            }
        }
        .build-num-rule-warn {
            cursor: pointer;
            position: relative;
            top: 5px;
            left: 5px;
        }
    }
</style>
