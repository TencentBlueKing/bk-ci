import * as artifactory from './artifactory'
import { mergeModules } from '@/utils/util'
import * as depot from './depot'
import Vue from 'vue'

const vue = new Vue()

const commonModules = {
    namespaced: true,
    state: {
        project: {}
    },
    mutations: {},
    actions: {
        requestProjectDetail: async ({ commit, state }, { projectCode }) => {
            return vue.$ajax.get(`/project/api/user/projects/${projectCode}/`).then(response => {
                if (response.data) {
                    state.project = response
                }
            })
        }
    },
    getters: {
        getProjectId: state => () => {
            let projectId = ''
            if (state.project && state.project.projectId) {
                projectId = state.project.projectId
            }
            return projectId
        }
    }
}

const tmpModules = mergeModules(commonModules, artifactory)

export default mergeModules(tmpModules, depot)
