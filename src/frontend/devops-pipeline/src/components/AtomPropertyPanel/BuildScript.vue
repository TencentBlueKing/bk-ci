<template>
    <section>
        <div class="bk-form bk-form-vertical">
            <template v-for="(obj, key) in atomPropsModel">
                <form-field v-if="!isHidden(obj, element)" :key="key" :desc="obj.desc" :desc-link="obj.descLink" :desc-link-text="obj.descLinkText" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                    <component :is="obj.component" :disabled="disabled" v-validate.initial="Object.assign(obj.rule, { required: !!obj.required })" :lang="lang" :name="key" :handle-change="handleUpdateElement" :value="element[key]" v-bind="obj"></component>
                </form-field>
            </template>
        </div>
    </section>
</template>

<script>
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    import { mapActions, mapGetters, mapState } from 'vuex'
    export default {
        name: 'build-script',
        mixins: [atomMixin, validMixins],
        computed: {
            ...mapGetters('atom', [
                'checkPipelineInvalid',
                'getEditingElementPos'
            ]),
            ...mapState('atom', [
                'pipeline'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            langList () {
                return this.atomPropsModel.scriptType.list
            },
            lang () {
                const lang = this.langList.find(stype => stype.value === this.element.scriptType)
                return lang ? lang.id : ''
            }
        },
        created () {
            if (this.atomPropsModel.archiveFile !== undefined) {
                this.atomPropsModel.archiveFile.hidden = !this.element.enableArchiveFile
            }
        },
        methods: {
            ...mapActions('atom', [
                'setPipeline'
            ])
        }
    }
</script>

<style lang="scss" scoped>
    .dialog-regist {
        position: relative;
        .regist-content {
            padding: 45px 65px 15px 65px;
        }
        .regist-footer {
            text-align: center;
            padding: 20px 65px 40px;
            font-size: 0;
            .bk-button {
                width: 110px;
                height: 36px;
                font-size: 14px;
                border: 1px solid #c3cdd7;
                border-radius: 2px;
                box-shadow: none;
                outline: none;
                background-color: #fff;
                text-overflow: ellipsis;
                overflow: hidden;
                white-space: nowrap;
                cursor: pointer;
                &.bk-primary {
                    margin-right: 20px;
                    color: #fff;
                    background-color: #3c96ff;
                    border-color: #3c96ff;
                }
            }
        }
    }
</style>
