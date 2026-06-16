# /orchestrate — Multi-Agent Coding Workflow

Runs the standard 5-phase multi-agent pipeline for any coding task on this project.
Each agent gets a fresh, isolated context — no shared conversation history between agents.

## Before starting

1. Identify what task or set of uncommitted changes needs to be processed (run `git diff HEAD` if needed).
2. Draft the per-agent scope for this specific task:
   - **Coding Agent**: what to implement or finalize
   - **Test Writing Agent**: what behaviours to test
   - **Test Running Agent**: which test targets to run
   - **Code Review Agent**: what to review and fix
   - **Security Agent**: what surface area to scan
   - **Documentation Agent**: which docs to update
3. Present the scoped plan to the user and ask: *"Are you good to proceed, or anything to add?"*
4. **Do not spawn any agents until the user confirms.**

---

## Phase 1 — Run in parallel

Spawn both agents in a single message (two Agent tool calls):

**Coding Agent**
- Task: implement or finalize the code change
- May edit production code only
- Must NOT write tests, do code-review cleanup, or write documentation

**Test Writing Agent**
- Task: write test cases based on requirements/spec
- May create/edit test files only
- Must NOT run tests, modify production code, or write documentation

Wait for both to complete before proceeding.

---

## Phase 2 — Sequential (after Phase 1)

**Test Running Agent**
- Task: run the test suite (`mvn test` for backend, or specific test classes)
- Reports pass/fail, failure messages, stack traces
- Must NOT fix any code

Wait for results before proceeding.

---

## Phase 3 — Sequential (after Phase 2)

**Code Review Agent**
- Receives: summary of Phase 1 changes + Phase 2 test results
- Task: review all changed files, fix every correctness bug, null-safety gap, resource leak, or test error found
- May edit production code and test files
- Must NOT write documentation

Wait for completion before proceeding.

---

## Phase 4 — Sequential (after Phase 3)

**Security Agent**
- Task: scan all changed files for security vulnerabilities (OWASP Top 10, injection, auth issues, sensitive data exposure, insecure dependencies, etc.)
- If issues found: **overwrite** `securityIssues.md` in the project root with a structured list of findings (severity, file, line, description, recommended fix)
- If no issues found: write "No security issues found." to `securityIssues.md` and report clean
- Must NOT fix code itself

### If Security Agent finds issues — ONE fix cycle (run sequentially):

**4a — Coding Agent (security fix)**
- Reads `securityIssues.md`
- Fixes every listed issue in production code
- Must NOT modify tests or documentation

**4b — Test Running Agent**
- Re-runs the test suite
- Reports pass/fail only, no fixes

**4c — Code Review Agent**
- Re-reviews all changed files (including security fixes from 4a)
- Fixes any new issues introduced by the security fixes
- Must NOT write documentation

**4d — Security Agent (re-scan)**
- Re-scans all changed files
- Overwrites `securityIssues.md` with updated findings (or "No security issues found.")

After this single cycle, proceed to Phase 5 regardless of remaining findings (any residual issues are documented in `securityIssues.md` for manual review).

### If Security Agent finds no issues — skip the fix cycle, proceed directly to Phase 5.

---

## Phase 5 — Sequential (after Phase 4, only when security scan is complete)

**Documentation Agent**
- Receives: summary of what was implemented, reviewed, and any security findings
- Task: update CLAUDE.md (and any other docs) to reflect the changes
- If `securityIssues.md` has content, note that a security review was performed
- Must NOT modify production code or test files

---

## Context isolation rules

- Each agent is spawned via the Agent tool — it starts with zero conversation history
- Pass only what is strictly necessary in each agent's prompt
- Never pre-load one agent's findings into another agent's prompt during the same phase
- The orchestrator (main Claude) relays outputs between phases
- `securityIssues.md` is the only cross-agent communication file — it is always overwritten, never appended
