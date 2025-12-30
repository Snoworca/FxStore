#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Package rename script: com.fxstore -> com.snoworca.fxstore
"""

import os
import shutil
import re

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
SRC_MAIN = os.path.join(BASE_DIR, "src", "main", "java")
SRC_TEST = os.path.join(BASE_DIR, "src", "test", "java")

OLD_PKG = "com.fxstore"
NEW_PKG = "com.snoworca.fxstore"
OLD_PATH = os.path.join("com", "fxstore")
NEW_PATH = os.path.join("com", "snoworca", "fxstore")

def process_java_file(filepath):
    """Replace package references in a Java file."""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
    except UnicodeDecodeError:
        # Try with other encodings if UTF-8 fails
        try:
            with open(filepath, 'r', encoding='cp949') as f:
                content = f.read()
        except:
            print(f"  [ERROR] Cannot read: {filepath}")
            return False

    # Replace package declarations and imports
    new_content = content.replace(OLD_PKG, NEW_PKG)

    if new_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"  [MODIFIED] {filepath}")
        return True
    return False

def move_directory(src_root):
    """Move com/fxstore to com/snoworca/fxstore."""
    old_dir = os.path.join(src_root, OLD_PATH)
    new_dir = os.path.join(src_root, NEW_PATH)

    if not os.path.exists(old_dir):
        print(f"  [SKIP] Directory not found: {old_dir}")
        return

    # Create new directory structure
    os.makedirs(os.path.dirname(new_dir), exist_ok=True)

    # Move the directory
    if os.path.exists(new_dir):
        shutil.rmtree(new_dir)
    shutil.move(old_dir, new_dir)
    print(f"  [MOVED] {old_dir} -> {new_dir}")

def process_all_java_files(root_dir):
    """Process all Java files in directory."""
    count = 0
    for dirpath, _, filenames in os.walk(root_dir):
        for filename in filenames:
            if filename.endswith('.java'):
                filepath = os.path.join(dirpath, filename)
                if process_java_file(filepath):
                    count += 1
    return count

def main():
    print("=" * 60)
    print("Package Rename: com.fxstore -> com.snoworca.fxstore")
    print("=" * 60)

    # Step 1: Process all Java files (replace text)
    print("\n[Step 1] Replacing package references in Java files...")

    total = 0
    for src_dir in [SRC_MAIN, SRC_TEST]:
        if os.path.exists(src_dir):
            print(f"\nProcessing: {src_dir}")
            total += process_all_java_files(src_dir)

    print(f"\nTotal files modified: {total}")

    # Step 2: Move directories
    print("\n[Step 2] Moving directory structure...")

    for src_dir in [SRC_MAIN, SRC_TEST]:
        if os.path.exists(src_dir):
            move_directory(src_dir)

    print("\n" + "=" * 60)
    print("Package rename completed!")
    print("=" * 60)

if __name__ == "__main__":
    main()
