import assert from 'node:assert/strict'
import test from 'node:test'
import { canCurrentUserReview, resolveManualReviewUsers } from '../../../../utils/manualReview.js'

test('uses actual reviewers, including variable-resolved users', () => {
    const atom = { actualReviewUsers: ['alice; bob'], reviewUsers: ['configured'] }
    assert.deepEqual(resolveManualReviewUsers(atom), ['alice', 'bob'])
    assert.equal(canCurrentUserReview(atom, 'bob'), true)
    assert.equal(canCurrentUserReview(atom, 'configured'), false)
})

test('an explicitly empty actual reviewer list does not fall back', () => {
    assert.deepEqual(resolveManualReviewUsers({ actualReviewUsers: [], reviewUsers: ['alice'] }), [])
    assert.equal(canCurrentUserReview({ actualReviewUsers: [], reviewUsers: ['alice'] }, 'alice'), false)
})

test('supports legacy reviewer fields and rejects malformed values safely', () => {
    assert.equal(canCurrentUserReview({ reviewUsers: ['alice; bob'] }, 'alice'), true)
    assert.equal(canCurrentUserReview({ data: { input: { reviewers: ['bob'] } } }, 'bob'), true)
    assert.deepEqual(resolveManualReviewUsers({ actualReviewUsers: [null, {}, 1] }), [])
    assert.equal(canCurrentUserReview({ actualReviewUsers: [null] }, null), false)
    assert.equal(canCurrentUserReview(null, 'alice'), false)
    assert.equal(canCurrentUserReview(undefined, 'alice'), false)
})
