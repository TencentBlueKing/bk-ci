import { ref } from 'vue'

export const templateCountRefreshVersion = ref(0)

export function refreshTemplateCount () {
    templateCountRefreshVersion.value += 1
}
