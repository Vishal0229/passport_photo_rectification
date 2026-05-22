# Agent Pipeline

The Passport Photo Corrector was built and maintained using a multi-agent Claude Code pipeline. Each agent has a focused role and communicates with the next agent through structured handoff JSON files stored in this directory.

---

## Agent Roles

### Coder Agent

**Purpose:** Implements features, fixes bugs, and writes application code.

**Inputs:**
- Feature requests or bug descriptions from the user
- `agents/coder-handoff.json` from a previous run (if iterating)

**Outputs:**
- Modified source files in `passport-photo-backend/` and `passport-photo-frontend/`
- `agents/coder-handoff.json` — a summary of what was built, key design decisions, and anything the tester needs to know

**What it writes in `coder-handoff.json`:**
```json
{
  "summary": "Short description of what was implemented",
  "filesChanged": ["list of modified files"],
  "designDecisions": ["key decisions and why"],
  "knownLimitations": ["things intentionally left simple or deferred"],
  "testingNotes": ["hints for the tester about edge cases"]
}
```

**How to invoke:**
```
/run  (to start the app and verify changes)
```
Or describe the feature directly to Claude Code.

---

### Tester Agent

**Purpose:** Writes and runs tests to verify that the coder's implementation is correct.

**Inputs:**
- `agents/coder-handoff.json`
- The current state of the codebase

**Outputs:**
- Test files in `passport-photo-backend/src/test/` and `passport-photo-frontend/src/`
- `agents/tester-handoff.json` — test coverage summary and any bugs found

**What it writes in `tester-handoff.json`:**
```json
{
  "backendTestCount": 49,
  "frontendTestCount": 38,
  "coverage": {
    "PhotoController": "all endpoints, all error paths",
    "PhotoAnalysisService": "all 5 compliance checks, correction logic"
  },
  "bugsFound": ["list of bugs discovered during testing"],
  "notesForReviewer": ["anything the reviewer should check"]
}
```

**How to invoke:**
```powershell
# Backend tests
cd passport-photo-backend && mvn test

# Frontend tests
cd passport-photo-frontend && npm test -- --run
```

---

### Reviewer Agent

**Purpose:** Reviews the diff for correctness bugs, security issues, and design problems. Does not write new features.

**Inputs:**
- `agents/coder-handoff.json`
- `agents/tester-handoff.json`
- The current git diff

**Outputs:**
- Inline review comments (via `/code-review --comment`) or a written report
- `agents/reviewer-handoff.json` — final state, remaining issues, and notes for the documentation agent

**What it writes in `reviewer-handoff.json`:**
```json
{
  "verdict": "APPROVE | REQUEST_CHANGES",
  "findings": ["list of issues found"],
  "remainingIssues": ["known issues not yet fixed"],
  "notesForDocs": ["things the documentation agent should highlight"],
  "securityNotes": ["any security considerations"]
}
```

**How to invoke:**
```
/code-review
/security-review
```

---

### Documentation Agent

**Purpose:** Writes all documentation. Does not modify application code.

**Inputs:**
- `CLAUDE.md`
- `agents/coder-handoff.json`
- `agents/tester-handoff.json`
- `agents/reviewer-handoff.json`
- All source files

**Outputs:**
- `README.md` — project overview and user guide
- `API.md` — complete REST API reference
- JavaDoc on all Java source files
- JSDoc on all React components and services
- `CONTRIBUTING.md` — developer guide
- `agents/README.md` — this file

**How to invoke:**
Describe the documentation task to Claude Code, prefixed with the instruction to act as the Documentation Agent.

---

## Running the Full Pipeline

Run agents in order. Each agent reads the previous agent's handoff file.

```
1. Coder Agent   → produces agents/coder-handoff.json
2. Tester Agent  → produces agents/tester-handoff.json
3. Reviewer Agent → produces agents/reviewer-handoff.json
4. Docs Agent    → produces README.md, API.md, JavaDoc, JSDoc, CONTRIBUTING.md
```

To re-run a single agent, just invoke it directly. It will use the most recent handoff files available.

---

## Handoff Files

All handoff files live in this `agents/` directory:

| File | Written by | Read by |
|------|-----------|---------|
| `coder-handoff.json` | Coder | Tester, Reviewer, Docs |
| `tester-handoff.json` | Tester | Reviewer, Docs |
| `reviewer-handoff.json` | Reviewer | Docs |

These files are the "message bus" between agents. They are plain JSON and can be read or edited by hand to guide the next agent.

---

## Reading Handoff Files

Each handoff file is valid JSON. To read the coder's design decisions:

```powershell
Get-Content agents\coder-handoff.json | ConvertFrom-Json | Select-Object -ExpandProperty designDecisions
```

To read known limitations from the reviewer:

```powershell
Get-Content agents\reviewer-handoff.json | ConvertFrom-Json | Select-Object -ExpandProperty remainingIssues
```

---

## Adding a New Agent

1. Define the agent's role, inputs, and outputs clearly.
2. Add a handoff JSON file that the agent writes at the end of its run.
3. Update this README with the new agent's section.
4. Update `CONTRIBUTING.md` with instructions on how to invoke it.
