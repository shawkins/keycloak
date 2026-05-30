<!-- see https://dev.to/techgirl1908/how-i-taught-github-copilot-code-review-to-think-like-a-maintainer-3l2c -->

## Review Philosophy

* Only comment when you have HIGH CONFIDENCE (>90%) that an issue exists
* Be concise: one sentence per comment when possible
* Focus on actionable feedback, not observations
* When reviewing text, only comment on clarity issues if the text is genuinely confusing or could lead to errors.

## Priority Areas (Review These)

### Repository-specific expectations

- Every PR should map to a GitHub issue and keep a focused scope (`CONTRIBUTING.md`).
- The commit message should map to the same GitHub issue
- Include docs/tests when behavior changes.
- Do not introduce new test frameworks or broad formatting/refactoring unrelated to the task.

### Security & Safety

* Command injection risks (shell commands, user input)
* Path traversal vulnerabilities
* Credential exposure or hardcoded secrets
* Missing input validation on external data
* Improper error handling that could leak sensitive info

### Correctness Issues

* Logic errors that could cause panics or incorrect behavior
* Race conditions in async code
* Resource leaks (files, connections, memory)
* Off-by-one errors or boundary conditions
* Incorrect error propagation (using `unwrap()` inappropriately)
* Optional types that don’t need to be optional
* Booleans that should default to false but are set as optional
* Error context that doesn’t add useful information
* Overly defensive code with unnecessary checks
* Unnecessary comments that restate obvious code behavior

## Skip These (Low Value)

Do not comment on:

* Style/formatting
* Test failures
* Minor naming suggestions
* Suggestions to add comments
* Refactoring unless addressing a real bug
* Multiple issues in one comment
* Logging suggestions unless security-related
* Pedantic text accuracy unless it affects meaning

## CI Pipeline Context

**Important**: You review PRs immediately, before CI completes. Do not flag issues that CI will catch.

<!-- elaborate on what is covered -->

## When to Stay Silent

If you’re uncertain whether something is an issue, don’t comment.