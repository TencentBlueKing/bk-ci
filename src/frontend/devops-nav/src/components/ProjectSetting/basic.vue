<template>
    <bk-form :model="projectForm">
        <devops-form-item
            :label="$t('projectName')"
            :required="true"
            :is-error="errors.has('projectName')"
        >
            <bk-input class="basic-setting-input"
                v-model="projectForm.projectName"
                v-validate="{ required: true, min: 1, max: 32, projectNameUnique: [projectForm.projectCode] }"
                maxlength="32"
                name="projectName"
                :placeholder="$t('projectNamePlaceholder')"
            />
            <div
                v-if="errors.has('projectName')"
                slot="error-tips"
                class="project-dialog-error-tips"
            >
                {{ errors.first('projectName') }}
            </div>
        </devops-form-item>
        <devops-form-item
            :label="$t('englishName')"
            :required="true"
            :rules="[]"
            property="englishName"
            :is-error="errors.has('englishName')"
            :error-msg="errors.first('englishName')"
        >
            <bk-input class="basic-setting-input"
                v-model="projectForm.englishName"
                v-validate="{ required: true, min: 2, max: 32 }"
                :placeholder="$t('projectEnglishNamePlaceholder')"
                name="englishName"
                maxlength="32"
                :disabled="true"
            />
        </devops-form-item>
        <devops-form-item
            :label="$t('projectDesc')"
            :required="true"
            property="description"
            :is-error="errors.has('description')"
        >
            <bk-input class="basic-setting-input"
                v-model="projectForm.description"
                v-validate="{ required: true }"
                type="textarea"
                maxlength="100"
                :placeholder="$t('projectDescPlaceholder')"
                name="description"
            />
        </devops-form-item>
        <devops-form-item>
            <bk-button theme="primary" @click="saveProject" :loading="isSaving">{{ $t("save") }}</bk-button>
        </devops-form-item>
    </bk-form>
</template>

<script>
    import { mapActions } from 'vuex'

    export default {
        data () {
            return {
                projectForm: {},
                isSaving: false
            }
        },

        computed: {
            projectCode () {
                return this.$route.params.projectId
            }
        },

        watch: {
            projectCode: {
                handler () {
                    const projectList = window.getLsCacheItem('projectList') || []
                    const curProject = projectList.find((project) => (project.projectCode === this.projectCode))
                    this.projectForm = JSON.parse(JSON.stringify(curProject))
                },
                immediate: true
            }
        },

        methods: {
            ...mapActions(['ajaxUpdatePM', 'getProjects']),

            async saveProject () {
                const valid = await this.$validator.validate()
                if (!valid) {
                    return valid
                }
                const params = {
                    projectCode: this.projectForm.projectCode,
                    data: this.projectForm
                }
                this.isSaving = true
                this.ajaxUpdatePM(params).then(() => {
                    this.$bkMessage({ theme: 'success', message: this.$t('updateProjectSuccuess') })
                    return this.getProjects()
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isSaving = false
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    .basic-setting-input {
        width: 50%;
    }
</style>
