export function resolveReviewUsers (atom) {
    const candidates = [
        atom?.actualReviewUsers,
        atom?.reviewUsers,
        atom?.data?.input?.reviewers
    ]

    for (const candidate of candidates) {
        if (!Array.isArray(candidate)) continue
        return candidate.reduce((all, user) => {
            if (typeof user === 'string') {
                all.push(...user.split(';').map(value => value.trim()).filter(Boolean))
            }
            return all
        }, [])
    }

    return []
}
