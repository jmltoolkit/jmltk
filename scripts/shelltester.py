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
import shutil
from pathlib import Path


def setup_utf8_stdio() -> None:
    """Accept Unicode on stdout/stderr regardless of the console encoding.

    The Windows console (and piped output) defaults to the ANSI codepage
    (e.g. cp1252), which cannot encode the emoji/checkmark markers used in
    the output and would raise UnicodeEncodeError.
    """
    for stream in (sys.stdout, sys.stderr):
        try:
            stream.reconfigure(encoding='utf-8', errors='replace')
        except (AttributeError, OSError, ValueError):
            pass


def resolve_windows_executable(command: str) -> str:
    """Give cmd.exe the explicit executable name on Windows.

    The test files invoke the installed CLI as ``bin/jmltk`` (the POSIX start
    script name). installDist also generates ``bin/jmltk.bat`` on Windows, and
    that is the variant cmd.exe can execute. A plain ``shutil.which`` lookup is
    not enough here: it returns the bare ``bin/jmltk`` file because that file
    exists too, after which cmd.exe fails to run a shell script without stdout.
    """
    if os.name != 'nt':
        return command
    head, sep, tail = command.partition(' ')
    # Prefer the platform start scripts over the bare POSIX script.
    for ext in ('.bat', '.cmd', '.exe'):
        resolved = shutil.which(os.path.normpath(head + ext))
        if resolved is not None:
            return resolved + (sep + tail if sep else '')
    return command


def parse_test_file(filepath: str) -> tuple[list[str], str, bool]:
    """
    Parse a test file with YAML-like frontmatter.
    
    Returns:
        Tuple of (command_lines, expected_output, ignore_stderr)
    """
    with open(filepath, 'r', encoding='utf-8') as f:
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


def run_command(command_lines: list[str], ignore_stderr: bool = False) -> tuple[str, int, str]:
    """Execute the command and return (stdout, exit code, raw stderr)."""
    # Join command lines - each line after 'run:' could be arguments or continuation
    full_command = '\n'.join(command_lines)

    # Execute using shell (cmd.exe /c on Windows, /bin/sh elsewhere). Decode the
    # output as UTF-8: the CLI is a Java 21 process and the expected fixtures are
    # UTF-8, while the locale codec on Windows would be cp1252.
    result = subprocess.run(
        resolve_windows_executable(full_command),
        shell=True,
        capture_output=True,
        text=True,
        encoding='utf-8',
        errors='replace'
    )

    # Return stdout (and optionally stderr) for comparison
    output = result.stdout
    if not ignore_stderr and result.stderr:
        output += result.stderr

    return output.strip(), result.returncode, result.stderr.strip()


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
        actual, returncode, stderr = run_command(command_lines, ignore_stderr)
    except Exception as e:
        return False, f"Command execution failed: {e}"
    
    if compare_output(actual, expected):
        return True, "PASSED"
    else:
        # Write actual output to file beside the test file
        actual_path = filepath.with_suffix(filepath.suffix + ".actual")
        actual_path.write_text(actual, encoding='utf-8')
        
        # Use diff for better output visualization
        with tempfile.NamedTemporaryFile(mode='w', suffix='.expected', encoding='utf-8', delete=False) as exp_file:
            exp_file.write(expected)
            exp_path = exp_file.name
        with tempfile.NamedTemporaryFile(mode='w', suffix='.actual', encoding='utf-8', delete=False) as act_file:
            act_file.write(actual)
            act_path = act_file.name
        
        diff_output = ""
        try:
            diff_result = subprocess.run(
                ['diff', '-u', exp_path, act_path],
                capture_output=True,
                text=True,
                encoding='utf-8',
                errors='replace'
            )
            diff_output = diff_result.stdout
        finally:
            os.unlink(exp_path)
            os.unlink(act_path)
        
        message = f"FAILED (exit code {returncode})"
        if stderr:
            message += "\n" + '\n'.join(f"   stderr: {line}" for line in stderr.split('\n'))
        message += f"\n   Actual output written to: {actual_path}\n   Diff:\n" + '\n'.join(f"   {line}" for line in diff_output.split('\n'))
        return False, message


def main():
    setup_utf8_stdio()
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
            with open(filepath, 'r', encoding='utf-8') as f:
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
