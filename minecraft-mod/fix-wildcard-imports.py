#!/usr/bin/env python3
"""
Fix wildcard imports in Java files by replacing them with explicit imports
"""

import re
import os

# Define import replacements
IMPORT_REPLACEMENTS = {
    "import java.util.*": [
        "import java.util.ArrayList",
        "import java.util.Collection", 
        "import java.util.HashMap",
        "import java.util.List",
        "import java.util.Map",
        "import java.util.Queue",
        "import java.util.Set"
    ],
    "import java.util.concurrent.*": [
        "import java.util.concurrent.ConcurrentHashMap",
        "import java.util.concurrent.ConcurrentLinkedQueue",
        "import java.util.concurrent.CopyOnWriteArrayList",
        "import java.util.concurrent.Executors",
        "import java.util.concurrent.ScheduledExecutorService",
        "import java.util.concurrent.TimeUnit",
        "import java.util.concurrent.atomic.AtomicLong"
    ]
}

def fix_wildcard_imports(file_path):
    """Fix wildcard imports in a single file"""
    
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    modified = False
    
    # Replace each wildcard import
    for wildcard, replacements in IMPORT_REPLACEMENTS.items():
        if wildcard in content:
            # Find what's actually used in the file
            used_imports = []
            for replacement in replacements:
                # Extract class name from import statement
                class_name = replacement.split('.')[-1]
                # Check if class is used in the file (simple heuristic)
                if re.search(r'\b' + class_name + r'\b', content):
                    used_imports.append(replacement)
            
            if used_imports:
                # Replace wildcard with explicit imports
                replacement_text = ';\n'.join(used_imports)
                content = content.replace(wildcard, replacement_text)
                modified = True
                print(f"Fixed {file_path}: replaced '{wildcard}' with {len(used_imports)} explicit imports")
    
    if modified:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    
    return False

def main():
    """Main function to fix all files"""
    
    # Files with wildcard imports (from the grep results)
    files_to_fix = [
        "src/main/java/edu/minecraft/collaboration/monitoring/apm/PerformanceProfiler.java",
        "src/main/java/edu/minecraft/collaboration/security/threat/UserThreatProfile.java",
        "src/main/java/edu/minecraft/collaboration/security/threat/ThreatAssessment.java",
        "src/main/java/edu/minecraft/collaboration/security/threat/ThreatDetectionEngine.java",
        "src/main/java/edu/minecraft/collaboration/security/zerotrust/RiskAssessment.java",
        "src/main/java/edu/minecraft/collaboration/security/zerotrust/RiskAssessmentEngine.java",
        "src/main/java/edu/minecraft/collaboration/security/zerotrust/PolicyEngine.java",
        "src/main/java/edu/minecraft/collaboration/security/zerotrust/ZeroTrustAccessControl.java",
        "src/main/java/edu/minecraft/collaboration/security/jwt/JWTAuthenticationProvider.java"
    ]
    
    fixed_count = 0
    for file_path in files_to_fix:
        if os.path.exists(file_path):
            if fix_wildcard_imports(file_path):
                fixed_count += 1
        else:
            print(f"File not found: {file_path}")
    
    print(f"\nFixed {fixed_count} files with wildcard imports")

if __name__ == "__main__":
    main()