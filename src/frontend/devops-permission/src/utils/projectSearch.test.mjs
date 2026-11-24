import test from 'node:test';
import assert from 'node:assert/strict';
import { filterProjectsByKeyword, splitHighlightSegments } from './projectSearch.js';

const sampleProjects = [
  { englishName: 'bk-ci', projectName: '蓝盾研发平台' },
  { englishName: 'devops-alpha', projectName: 'DevOps Alpha Service' },
  { englishName: 'alpha-beta', projectName: 'Alpha-alpha 项目' },
  { englishName: 'regex-proj', projectName: 'App(v1+2).*[prod]' },
  { englishName: 'html-proj', projectName: '<script>&DevOps</script>' },
];

test('filterProjectsByKeyword filters Chinese and mixed-case English keywords in original order', () => {
  assert.deepEqual(
    filterProjectsByKeyword(sampleProjects, '研发').map(item => item.englishName),
    ['bk-ci'],
  );

  assert.deepEqual(
    filterProjectsByKeyword(sampleProjects, '  aLpHa  ').map(item => item.englishName),
    ['devops-alpha', 'alpha-beta'],
  );
});

test('filterProjectsByKeyword returns full list for empty or whitespace-only keyword', () => {
  assert.deepEqual(filterProjectsByKeyword(sampleProjects, ''), sampleProjects);
  assert.deepEqual(filterProjectsByKeyword(sampleProjects, '   '), sampleProjects);
});

test('filterProjectsByKeyword matches regex and HTML special characters literally', () => {
  assert.deepEqual(
    filterProjectsByKeyword(sampleProjects, '(v1+2).*[prod]').map(item => item.englishName),
    ['regex-proj'],
  );

  assert.deepEqual(
    filterProjectsByKeyword(sampleProjects, '<script>&').map(item => item.englishName),
    ['html-proj'],
  );
});

test('filterProjectsByKeyword handles non-matching keywords and missing projectName safely', () => {
  assert.deepEqual(filterProjectsByKeyword(sampleProjects, 'not-found'), []);

  const malformedProjects = [
    { englishName: 'missing-name' },
    { englishName: 'empty-name', projectName: '' },
    { englishName: 'null-name', projectName: null },
    null,
    { englishName: 'valid', projectName: 'Valid Project' },
  ];
  assert.deepEqual(
    filterProjectsByKeyword(malformedProjects, 'valid').map(item => item.englishName),
    ['valid'],
  );
  assert.deepEqual(filterProjectsByKeyword(null, 'valid'), []);
});

test('splitHighlightSegments preserves original casing and highlights multiple occurrences', () => {
  assert.deepEqual(splitHighlightSegments('蓝盾研发平台', '研发'), [
    { text: '蓝盾', matched: false },
    { text: '研发', matched: true },
    { text: '平台', matched: false },
  ]);

  assert.deepEqual(splitHighlightSegments('Alpha-alpha 项目', '  ALPHA '), [
    { text: 'Alpha', matched: true },
    { text: '-', matched: false },
    { text: 'alpha', matched: true },
    { text: ' 项目', matched: false },
  ]);
});

test('splitHighlightSegments returns single unhighlighted segment for empty, whitespace, or unmatched keyword', () => {
  assert.deepEqual(splitHighlightSegments('DevOps Alpha Service', ''), [
    { text: 'DevOps Alpha Service', matched: false },
  ]);
  assert.deepEqual(splitHighlightSegments('DevOps Alpha Service', '   '), [
    { text: 'DevOps Alpha Service', matched: false },
  ]);
  assert.deepEqual(splitHighlightSegments('DevOps Alpha Service', 'missing'), [
    { text: 'DevOps Alpha Service', matched: false },
  ]);
});

test('splitHighlightSegments handles regex special chars, HTML chars, and missing text safely', () => {
  assert.deepEqual(splitHighlightSegments('App(v1+2).*[prod]', '(v1+2).*[prod]'), [
    { text: 'App', matched: false },
    { text: '(v1+2).*[prod]', matched: true },
  ]);

  assert.deepEqual(splitHighlightSegments('<script>&DevOps</script>', '<script>&'), [
    { text: '<script>&', matched: true },
    { text: 'DevOps</script>', matched: false },
  ]);

  assert.deepEqual(splitHighlightSegments('', 'abc'), []);
  assert.deepEqual(splitHighlightSegments(null, 'abc'), []);
  assert.deepEqual(splitHighlightSegments(undefined, ''), []);
});
