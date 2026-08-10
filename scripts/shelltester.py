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


def collect_test_files(args: list[str]) -> list[Path]:
    """Collect all test files from arguments (files or directories)."""
    test_files = []
    for arg in args:
        path = Path(arg)
        if not path.exists():
            print(f"⚠️  {arg}: Not found, skipping")
            continue
        if path.is_file():
            test_files.append(path)
        elif path.is_dir():
            # Recursively find all test files in directory
            for file in path.rglob('*.txt'):
                if file.is_file():
                    test_files.append(file)
    return test_files


def compare_output(actual: str, expected: str) -> bool:
    """Compare actual output with expected output."""
    return actual == expected


def run_single_test(filepath: Path) -> tuple[bool, str]:
    """Run a single test file and return (passed, message)."""
    if not filepath.exists():
        return False, "File not found"
    
    try:
        command_lines, expected, ignore_stderr = parse_test_file(str(filepath))
    except ValueError as e:
        return False, str(e)
    
    try:
        actual = run_command(command_lines, ignore_stderr)
    except Exception as e:
        return False, f"Command execution failed: {e}"
    
    if compare_output(actual, expected):
        return True, "PASSED"
    else:
        # Write actual output to file beside the test file
        actual_path = filepath.with_suffix(filepath.suffix + ".actual")
        actual_path.write_text(actual)
        
        # Use diff for better output visualization
        with tempfile.NamedTemporaryFile(mode='w', suffix='.expected', delete=False) as exp_file:
            exp_file.write(expected)
            exp_path = exp_file.name
        with tempfile.NamedTemporaryFile(mode='w', suffix='.actual', delete=False) as act_file:
            act_file.write(actual)
            act_path = act_file.name
        
        diff_output = ""
        try:
            diff_result = subprocess.run(
                ['diff', '-u', exp_path, act_path],
                capture_output=True,
                text=True
            )
            diff_output = diff_result.stdout
        finally:
            os.unlink(exp_path)
            os.unlink(act_path)
        
        return False, f"FAILED\n   Actual output written to: {actual_path}\n   Diff:\n" + '\n'.join(f"   {line}" for line in diff_output.split('\n'))


def main():
    if len(sys.argv) < 2:
        print("Usage: shelltester.py <test_file|directory> [test_file|directory ...]")
        print("\nTest file format:")
        print("---")
        print("run: <shell command>")
        print("ignoreStderr: true|false (optional, default: false)")
        print("---")
        print("<expected output>")
        sys.exit(1)
    
    # Collect all test files from arguments (files and directories)
    test_files = collect_test_files(sys.argv[1:])
    
    if not test_files:
        print("No test files found")
        sys.exit(1)
    
    passed = 0
    failed = 0
    
    for filepath in test_files:
        # Skip files that don't look like test files (no --- header)
        try:
            with open(filepath, 'r') as f:
                first_line = f.readline().strip()
                if not first_line.startswith('---'):
                    continue
        except:
            continue
        
        success, message = run_single_test(filepath)
        
        if success:
            print(f"✅ {filepath}: PASSED")
            passed += 1
        else:
            print(f"❌ {filepath}: {message}")
            failed += 1
    
    print(f"\nResults: {passed} passed, {failed} failed out of {passed + failed} tests")
    sys.exit(0 if failed == 0 else 1)


if __name__ == '__main__':
    main()
