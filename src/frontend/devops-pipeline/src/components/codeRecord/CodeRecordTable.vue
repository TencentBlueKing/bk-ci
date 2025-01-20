<template>
    <div
        class="code-record-table-container"
        v-if="commitList.length"
    >
        <p class="prompt-tips">
            {{ $t('details.commitIdsRange') }}
            <span v-if="commitList.length > 1">
                <span>{{ formatCommitId(lastCommit.commit) }}</span>
                <span>-</span>
                <span>{{ formatCommitId(firstCommit.commit) }}</span>
            </span>
            <span v-else>
                <span>{{ formatCommitId(firstCommit.commit) }}</span>
            </span>
            <span
                v-if="isGItRepo"
                class="ai-cr-button"
                @click="handleOpenAISummary"
            >
                <logo
                    name="aicr-colorful"
                    size="12"
                />
                <span class="ai-cr-text">{{ $t('details.aiSummary') }}</span>
            </span>
        </p>
        <bk-table :data="commitList">
            <!-- <bk-table-column :label="$t('history.remark')" prop="comment"></bk-table-column> -->
            <bk-table-column
                :label="$t('history.remark')"
                prop="comment"
            >
                <template slot-scope="props">
                    <a
                        class="commit-link"
                        :href="props.row.url"
                        target="_blank"
                        v-if="props.row.type === 2 && props.row.url"
                    >{{ props.row.comment }}
                    </a>
                    <span v-else>{{ props.row.comment }}</span>
                </template>
            </bk-table-column>
            <bk-table-column
                :width="150"
                label="Commit"
                prop="commit"
                :formater="row => formatCommitId(row.commit)"
            ></bk-table-column>
            <bk-table-column
                :width="150"
                :label="$t('details.committer')"
                prop="committer"
            ></bk-table-column>
            <bk-table-column
                :width="200"
                :label="$t('details.commitTime')"
                prop="commitTime"
                :formatter="formatTimeColumn"
            ></bk-table-column>
        </bk-table>

        <code-ai-summary
            :value.sync="showAiSummary"
            :element-id="id"
        />
    </div>
    <div
        v-else
        class="code-records-empty"
    >
        <div class="no-data-right">
            <logo
                name="empty"
                size="180"
            />
            <p>
                <span>{{ label }}</span>{{ $t('details.noCodeRecords') }}
            </p>
        </div>
    </div>
</template>

<script>
    import { convertTime } from '@/utils/util'
    import Logo from '@/components/Logo'
    import CodeAiSummary from '@/components/CodeAiSummary/'
    export default {
        name: 'code-record-table',
        components: {
            Logo,
            CodeAiSummary
        },
        props: {
            commitList: {
                type: Array,
                default: []
            },
            label: {
                type: String,
                default: ''
            },
            id: {
                type: String,
                default: ''
            },
            scmType: {
                type: String
            }
        },
        data () {
            return {
                showAiSummary: false
            }
        },
        computed: {
            hasCommitList () {
                return Array.isArray(this.commitList) && this.commitList.length > 0
            },
            firstCommit () {
                return this.hasCommitList ? this.commitList[0] : ''
            },
            lastCommit () {
                const len = this.commitList.length
                return this.hasCommitList && len > 0 ? this.commitList[len - 1] : ''
            },
            isGItRepo () {
                return this.scmType === 'CODE_GIT'
            }
        },
        methods: {
            formatTimeColumn (row) {
                return row.commitTime ? convertTime(row.commitTime) : '--'
            },
            formatCommitId (commitId) {
                return commitId && typeof commitId === 'string' ? commitId.slice(0, 8) : '--'
            },
            async handleOpenAISummary () {
                try {
                    const res = await this.$store.dispatch('common/checkOAuth', {
                        redirectUrlType: 'SPEC',
                        redirectUrl: window.location.href
                    })
                    if (res.status === 403) {
                        window.top.location.href = res.url
                        return
                    }
                    this.showAiSummary = true
                } catch (e) {
                    this.$showTips({
                        message: e.message ? e.message : e,
                        theme: 'error'
                    })
                }
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';
    .code-record-table-container {
        overflow: auto;
        height: 100%;
        .prompt-tips {
            margin-bottom: 12px;
            height: 32px;
            line-height: 32px;
        }
        .commit-link {
            color: $primaryColor;
        }
    }
    .ai-cr-button {
        display: inline-flex;
        align-items: center;
        background: linear-gradient(#fff, #fff) padding-box, linear-gradient(90deg, #21e3c0, #3144ec) border-box;
        border: 1px solid transparent;
        padding: 5px;
        line-height: 18px;
        font-size: 12px;
        border-radius: 2px;
        margin-left: 10px;
        cursor: pointer;
        span {
            margin-left: 5px;
        }
    }
    .ai-cr-text {
        background-image: linear-gradient(90deg, #21e3c0, #3144ec);
        background-size: 100%;
        background-repeat: repeat;
        background-clip: text;
        -webkit-text-fill-color: transparent;
    }
</style>
