---
name: Translation Quality Evaluation Criteria
description: 7-criteria framework for evaluating Korean to English translation quality
created: 2026-01-02
version: 1.0
---

# Translation Quality Evaluation Criteria

## Purpose

This document defines the 7-criteria framework for evaluating the quality of Korean to English translations for FxStore documentation. Each translated document must achieve **A+ grade on all 7 criteria** before being accepted.

---

## Grading Scale

- **A+**: Perfect - Meets all requirements with excellence
- **A**: Excellent - Meets all requirements with minor improvements possible
- **B+**: Good - Meets most requirements
- **B**: Satisfactory - Meets basic requirements but has noticeable issues
- **C**: Needs Improvement - Significant issues requiring revision
- **F**: Fail - Does not meet minimum standards

**Acceptance Threshold**: All criteria must achieve **A+** grade.

---

## Criterion 1: Completeness (완전성)

### Description
All content from the original Korean document is translated without omissions or additions.

### A+ Requirements
- ✅ 100% of Korean text translated (no Korean text remains)
- ✅ All sections, headings, and subheadings translated
- ✅ All code comments translated to English
- ✅ All table content translated
- ✅ All list items translated
- ✅ Front matter metadata (name, description) translated
- ✅ No content added that wasn't in the original
- ✅ No content removed from the original

### Evaluation Checklist
- [ ] Compare section count: original vs translation
- [ ] Verify all headings are present
- [ ] Check all code comments are in English
- [ ] Verify all table rows/columns translated
- [ ] Confirm metadata fields translated
- [ ] Spot-check paragraphs for omissions

### Common Issues
- Missing bullet points or list items
- Untranslated code comments
- Skipped table cells
- Omitted metadata fields
- Added explanatory text not in original

---

## Criterion 2: Accuracy (정확성)

### Description
Translation accurately conveys the meaning and technical details of the original Korean text.

### A+ Requirements
- ✅ Technical terms translated correctly and consistently
- ✅ No mistranslations or meaning changes
- ✅ Code examples remain functionally identical
- ✅ API names, class names, method names unchanged
- ✅ Numeric values, version numbers preserved exactly
- ✅ Links and file paths unchanged
- ✅ Technical concepts accurately represented

### Evaluation Checklist
- [ ] Verify technical terms against glossary
- [ ] Cross-check code block integrity
- [ ] Confirm API/class names are identical
- [ ] Validate numeric data matches
- [ ] Check links point to same destinations
- [ ] Review technical explanations for accuracy

### Common Issues
- Incorrect technical term translation
- Changed code variable names
- Altered numeric values
- Modified API method names
- Broken or changed links
- Misunderstood technical concepts

---

## Criterion 3: Consistency (일관성)

### Description
Terminology, style, and conventions are consistent throughout the document and across all translated documents.

### A+ Requirements
- ✅ Technical terms follow established glossary
- ✅ Same Korean term always translates to same English term
- ✅ Code formatting conventions preserved
- ✅ Heading capitalization style consistent
- ✅ Voice and tone consistent (e.g., imperative for instructions)
- ✅ Terminology consistent with Java/technical documentation standards

### Evaluation Checklist
- [ ] Check glossary compliance for key terms
- [ ] Verify same term used for repeated concepts
- [ ] Confirm heading capitalization matches style guide
- [ ] Review tone consistency (formal/informal)
- [ ] Validate code formatting unchanged
- [ ] Cross-reference with other translated documents

### Common Issues
- Inconsistent translation of repeated terms
- Mixed capitalization styles
- Switching between formal/informal tone
- Different translations for same Korean word
- Inconsistent code formatting

---

## Criterion 4: Natural English (자연스러운 영어)

### Description
Translation reads naturally in English, not as a word-for-word translation from Korean.

### A+ Requirements
- ✅ Sentences flow naturally in English
- ✅ Proper English idioms and expressions used
- ✅ No awkward or unnatural phrasing
- ✅ Grammar is flawless
- ✅ Word choice is appropriate and natural
- ✅ Reads as if originally written in English
- ✅ Avoids literal/mechanical translation

### Evaluation Checklist
- [ ] Read aloud - does it sound natural?
- [ ] Check for Korean sentence structure patterns
- [ ] Verify proper English article usage (a, an, the)
- [ ] Confirm natural preposition choices
- [ ] Review for fluency and readability
- [ ] Check that context-specific terms are appropriate

### Common Issues
- Word-for-word translation
- Korean sentence structure preserved
- Missing or incorrect articles (a, an, the)
- Unnatural word order
- Awkward phrasing
- Overly literal translation

---

## Criterion 5: Grammar and Syntax (문법과 구문)

### Description
Translation follows English grammar rules perfectly with no errors.

### A+ Requirements
- ✅ Zero grammatical errors
- ✅ Correct subject-verb agreement
- ✅ Proper tense usage (present, past, future)
- ✅ Correct pronoun usage
- ✅ Proper punctuation (commas, periods, colons, semicolons)
- ✅ Correct sentence structure
- ✅ No run-on sentences or fragments

### Evaluation Checklist
- [ ] Run through grammar checker
- [ ] Verify subject-verb agreement in all sentences
- [ ] Check verb tenses are appropriate
- [ ] Confirm punctuation is correct
- [ ] Review sentence completeness
- [ ] Check for parallel structure in lists
- [ ] Verify article usage

### Common Issues
- Subject-verb disagreement
- Incorrect tense
- Missing commas in lists
- Run-on sentences
- Sentence fragments
- Incorrect article usage (a/an/the)
- Wrong pronoun case

---

## Criterion 6: Formatting Preservation (형식 보존)

### Description
Markdown formatting, structure, and visual layout are identical to the original document.

### A+ Requirements
- ✅ All markdown syntax preserved (headings, lists, code blocks, tables)
- ✅ Heading levels unchanged (##, ###, etc.)
- ✅ Code block language tags preserved
- ✅ Link syntax correct and functional
- ✅ Table structure identical
- ✅ Line breaks and spacing preserved where meaningful
- ✅ Front matter YAML structure intact

### Evaluation Checklist
- [ ] Compare heading levels visually
- [ ] Verify code block rendering
- [ ] Test all links (internal and external)
- [ ] Check table column alignment
- [ ] Confirm list formatting (ordered/unordered)
- [ ] Validate front matter YAML syntax
- [ ] Check for unwanted line breaks

### Common Issues
- Changed heading levels
- Broken code block formatting
- Broken links or link syntax
- Malformed tables
- Incorrect list formatting
- YAML syntax errors
- Extra or missing line breaks

---

## Criterion 7: Technical Documentation Standards (기술문서 표준)

### Description
Translation adheres to professional technical documentation standards and best practices.

### A+ Requirements
- ✅ Clear and concise language
- ✅ Consistent use of imperative mood for instructions
- ✅ Proper use of technical writing conventions
- ✅ Code examples properly formatted and syntactically correct
- ✅ API documentation follows JavaDoc-style conventions
- ✅ Appropriate level of detail (not too verbose or too terse)
- ✅ Professional tone throughout

### Evaluation Checklist
- [ ] Verify imperative mood in tutorials ("Open the file" not "You open the file")
- [ ] Check code examples for syntax correctness
- [ ] Review for appropriate technical detail level
- [ ] Confirm professional tone (no casual language)
- [ ] Validate API doc format consistency
- [ ] Check that examples are complete and runnable
- [ ] Verify clarity of explanations

### Common Issues
- Using second person instead of imperative ("You should" vs "Do")
- Overly casual language
- Too verbose or too terse explanations
- Code examples with syntax errors
- Inconsistent API documentation format
- Unclear or ambiguous instructions
- Missing context for code examples

---

## Evaluation Process

### Step 1: Initial Translation
Translate the document from Korean to English following the plan.

### Step 2: Self-Review
Review translation against all 7 criteria, checking each requirement.

### Step 3: Grading
Assign grade (F/C/B/B+/A/A+) to each criterion based on checklist.

### Step 4: Issue Identification
For any criterion below A+, identify specific issues.

### Step 5: Revision
Fix identified issues and update translation.

### Step 6: Re-evaluation
Re-grade all criteria. Repeat steps 4-6 until all criteria achieve A+.

### Step 7: Final Approval
Document passes when all 7 criteria are A+.

---

## Evaluation Template

Use this template for each document evaluation:

```markdown
## Translation Evaluation: [DOCUMENT_NAME]

**Document**: `[path/to/document.md]`
**Original**: `docs/guide/[original].md`
**Translation**: `docs/guide.en/[translated].md`
**Date**: [YYYY-MM-DD]
**Iteration**: [N]

### Criterion 1: Completeness
**Grade**: [A+/A/B+/B/C/F]
**Issues**: 
- [List specific issues or "None"]
**Status**: [✅ PASS / ❌ FAIL]

### Criterion 2: Accuracy
**Grade**: [A+/A/B+/B/C/F]
**Issues**: 
- [List specific issues or "None"]
**Status**: [✅ PASS / ❌ FAIL]

### Criterion 3: Consistency
**Grade**: [A+/A/B+/B/C/F]
**Issues**: 
- [List specific issues or "None"]
**Status**: [✅ PASS / ❌ FAIL]

### Criterion 4: Natural English
**Grade**: [A+/A/B+/B/C/F]
**Issues**: 
- [List specific issues or "None"]
**Status**: [✅ PASS / ❌ FAIL]

### Criterion 5: Grammar and Syntax
**Grade**: [A+/A/B+/B/C/F]
**Issues**: 
- [List specific issues or "None"]
**Status**: [✅ PASS / ❌ FAIL]

### Criterion 6: Formatting Preservation
**Grade**: [A+/A/B+/B/C/F]
**Issues**: 
- [List specific issues or "None"]
**Status**: [✅ PASS / ❌ FAIL]

### Criterion 7: Technical Documentation Standards
**Grade**: [A+/A/B+/B/C/F]
**Issues**: 
- [List specific issues or "None"]
**Status**: [✅ PASS / ❌ FAIL]

### Overall Result
**Total A+ Criteria**: [N/7]
**Overall Status**: [✅ APPROVED / ❌ REVISION NEEDED]

**Action**: [Approve / Revise and re-evaluate]
```

---

## Reference: Technical Glossary

| Korean | English | Context |
|--------|---------|---------|
| 저장소 | store | FxStore context |
| 컬렉션 | collection | Standard Java term |
| 코덱 | codec | Encoder/decoder |
| 커밋 모드 | commit mode | AUTO/BATCH |
| 영속 | persistence/persistent | Context dependent |
| 트랜잭션 | transaction | Standard term |
| 단일 파일 | single-file | Hyphenated adjective |
| 순회 | iteration/traversal | Context dependent |
| 직렬화 | serialization | Standard Java term |
| 역직렬화 | deserialization | Standard Java term |
| 키-값 쌍 | key-value pair | Standard term |
| 읽기 전용 | read-only | Hyphenated adjective |
| 쓰기 | write | Operation context |
| 조회 | query/lookup | Context dependent |
| 삭제 | delete/remove | Context dependent |
| 추가 | add/insert | Context dependent |
| 갱신 | update | Standard term |
| 범위 | range | Standard term |
| 뷰 | view | As in "collection view" |
| 반복자 | iterator | Standard Java term |

---

## Success Criteria Summary

A translation is **APPROVED** when:

1. ✅ All 7 criteria achieve **A+** grade
2. ✅ Zero issues identified in final evaluation
3. ✅ Document builds correctly in markdown viewer
4. ✅ All links tested and functional
5. ✅ Code examples verified for syntax correctness

---

**Document Status**: ✅ ACTIVE
**Version**: 1.0
**Last Updated**: 2026-01-02T02:29:01Z
