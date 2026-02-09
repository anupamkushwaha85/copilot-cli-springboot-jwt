@echo off
REM Script to add remote and push to GitHub
REM Usage: git_push.bat <your-github-repo-url>

if "%~1"=="" (
    echo Error: GitHub repository URL is required
    echo.
    echo Usage: git_push.bat ^<your-github-repo-url^>
    echo Example: git_push.bat https://github.com/username/copilot-hackathon.git
    echo.
    pause
    exit /b 1
)

cd /d "%~dp0"

echo Adding remote repository...
git remote add origin %1

echo Setting main branch...
git branch -M main

echo Pushing to GitHub...
git push -u origin main

echo.
echo ========================================
echo Successfully pushed to GitHub!
echo ========================================
echo.
echo Repository URL: %1
echo.
pause
