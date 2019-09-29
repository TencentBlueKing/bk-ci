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
            return vue.$ajax.get(`/backend/api/projects/${projectCode}/`).then(response => {
                if (response.data) {
                    state.project = response
                }
            })
        }
    },
    getters: {
        getProjectId: state => () => {
            let projectId = ''
            if (state.project && state.project.project_id) {
                projectId = state.project.project_id
            }
            return projectId
        }
    }
}

const tmpModules = mergeModules(commonModules, artifactory)

export default mergeModules(tmpModules, depot)
