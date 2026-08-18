#!/usr/bin/env python3
import argparse
import json
import subprocess
import sys
import threading
import time

def main():
    cmd = ("/home/weigl/work/javaparser/tools/cli/build/install/jmltk/bin/jmltk-lsp","--stdio")
    messages= [
    r'''
        {"jsonrpc":"2.0","id":1,"method":"initialize",
        "params":{"processId":44629,
        "clientInfo":{"name":"Eglot","version":"1.17.30"},
            "rootPath":"/home/weigl/work/emacs-lsp-jml/examples/",
            "rootUri":"file:///home/weigl/work/emacs-lsp-jml/examples",
            "initializationOptions":{},
            "workspaceFolders":[{
            "uri":"file:///home/weigl/work/emacs-lsp-jml/examples",

            "name":"/home/weigl/work/emacs-lsp-jml/examples/"}]}}''',
    r'{"jsonrpc":"2.0","method":"initialized","params":{}}',
    #r'{"jsonrpc":"2.0","method":"textDocument/didOpen","params":{"textDocument":{"uri":"file:///home/weigl/work/emacs-lsp-jml/examples/Stack.java","version":0,"languageId":"java","text":""}}}',
    #r'{"jsonrpc":"2.0","method":"workspace/didChangeConfiguration","params":{"settings":{}}}',
    #r'{"jsonrpc":"2.0","id":2,"method":"textDocument/hover","params":{"textDocument":{"uri":"file:///home/weigl/work/emacs-lsp-jml/examples/Stack.java"},"position":{"line":0,"character":0}}}',
    r'{"jsonrpc":"2.0","id":4,"method":"textDocument/hover","params":{"textDocument":{"uri":"file:///home/weigl/work/emacs-lsp-jml/examples/Stack.java"},"position":{"line":16,"character":12}}}',
    r'{"jsonrpc":"2.0","id":8,"method":"shutdown","params":null}',
    r'{"jsonrpc":"2.0","method":"exit","params":null}',
    ]

    proc = subprocess.Popen(
        cmd,
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        bufsize=0,
    )

    def consumeStdout():
        for line in iter(proc.stdout.readline, b""):
            sys.stdout.write("[stdout] " + line.decode(errors="replace"))

    def consumeStderr():
        for line in iter(proc.stderr.readline, b""):
            sys.stderr.write("[stderr] " + line.decode(errors="replace"))

    threading.Thread(target=consumeStdout, daemon=True).start()
    threading.Thread(target=consumeStderr, daemon=True).start()


    for msg in messages:
        proc.stdin.write(f"Content-Length: {len(msg)}\r\n\r\n{msg}".encode())
        print(f">>> {msg}")
        time.sleep(0.05)  # small pacing delay; helps servers that assume ordered delivery

    time.sleep(10)



if __name__ == "__main__":
    main()
