import { mapState, mapGetters } from 'vuex'
import { VERSION_STATUS_ENUM } from '@/utils/pipelineConst'

/**
 * 获取当前查看/编辑的 YAML 配置所在分支名（仅分支版本场景有值）
 */
export function getCurrentYamlBranch ({ isBranchVersion, activePipelineVersion, pipelineInfo }) {
    // 历史/预览页：正在查看分支版本
    if (isBranchVersion) {
        return activePipelineVersion?.versionName || ''
    }
    // 编辑页：正在编辑基于分支版本创建的草稿
    if (pipelineInfo?.baseVersionStatus === VERSION_STATUS_ENUM.BRANCH) {
        return pipelineInfo?.baseVersionName || ''
    }
    return ''
}

/**
 * 判断 yaml-branch 是否未生效（与当前配置所在分支不一致、通配、写在默认分支上等）
 */
export function isYamlBranchInactive ({ value, currentBranch, defaultBranch }) {
    if (!value) {
        return false
    }
    if (/[*?]/.test(value)) {
        return true
    }
    return !currentBranch || currentBranch === defaultBranch || value !== currentBranch
}

export default {
    computed: {
        ...mapState('atom', [
            'pipelineInfo',
            'activePipelineVersion'
        ]),
        ...mapGetters('atom', [
            'isBranchVersion',
            'isTemplate'
        ]),
        yamlBranchValue () {
            return (this.element?.yamlBranch || '').trim()
        },
        // 当前查看/编辑的 YAML 配置所在分支名（仅分支版本场景有值）
        currentYamlBranch () {
            return getCurrentYamlBranch({
                isBranchVersion: this.isBranchVersion,
                activePipelineVersion: this.activePipelineVersion,
                pipelineInfo: this.pipelineInfo
            })
        },
        showYamlBranch () {
            // 非分支版本上下文（默认分支/正式版本）一律不展示「配置分支」，只展示「分支」
            if (this.currentYamlBranch === '') {
                return false
            }
            // 分支版本：编辑（面板可编辑）时常展示，没填为空输入；查看时仅已配置才展示（只读）
            return this.yamlBranchValue !== '' || !this.disabled
        },
        yamlBranchInactive () {
            return isYamlBranchInactive({
                value: this.yamlBranchValue,
                currentBranch: this.currentYamlBranch,
                defaultBranch: this.pipelineInfo?.yamlInfo?.defaultBranch || ''
            })
        }
    },
    watch: {
        currentYamlBranch: {
            immediate: true,
            handler (val) {
                // 编辑非分支版本（默认分支/正式版本草稿）时不保留 yaml-branch，保存按「分支」写入
                if (this.isTemplate || this.pipelineInfo == null) return
                if (!val && !this.$props.disabled && this.yamlBranchValue !== '') {
                    this.handleUpdateElement('yamlBranch', '')
                }
            }
        }
    }
}
