@echo off
echo Initializing Git repository...

cd /d "%~dp0"

REM Initialize git repository
git init

REM Create docs/screenshots directory
if not exist "docs\screenshots" mkdir "docs\screenshots"

REM Create placeholder file in screenshots directory
echo # Screenshots Directory > docs\screenshots\README.md
echo. >> docs\screenshots\README.md
echo Add your project screenshots here: >> docs\screenshots\README.md
echo. >> docs\screenshots\README.md
echo - copilot-session.png >> docs\screenshots\README.md
echo - project-structure.png >> docs\screenshots\README.md
echo - entity-generation.png >> docs\screenshots\README.md
echo - jwt-security.png >> docs\screenshots\README.md
echo - 403-error.png >> docs\screenshots\README.md
echo - fix-applied.png >> docs\screenshots\README.md
echo - success-response.png >> docs\screenshots\README.md
echo - database-config.png >> docs\screenshots\README.md
echo - api-testing.png >> docs\screenshots\README.md
echo - documentation.png >> docs\screenshots\README.md
echo - adaptive-solutions.png >> docs\screenshots\README.md
echo - productivity-graph.png >> docs\screenshots\README.md

REM Add all files to git
git add .

REM Create initial commit
git commit -m "Initial commit: Spring Boot backend with JWT authentication built using GitHub Copilot CLI"

echo.
echo ========================================
echo Git repository initialized successfully!
echo ========================================
echo.
echo Next steps:
echo 1. Create a repository on GitHub
echo 2. Run the following commands with your repo URL:
echo.
echo    git remote add origin YOUR_REPO_URL
echo    git branch -M main
echo    git push -u origin main
echo.
pause
