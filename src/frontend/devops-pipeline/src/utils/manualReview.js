export function resolveManualReviewUsers (atom = {}) {
    atom = atom || {}
    const source = atom.actualReviewUsers ?? atom.reviewUsers ?? atom.data?.input?.reviewers
    const values = Array.isArray(source) ? source : []
    return values
        .filter(value => typeof value === 'string')
        .flatMap(value => value.split(';'))
        .map(value => value.trim())
        .filter(Boolean)
}

export function canCurrentUserReview (atom, username) {
    return typeof username === 'string' && resolveManualReviewUsers(atom).includes(username)
}
