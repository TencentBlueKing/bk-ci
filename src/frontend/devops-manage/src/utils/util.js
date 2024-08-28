function prezero (num) {
    num = Number(num)

    if (num < 10) {
        return '0' + num
    }

    return num
}

export function convertTime (ms) {
    if (!ms) return '--'
    const time = new Date(ms)

    return `${time.getFullYear()}-${prezero(time.getMonth() + 1)}-${prezero(time.getDate())} ${prezero(time.getHours())}:${prezero(time.getMinutes())}:${prezero(time.getSeconds())}`
}

