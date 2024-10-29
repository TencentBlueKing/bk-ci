<template>
    <bk-sideslider
        :is-show.sync="isShow"
        :quick-close="true"
        width="640"
        :title="$t('codelib.触发器详情')"
    >
        <div slot="content">
            <div
                v-if="atom && Object.keys(atomPropsModel).length"
                :is="AtomComponent"
                :element="atom.taskParams"
                :atom-props-model="atomPropsModel"
                :disabled="true"
                class="atom-content"
            >
            </div>
        </div>
    </bk-sideslider>
</template>

<script>
    import {
        mapActions
    } from 'vuex'
    import NormalAtom from './AtomContent/NormalAtom'
    import CodeGitWebHookTrigger from './AtomContent/CodeGitWebHookTrigger'

    export default {
        components: {
            NormalAtom,
            CodeGitWebHookTrigger
        },
        props: {
            atom: {
                type: Object,
                default: () => {}
            }
        },
        data () {
            return {
                isShow: false,
                atomPropsModel: {}
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            atomCode () {
                return this.atom.atomCode
            },
            AtomComponent () {
                if (this.atomCode === 'codeGitWebHookTrigger') return CodeGitWebHookTrigger
                return NormalAtom
            }
        },
        watch: {
            atomCode (val) {
                this.atomPropsModel = {}
                this.fetchAtomModalData()
            }
        },
        methods: {
            ...mapActions('codelib', [
                'fetchAtomModal'
            ]),

            fetchAtomModalData () {
                this.fetchAtomModal({
                    projectCode: this.projectId,
                    atomCode: this.atomCode
                }).then(res => {
                    this.atomPropsModel = res.props.input || res.props
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    ::v-deep .bk-sideslider-content {
        padding: 20px;
        height: calc(100vh - 60px);
    }
</style>
