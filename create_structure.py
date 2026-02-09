import os
import sys

base_path = r'C:\Users\anupa\copilot-hackathon'

directories = [
    r'src\main\java\com\hackathon\app',
    r'src\main\java\com\hackathon\app\controller',
    r'src\main\java\com\hackathon\app\service',
    r'src\main\java\com\hackathon\app\repository',
    r'src\main\java\com\hackathon\app\entity',
    r'src\main\java\com\hackathon\app\dto',
    r'src\main\java\com\hackathon\app\security',
    r'src\main\java\com\hackathon\app\exception',
    r'src\main\resources',
    r'src\test\java\com\hackathon\app',
    r'.github\workflows'
]

try:
    for directory in directories:
        full_path = os.path.join(base_path, directory)
        os.makedirs(full_path, exist_ok=True)
        print(f'Created: {full_path}')
    
    print('\nDirectory structure created successfully!')
    sys.exit(0)
except Exception as e:
    print(f'Error: {e}')
    sys.exit(1)
