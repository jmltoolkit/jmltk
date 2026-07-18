#!/usr/bin/env python3
"""
Compare shell command output with expected output.

File format:
---
run: <shell command>
ignoreStderr: true|false (optional, default: false)
---

<expected output>
"""

import subprocess
import sys
import tempfile
import os
from pathlib import Path


def parse_test_file(filepath: str) -> tuple[list[str], str, bool]:
    """
    Parse a test file with YAML-like frontmatter.
    
    Returns:
        Tuple of (command_lines, expected_output, ignore_stderr)
    """
    with open(filepath, 'r') as f:
        content = f.read()
    
    # Find the frontmatter between --- markers
    if not content.startswith('---'):
        raise ValueError(f"File must start with '---': {filepath}")
    
    # Split on the closing ---
    parts = content.split('---', 2)
    if len(parts) < 2:
        raise ValueError(f"File must have closing '---': {filepath}")
    
    # Extract header content (between first and second ---)
    header_content = parts[1].strip()
    
    # Everything after the second --- is the expected output
    expected_output = parts[2].strip() if len(parts) > 2 else ""
    
    # Parse command lines and options from header
    command_lines = []
    ignore_stderr = False
    
    for line in header_content.split('\n'):
        line = line.strip()
        if line.startswith('run:'):
            # Extract the command after 'run:'
            cmd = line[4:].strip()
            if cmd:
                command_lines.append(cmd)
        elif line.startswith('ignoreStderr:'):
            # Parse ignoreStderr option
            value = line[13:].strip().lower()
            ignore_stderr = value in ('true', 'yes', '1')
        elif line and command_lines:
            # Additional lines after 'run:' are part of the command
            command_lines.append(line)
    
    if not command_lines:
        raise ValueError(f"No 'run:' command found in header: {filepath}")
    
    return command_lines, expected_output, ignore_stderr


def run_command(command_lines: list[str], ignore_stderr: bool = False) -> str:
    """Execute the command and return stdout."""
    # Join command lines - each line after 'run:' could be arguments or continuation
    full_command = '\n'.join(command_lines)
    
    # Execute using shell
    result = subprocess.run(
        full_command,
        shell=True,
        capture_output=True,
        text=True
    )
    
    # Return stdout (and optionally stderr) for comparison
    output = result.stdout
    if not ignore_stderr and result.stderr:
        output += result.stderr
    
    return output.strip()


def compare_output(actual: str, expected: str) -> bool:
    """Compare actual output with expected output."""
    return actual == expected


def main():
    if len(sys.argv) < 2:
        print("Usage: shelltester.py <test_file> [test_file ...]")
        print("\nTest file format:")
        print("---")
        print("run: <shell command>")
        print("---")
        print("<expected output>")
        sys.exit(1)
    
    test_files = sys.argv[1:]
    passed = 0
    failed = 0
    
    for filepath in test_files:
        path = Path(filepath)
        
        if not path.exists():
            print(f"❌ {filepath}: File not found")
            failed += 1
            continue
        
        try:
            command_lines, expected, ignore_stderr = parse_test_file(filepath)
        except ValueError as e:
            print(f"❌ {filepath}: {e}")
            failed += 1
            continue
        
        try:
            actual = run_command(command_lines, ignore_stderr)
        except Exception as e:
            print(f"❌ {filepath}: Command execution failed: {e}")
            failed += 1
            continue
        
        if compare_output(actual, expected):
            print(f"✅ {filepath}: PASSED")
            passed += 1
        else:
            print(f"❌ {filepath}: FAILED")
            # Write actual output to file beside the test file
            actual_path = str(filepath) + ".actual"
            with open(actual_path, 'w') as f:
                f.write(actual)
            print(f"   Actual output written to: {actual_path}")
            
            # Use diff for better output visualization
            with tempfile.NamedTemporaryFile(mode='w', suffix='.expected', delete=False) as exp_file:
                exp_file.write(expected)
                exp_path = exp_file.name
            with tempfile.NamedTemporaryFile(mode='w', suffix='.actual', delete=False) as act_file:
                act_file.write(actual)
                act_path = act_file.name
            
            try:
                diff_result = subprocess.run(
                    ['diff', '-u', exp_path, act_path],
                    capture_output=True,
                    text=True
                )
                print("   Diff:")
                for line in diff_result.stdout.split('\n'):
                    print(f"   {line}")
            finally:
                os.unlink(exp_path)
                os.unlink(act_path)
            failed += 1
    
    print(f"\nResults: {passed} passed, {failed} failed out of {len(test_files)} tests")
    sys.exit(0 if failed == 0 else 1)


if __name__ == '__main__':
    main()
