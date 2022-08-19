---
name: Enhancement Request
about: Suggest an enhancement to the bk-ci project
title: ''
labels: kind/enhancement
assignees: ''

---

- type: textarea
    id: feature
    attributes:
      label: What would you like to be added?
      description: |
        Feature requests are unlikely to make progress as issues. Please consider engaging with SIGs on slack and mailing lists, instead.
    validations:
      required: true

  - type: textarea
    id: rationale
    attributes:
      label: Why is this needed?
    validations:
      required: true
