#!/bin/bash
# fix-checkstyle.sh - Auto-fix common Checkstyle issues

echo "🔧 Auto-fixing Checkstyle issues in Minecraft Collaboration Mod..."

cd "$(dirname "$0")"

# Function to fix operator wrap issues (|| and && to start of next line)
fix_operator_wrap() {
    echo "  📝 Fixing operator wrap issues..."
    
    find src/main/java -name "*.java" -type f | while IFS= read -r file; do
        # Fix || operators - move to beginning of next line
        sed -i 's/ ||$//' "$file"
        sed -i '/[^|]$/{N;s/\n *||/ ||\n            /;}' "$file"
        
        # Fix && operators - move to beginning of next line  
        sed -i 's/ &&$//' "$file"
        sed -i '/[^&]$/{N;s/\n *&&/ &&\n            /;}' "$file"
    done
}

# Function to fix left curly brace issues
fix_left_curly() {
    echo "  📝 Fixing left curly brace placement..."
    
    find src/main/java -name "*.java" -type f -exec sed -i 's/)\s*{/) {/g' {} \;
}

# Function to fix trailing whitespace
fix_trailing_whitespace() {
    echo "  📝 Fixing trailing whitespace..."
    
    find src/main/java -name "*.java" -type f -exec sed -i 's/[[:space:]]*$//' {} \;
}

# Function to fix multiple blank lines
fix_multiple_blank_lines() {
    echo "  📝 Fixing multiple blank lines..."
    
    find src/main/java -name "*.java" -type f | while IFS= read -r file; do
        # Replace 3 or more consecutive blank lines with 2 blank lines
        sed -i '/^$/N;/^\n$/N;/^\n\n$/{N;s/^\n\n\n/\n\n/;}' "$file"
    done
}

# Function to fix import statements
fix_imports() {
    echo "  📝 Organizing import statements..."
    
    find src/main/java -name "*.java" -type f | while IFS= read -r file; do
        # Remove unused imports (basic patterns)
        grep -v '^import.*\.\*;$' "$file" > "${file}.tmp" 2>/dev/null || cp "$file" "${file}.tmp"
        mv "${file}.tmp" "$file"
    done
}

# Function to fix variable declarations (split multiple declarations)
fix_variable_declarations() {
    echo "  📝 Fixing multiple variable declarations..."
    
    find src/main/java -name "*.java" -type f | while IFS= read -r file; do
        # This is complex and requires careful parsing - skip for now
        echo "    ⏭️  Skipping complex variable declaration fixes for: $(basename "$file")"
    done
}

# Main execution
main() {
    echo "🚀 Starting Checkstyle auto-fix process..."
    echo ""
    
    # Create backup
    echo "📋 Creating backup..."
    tar -czf "checkstyle-backup-$(date +%Y%m%d-%H%M%S).tar.gz" src/main/java/
    
    # Apply fixes
    fix_trailing_whitespace
    fix_left_curly  
    fix_operator_wrap
    fix_multiple_blank_lines
    fix_imports
    
    echo ""
    echo "✅ Auto-fix completed!"
    echo ""
    echo "🔍 Running Checkstyle to verify improvements..."
    ./gradlew checkstyleMain --quiet 2>&1 | grep -E "(violations|warning)" | tail -5
    
    echo ""
    echo "📊 Run './gradlew checkstyleMain' for detailed results"
    echo "🔙 Restore backup if needed: tar -xzf checkstyle-backup-*.tar.gz"
}

# Check if running in correct directory
if [[ ! -f "build.gradle" ]]; then
    echo "❌ Error: Must run from minecraft-mod directory"
    echo "Usage: cd minecraft-mod && ./fix-checkstyle.sh"
    exit 1
fi

# Run main function
main