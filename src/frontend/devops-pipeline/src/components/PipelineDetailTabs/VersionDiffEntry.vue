<template>
    <span>
        <bk-button
            text
            size="small"
            theme="primary"
            @click="diffVersion(version)"
        >
            <slot>
                {{ $t('diff') }}
            </slot>
        </bk-button>
        <bk-dialog
            render-directive="if"
            v-model="showVersionDiffDialog"
            header-position="left"
            width="90%"
            :title="$t('diff')"
        >
            <div v-bkloading="{ isLoading: isLoadYaml, color: '#1d1d1d' }">
                <header class="diff-version-header">
                    <span>
                        {{ releaseVersionName }}
                        <bk-tag theme="info">{{ $t('template.current') }}</bk-tag>
                    </span>
                    <span>
                        <bk-select
                            ext-cls="dark-theme-select-trigger"
                            ext-popover-cls="dark-theme-select-menu"
                            v-model="activeVersion"
                            @change="diffVersion"
                            enable-scroll-load
                            :scroll-loading="bottomLoadingOptions"
                            @scroll-end="loadMore()"
                        >
                            <bk-option
                                v-for="item in pipelineVersionList"
                                :key="item.version"
                                :id="item.version"
                                :name="item.versionName"
                            />
                        </bk-select>
                    </span>
                </header>
                <yaml-diff
                    :old-yaml="currentYaml"
                    :new-yaml="activeYaml"
                />
            </div>

            <footer slot="footer">
                <bk-button
                    @click="showVersionDiffDialog = false"
                >
                    {{ $t('close') }}
                </bk-button>
            </footer>
        </bk-dialog>
    </span>
</template>

<script>
    import { mapActions } from 'vuex'
    import YamlDiff from '@/components/YamlDiff'
    export default {
        components: {
            YamlDiff
        },
        props: {
            version: {
                type: Number,
                required: true
            },
            releaseVersion: {
                type: Number,
                required: true
            }
        },
        data () {
            return {
                isLoadYaml: false,
                showVersionDiffDialog: false,
                activeVersion: '',
                activeYaml: '',
                currentYaml: '',
                pipelineVersionList: [],
                page: 1,
                hasNext: true,
                bottomLoadingOptions: {
                    size: 'small',
                    isLoading: false
                }
            }
        },
        computed: {
            releaseVersionName () {
                return this.pipelineVersionList.find(item => item.version === this.releaseVersion)?.versionName ?? '--'
            }
        },
        methods: {
            ...mapActions('atom', [
                'fetchPipelineByVersion'
            ]),
            ...mapActions('pipelines', [
                'requestPipelineVersionList'
            ]),
            async loadMore (page) {
                try {
                    if (!this.hasNext) return
                    const { projectId, pipelineId } = this.$route.params
                    const pageSize = 20
                    this.bottomLoadingOptions.isLoading = true
                    const res = await this.requestPipelineVersionList({
                        projectId,
                        pipelineId,
                        page: page ?? this.page + 1,
                        pageSize
                    })
                    this.page = res.page
                    this.hasNext = res.count > res.page * pageSize
                    console.log(res.records)
                    if (res.records.length > 0) {
                        this.pipelineVersionList.push(...res.records)
                    }
                } catch (error) {
                    console.log(error)
                } finally {
                    this.bottomLoadingOptions.isLoading = false
                }
            },
            async diffVersion (version) {
                console.log(version)
                try {
                    this.isLoadYaml = true
                    this.showVersionDiffDialog = true
                    this.loadMore(this.page)
                    this.activeVersion = version

                    const [{ yaml }, current] = await Promise.all([
                        this.fetchPipelineByVersion({
                            projectId: this.$route.params.projectId,
                            pipelineId: this.$route.params.pipelineId,
                            version: this.activeVersion
                        }),
                        this.fetchPipelineByVersion({
                            projectId: this.$route.params.projectId,
                            pipelineId: this.$route.params.pipelineId,
                            version: this.releaseVersion
                        })
                    ])
                    this.activeYaml = yaml
                    this.currentYaml = current.yaml
                } catch (error) {
                    console.log(error)
                } finally {
                    this.isLoadYaml = false
                }
            }
        }
    }
</script>
