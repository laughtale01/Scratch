#!/usr/bin/env python3
import os
import re

def fix_test_imports(directory):
    """Fix all import statements in test files"""
    
    # Mapping of wrong imports to correct ones
    replacements = {
        'edu.minecraft.collaboration': 'com.yourname.minecraftcollaboration',
        'import edu.minecraft.collaboration.collaboration.Invitation': 'import com.yourname.minecraftcollaboration.collaboration.CollaborationManager.Invitation',
        'import edu.minecraft.collaboration.collaboration.VisitRequest': 'import com.yourname.minecraftcollaboration.collaboration.CollaborationManager.VisitRequest',
    }
    
    fixed_count = 0
    
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith('.java'):
                filepath = os.path.join(root, file)
                
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                original_content = content
                
                # Apply replacements
                for old, new in replacements.items():
                    content = content.replace(old, new)
                
                # Fix getName() method calls
                content = re.sub(
                    r'when\((\w+)\.getName\(\)\)\.thenReturn\(\(\) -> "([^"]+)"\)',
                    r'when(\1.getName()).thenReturn(Component.literal("\2"))',
                    content
                )
                
                # Fix CollaborationCoordinator constructor
                content = re.sub(
                    r'new CollaborationCoordinator\(mockServer\)',
                    r'new CollaborationCoordinator()',
                    content
                )
                
                # Fix CompletableFuture<Boolean> vs boolean
                content = re.sub(
                    r'CompletableFuture<Boolean> future = coordinator\.requestVisit',
                    r'boolean result = coordinator.requestVisit',
                    content
                )
                
                if content != original_content:
                    with open(filepath, 'w', encoding='utf-8') as f:
                        f.write(content)
                    fixed_count += 1
                    print(f"Fixed: {filepath}")
    
    print(f"\nTotal files fixed: {fixed_count}")

if __name__ == "__main__":
    fix_test_imports("src/test/java")