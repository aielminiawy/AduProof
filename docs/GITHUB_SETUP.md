# Publishing AduProof to your GitHub account

A GitHub connection was available for the account `aielminiawy`, but the connected actions could list and edit existing repositories only; it did not expose a "create repository" action. Therefore **no GitHub repository was created or changed automatically**.

## Publish as an open-source repository

1. Sign in to GitHub and create a **public** repository named `AduProof`. Use an empty repository (no automatic README, .gitignore, or license, because these files are included).
2. Download and extract the `AduProof-source.zip` archive. Open a terminal/PowerShell inside the extracted `AduProof` folder.
3. If Git is installed, run:

```bash
git init
git add .
git commit -m "Initial AduProof local prototype"
git branch -M main
git remote add origin https://github.com/aielminiawy/AduProof.git
git push -u origin main
```

4. Check that `README.md`, `LICENSE`, `.github/workflows/java-checks.yml`, `src/`, `scripts/`, and `docs/` appear on GitHub.
5. Check the Actions tab: the `Java checks` workflow should compile the project and run its tests.
6. Enable Issues, add topics such as `java`, `ed25519`, `digital-signatures`, and `open-source` if appropriate.

The remote URL above is a **suggested name** that does not exist until you create it. If you choose a different repo name, replace the URL accordingly.

Never upload the local `data/` folder, private signing keys, or user information.
