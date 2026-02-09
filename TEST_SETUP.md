# Test Setup Instructions

## Issue
PowerShell Core (pwsh) is not installed on this system, which prevents automated directory creation through the tool interface.

## Solution - Create Test Directories

Choose one of these options:

### Option 1: Run the Python script
```bash
python create_test_dirs.py
```

### Option 2: Run the batch file (Windows)
```cmd
create_test_subdirs.bat
```

### Option 3: Create directories manually
Create these directories under `src\test\java\com\hackathon\app\`:
- `controller`
- `service`  
- `repository`

## Test Files Ready to Create

Once the directories exist, create these two test files:

### 1. AuthControllerTest.java
Path: `src\test\java\com\hackathon\app\controller\AuthControllerTest.java`

Tests registration, login, duplicate username handling, and invalid credentials.

### 2. TaskControllerTest.java  
Path: `src\test\java\com\hackathon\app\controller\TaskControllerTest.java`

Tests task CRUD operations, authorization, and error handling.

## Note
The test file contents are provided below. After creating the directories using one of the options above, I can create these test files for you, or you can copy the content from the conversation history above where the test files were attempted to be created.
