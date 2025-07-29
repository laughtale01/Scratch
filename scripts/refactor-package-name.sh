#!/bin/bash
# Bash script to refactor package names from com.yourname to edu.minecraft.collaboration
# This script updates all Java files and related configurations

OLD_PACKAGE="com.yourname.minecraftcollaboration"
NEW_PACKAGE="edu.minecraft.collaboration"
OLD_PATH="com/yourname/minecraftcollaboration"
NEW_PATH="edu/minecraft/collaboration"

echo -e "\033[32mPackage Name Refactoring Script\033[0m"
echo -e "\033[32m==============================\033[0m"
echo -e "\033[33mOld package: $OLD_PACKAGE\033[0m"
echo -e "\033[36mNew package: $NEW_PACKAGE\033[0m"
echo ""

# Function to update file content
update_file_content() {
    local file=$1
    local temp_file="${file}.tmp"
    
    # Create a backup
    cp "$file" "${file}.bak"
    
    # Replace package declarations and imports
    sed -e "s/package $OLD_PACKAGE/package $NEW_PACKAGE/g" \
        -e "s/import $OLD_PACKAGE/import $NEW_PACKAGE/g" \
        -e "s/$OLD_PACKAGE/$NEW_PACKAGE/g" \
        "$file" > "$temp_file"
    
    # Check if file was modified
    if ! cmp -s "$file" "$temp_file"; then
        mv "$temp_file" "$file"
        echo -e "\033[32mUpdated: $file\033[0m"
        return 0
    else
        rm "$temp_file"
        rm "${file}.bak"
        return 1
    fi
}

# Base directory
BASE_DIR="minecraft-mod"
SRC_MAIN_JAVA="$BASE_DIR/src/main/java"
SRC_TEST_JAVA="$BASE_DIR/src/test/java"

# New directory structure
NEW_MAIN_DIR="$SRC_MAIN_JAVA/edu/minecraft/collaboration"
NEW_TEST_DIR="$SRC_TEST_JAVA/edu/minecraft/collaboration"

# Step 1: Create new directory structure
echo -e "\n\033[36mStep 1: Creating new directory structure...\033[0m"
mkdir -p "$NEW_MAIN_DIR"
mkdir -p "$NEW_TEST_DIR"
echo -e "\033[32mCreated: $NEW_MAIN_DIR\033[0m"
echo -e "\033[32mCreated: $NEW_TEST_DIR\033[0m"

# Step 2: Update all Java files
echo -e "\n\033[36mStep 2: Updating Java files...\033[0m"
updated_count=0

# Find all Java files and update them
find "$BASE_DIR" -name "*.java" -type f | while read -r file; do
    if update_file_content "$file"; then
        ((updated_count++))
    fi
done

echo -e "\033[32mUpdated Java files\033[0m"

# Step 3: Update build.gradle
echo -e "\n\033[36mStep 3: Updating build.gradle...\033[0m"
if [ -f "$BASE_DIR/build.gradle" ]; then
    update_file_content "$BASE_DIR/build.gradle"
fi

# Step 4: Update configuration files
echo -e "\n\033[36mStep 4: Updating configuration files...\033[0m"
config_files=(
    "$BASE_DIR/src/main/resources/META-INF/mods.toml"
    "$BASE_DIR/gradle.properties"
)

for file in "${config_files[@]}"; do
    if [ -f "$file" ]; then
        update_file_content "$file"
    fi
done

# Step 5: Instructions for moving files
echo -e "\n\033[36mStep 5: Moving files to new package structure...\033[0m"
echo -e "\033[33mThis step requires manual execution to preserve file integrity\033[0m"
echo -e "\033[33mPlease run the following commands:\033[0m"
echo ""
echo -e "\033[37m# For main source files:\033[0m"
echo -e "\033[90mmv $SRC_MAIN_JAVA/$OLD_PATH/* $NEW_MAIN_DIR/\033[0m"
echo ""
echo -e "\033[37m# For test files:\033[0m"
echo -e "\033[90mmv $SRC_TEST_JAVA/$OLD_PATH/* $NEW_TEST_DIR/\033[0m"
echo ""
echo -e "\033[37m# Remove old directories:\033[0m"
echo -e "\033[90mrm -rf $SRC_MAIN_JAVA/com\033[0m"
echo -e "\033[90mrm -rf $SRC_TEST_JAVA/com\033[0m"

echo -e "\n\033[32m==============================\033[0m"
echo -e "\033[32mPackage refactoring preparation complete!\033[0m"
echo -e "\033[33mPlease review the changes and run the move commands manually.\033[0m"

# Make the script executable
chmod +x "$0"