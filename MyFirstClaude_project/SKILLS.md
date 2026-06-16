# Skills & Custom Commands

Slash commands available in this project. Invoke with `/command-name` in the Claude Code prompt.

## Project-specific skills

| Command | What it does |
|---------|-------------|
| `/orchestrate` | Runs the full 5-phase multi-agent pipeline (see diagram below). Always presents a scoped plan for approval before starting. |
| `/run-backend` | Kills any stale port-8080 listener and starts the Spring Boot backend |
| `/run-frontend` | Starts the Vite dev server for the React frontend |

## Built-in Claude Code skills (always available)

| Command | What it does |
|---------|-------------|
| `/code-review` | Review current diff for bugs and cleanups. Add `--fix` to apply findings, `--comment` to post as PR comments. Use `ultra` for deep multi-agent cloud review. |
| `/verify` | Runs the app and observes behaviour to confirm a change works |
| `/run` | Launches the app (auto-detects project type) |
| `/simplify` | Reviews changed code for reuse/simplification and applies fixes |
| `/security-review` | Full security review of pending branch changes |
| `/update-config` | Configures Claude Code settings (permissions, hooks, env vars) |
| `/fewer-permission-prompts` | Scans session transcripts and adds common commands to the auto-allow list in settings.json |
| `/eli5` | Explains a concept in simple terms |
| `/step-by-step` | Plans a task step-by-step, explains each step, then executes |

## How /orchestrate works

```
Phase 1  ──────── PARALLEL ──────────
  [Coding Agent]   [Test Writing Agent]
        ↓                  ↓
Phase 2  ── SEQUENTIAL (after Phase 1) ──
       [Test Running Agent]
              ↓
Phase 3  ── SEQUENTIAL ──
       [Code Review Agent]  ← fixes code issues
              ↓
Phase 4  ── SEQUENTIAL ──
       [Security Agent]  ← scans for vulnerabilities
                         → writes securityIssues.md
              ↓
     Issues found?
     │                      │
     ▼ YES (1 cycle only)   ▼ NO
  [Coding Agent]            │
  reads securityIssues.md   │
  fixes security bugs        │
        ↓                   │
  [Test Running Agent]      │
  re-run tests               │
        ↓                   │
  [Code Review Agent]       │
  re-review                  │
        ↓                   │
  [Security Agent]          │
  re-scan → overwrites       │
  securityIssues.md          │
        ↓                   │
        └───────────────────┘
              ↓
Phase 5  ── SEQUENTIAL (security scan complete) ──
       [Documentation Agent]  ← no code changes
```

**Context isolation:** Each agent runs with a completely isolated context — no shared memory or
conversation history. The orchestrator (main Claude) controls what flows between phases.
`securityIssues.md` is the only cross-agent file — always overwritten, never appended.

**Before any phase starts**, Claude presents the per-agent scope and waits for your approval.
