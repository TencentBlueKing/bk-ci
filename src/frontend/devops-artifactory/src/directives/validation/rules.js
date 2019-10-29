/**
 *  输入不为空
 *  @param {Custom} val - 被检查的值
 */
function notEmpty (val) {
    console.log(val)
    if (!val.toString().length) return false

    return true
}

/**
 *  限制长度
 *  @param {Custom} val - 被检查的值
 *  @param {Number} limitation - 自定义限制长度
 */
function limit (val, limitation) {
    // 没有限制
    if (!limitation) return true

    // 根据传入的长度判断传入的值是否符合要求
    if (val.toString().length > ~~limitation) {
        return false
    } else {
        return true
    }
}

export default {
    notEmpty,
    limit
}
