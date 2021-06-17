/**
 * @file mock util
 * @author blueking
 */

export function randomInt (n, m) {
    return Math.floor(Math.random() * (m - n + 1) + n)
}

/**
 * sleep 函数
 *
 * @param {number} ms 毫秒数
 */
export function sleep (ms) {
    return new Promise(resolve => setTimeout(resolve, ms))
}
