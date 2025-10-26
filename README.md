# Sparely

Sparely is an Android personal finance / savings app. This repository contains the app source code and developer documentation.

This repo is being prepared for open-source release. The codebase may contain unfinished features and known bugs.

Quick links
- See `README_IMPROVEMENTS.md` for feature and integration notes.
- License: MIT (see `LICENSE`)

Build & run (local)

Requirements
- Android SDK (as configured in your `local.properties`)
- JDK 11

Build
```powershell
cd 'c:\Users\loic\AndroidStudioProjects\Sparely'
.\gradlew.bat clean assembleDebug
```

Notes before contributing
- Do NOT commit private keys or `google-services.json`/`keystore.properties`/`secrets.properties`.
- `local.properties` should remain local — it contains the SDK path.
- The repository already ignores common sensitive files; please check `.gitignore` before adding secrets.

Contributing
Please read `CONTRIBUTING.md` and `CODE_OF_CONDUCT.md` before opening issues or pull requests.
