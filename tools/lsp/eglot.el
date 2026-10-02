;(require 'eglot)

(setq path
      "/home/weigl/work/javaparser/tools/cli/build/install/jmltk/bin/jmltk-lsp")

(require 'cc-mode)
(define-derived-mode key-mode prog-mode "KEY"
  "Major mode for editing KEY"
  :syntax-table java-mode-syntax-table
  (setq-local comment-start "//")
  (setq-local comment-end "")
  (setq-local indent-tabs-mode nil))
(add-to-list 'auto-mode-alist '("\\.key\\'" . key-mode))

;(add-hook 'key-mode-hook 'eglot-ensure)
;(add-to-list 'eglot-server-programs
;             `(key-mode ,path "--stdio"))

(use-package lsp-mode
  :ensure t
  :init (setq lsp-keymap-prefix "C-c l")
  :commands lsp)


(lsp-register-client
 (make-lsp-client
  :new-connection (lsp-stdio-connection (list path "--stdio"))
  :major-modes '(java-mode)
  :server-id 'key-lsp))

(use-package lsp-ui :ensure t :commands lsp-ui-mode)
(setq lsp-print-io t)

(use-package lsp-treemacs :ensure t)


(defun --key-hook ()
  (lsp)
  (lsp-semantic-tokens-mode)
  (lsp-ui-doc-enable t)
  (lsp-modeline-diagnostics-mode)
  (lsp-diagnostics-mode)
  (lsp-modeline-code-actions-mode)
  (lsp-modeline-workspace-status-mode)
  (lsp-ui-sideline))

(add-hook 'key-mode-hook '--key-hook)


(with-eval-after-load 'lsp-mode
  ;; :global/:workspace/:file
  (setq lsp-modeline-diagnostics-scope :workspace))
