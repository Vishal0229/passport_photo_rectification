# Skills & Custom Commands

Slash commands available in this project. Invoke with `/command-name` in the Claude Code prompt.

## Project-specific skills

| Command | What it does |
|---------|-------------|
| `/orchestrate` | Runs the full 4-phase multi-agent pipeline (Coding + Tests → Test Run → Code Review → Docs). Always presents a scoped plan for approval before starting. |
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
       [Code Review Agent]  ← fixes issues
              ↓
Phase 4  ── SEQUENTIAL ──
       [Documentation Agent]  ← no code changes
```

Each agent runs with a **completely isolated context** — no shared memory or conversation history between agents. The orchestrator (main Claude) controls what information flows between phases.

**Before any phase starts**, Claude presents the per-agent scope and waits for your approval.
