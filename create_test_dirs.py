import os

base_path = r'C:\Users\anupa\copilot-hackathon'

test_directories = [
    r'src\test\java\com\hackathon\app\controller',
    r'src\test\java\com\hackathon\app\service',
    r'src\test\java\com\hackathon\app\repository'
]

for directory in test_directories:
    full_path = os.path.join(base_path, directory)
    os.makedirs(full_path, exist_ok=True)
    print(f'Created: {full_path}')

print('\nTest subdirectories created!')
