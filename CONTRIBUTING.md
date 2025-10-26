# Contributing to Sparely

Thanks for your interest in contributing! A few guidelines to help your contribution go smoothly.

Before you start
- Check open issues and existing PRs to avoid duplicate work.
- Make sure you have the Android SDK and JDK 11 installed.

How to contribute
1. Fork the repository and create a feature branch off `master`.
2. Make small, focused changes with clear commit messages.
3. Add or update tests where applicable.
4. Open a pull request describing the change and why it is needed.

PR checklist
- [ ] My code builds and runs (at least `assembleDebug`).
- [ ] No secrets or keystores are added.
- [ ] I added or updated tests for my change where reasonable.
- [ ] I ran `./gradlew test` locally and fixed failing tests.

Style
- This project uses Kotlin & Compose conventions. Keep UI modular and state hoisted where appropriate.

Reporting security issues
- Please follow `SECURITY.md` to privately report any security vulnerabilities.
