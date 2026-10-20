import { mapState, mapGetters } from 'vuex'

export default {
    computed: {
        ...mapState('atom', [
            'pipelineInfo',
            'activePipelineVersion'
        ]),
        ...mapGetters('atom', [
            'isBranchVersion'
        ]),
        yamlBranchValue () {
            return (this.element?.yamlBranch || '').trim()
        },
        showYamlBranch () {
            return this.yamlBranchValue !== '' || (this.isBranchVersion && !this.disabled)
        },
        yamlBranchInactive () {
            const value = this.yamlBranchValue
            if (!value) {
                return false
            }
            if (/[*?]/.test(value)) {
                return true
            }
            if (!this.isBranchVersion) {
                return true
            }
            const currentBranch = this.activePipelineVersion?.versionName || ''
            const defaultBranch = this.pipelineInfo?.yamlInfo?.defaultBranch || ''
            return !currentBranch || currentBranch === defaultBranch || value !== currentBranch
        }
    }
}
