export function validateDockerLimits (cpus, memory) {
    const errors = {}
    if (cpus && (!/^[0-9]+(\.[0-9]+)?$/.test(cpus) || !Number.isFinite(Number(cpus)) || Number(cpus) <= 0)) {
        errors.cpus = 'CPU 上限须为大于 0 的数，支持小数'
    }
    if (memory && !/^[1-9][0-9]*[mg]$/.test(memory)) {
        errors.memory = '内存上限支持 m、g，例如 512m、4g'
    } else if (memory && memory.endsWith('m') && Number(memory.slice(0, -1)) < 6) {
        errors.memory = '内存上限不能低于 6m'
    }
    return errors
}
