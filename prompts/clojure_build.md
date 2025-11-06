# Clojure MCP Agent Guide

**Purpose**: Enable LLM agents to effectively use Clojure MCP tools for development.

## Quick Start

### The Golden Rule: REPL → Validate → Save

1. **Prototype in REPL** with `clojure_eval`
2. **Validate** with test data
3. **Save** using `clojure_edit`
4. **Verify** by reloading

**Example**:
```clojure
(require '[myapp.core :reload])
(in-ns 'myapp.core)

(defn validate-email [email]
  (re-matches #".+@.+" email))

(validate-email "test@example.com")  ; => "test@example.com"
(validate-email "invalid")            ; => nil
```

After validating in REPL, save:
```clojure
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "defn",
  form_identifier: "validate-email",
  operation: "replace",
  content: "(defn validate-email [email] (re-matches #\".+@.+\" email))")
```

## Core Tools

### Reading Code

- **`clojure_read_file`** - Smart Clojure reader, collapsed view by default
  - `read_file(path: "src/myapp/core.clj")` - See function signatures only
  - `read_file(path: "src/myapp/core.clj", name_pattern: "validate.*")` - Expand specific functions
  - `read_file(path: "src/myapp/core.clj", content_pattern: "try|catch")` - Expand functions with pattern

- **`clojure_grep`** - Search file contents
  - Find: `grep(pattern: "defn validate-", include: "*.clj")`
  - Errors: `grep(pattern: "try|catch|throw", include: "src/**/*.clj")`

- **`clojure_glob_files`** - Find files by pattern
  - Tests: `glob_files(pattern: "**/*_test.clj")`
  - All Clojure: `glob_files(pattern: "src/**/*.clj")`

- **`clojure_inspect_project`** - Project structure, dependencies, namespaces

### Editing Code (ALWAYS use `clojure_edit` for Clojure files)

- **`clojure_edit`** ⭐ PRIMARY TOOL - Matches by form type + identifier (not brittle text)
  - Operations: `replace`, `insert_before`, `insert_after`
  - Validates syntax, balances parentheses, provides linting feedback

**Examples**:
```clojure
; Replace function
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "defn",
  form_identifier: "validate-email",
  operation: "replace",
  content: "(defn validate-email [email] (re-matches #\".+@.+\\..+\" email))")

; Insert before another function
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "defn",
  form_identifier: "process-order",
  operation: "insert_before",
  content: "(defn- calculate-tax [amount] (* amount 0.08))")

; Update namespace
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "ns",
  form_identifier: "myapp.core",
  operation: "replace",
  content: "(ns myapp.core (:require [clojure.string :as str] [clojure.java.io :as io]))")

; Edit defmethod (include dispatch value!)
clojure_edit(
  file_path: "src/myapp/shapes.clj",
  form_type: "defmethod",
  form_identifier: "area :rectangle",
  operation: "replace",
  content: "(defmethod area :rectangle [{:keys [width height]}] (* width height))")
```

**Special cases**:
- For `defmethod`: Include dispatch value in identifier: `"area :rectangle"`
- For qualified: `"shape/area :circle"`
- For vector dispatch: `"convert [:feet :inches]"`

- **`clojure_edit_replace_sexp`** - Edit expressions within functions (syntax-aware, ignores whitespace)
  ```clojure
  ; Rename symbol throughout
  clojure_edit_replace_sexp(
    file_path: "src/myapp/core.clj",
    match_form: "old-name",
    new_form: "new-name",
    replace_all: true)

  ; Change calculation
  clojure_edit_replace_sexp(
    file_path: "src/myapp/core.clj",
    match_form: "(+ x 2)",
    new_form: "(* x 2)")

  ; Wrap in try-catch
  clojure_edit_replace_sexp(
    file_path: "src/myapp/core.clj",
    match_form: "(risky-operation)",
    new_form: "(try (risky-operation) (catch Exception e (log/error e)))")
  ```

- **`file_edit`** - Text-based (for markdown, configs, or very simple edits)

### Testing & Validation

- **`clojure_eval`** ⭐ YOUR MOST IMPORTANT TOOL - REPL evaluation

**Critical rules**:
1. Always reload with `:reload`
2. Switch to namespace with `in-ns`
3. Validate before saving

```clojure
clojure_eval(code: """
(require '[myapp.core :reload])
(in-ns 'myapp.core)

(def test-order {:items [{:price 10} {:price 20}]})
(defn calculate-total [order]
  (reduce + (map :price (:items order))))

(calculate-total test-order)    ; => 30
(calculate-total {:items []})   ; => 0
""")
```

**Built-in helpers** (auto-loaded):
```clojure
(clj-mcp.repl-tools/list-ns)                    ; list namespaces
(clj-mcp.repl-tools/list-vars 'clojure.string)  ; functions in ns
(clj-mcp.repl-tools/doc-symbol 'map)            ; documentation
(clj-mcp.repl-tools/source-symbol 'map)         ; source code
(clj-mcp.repl-tools/find-symbols "seq")         ; find symbols
(clj-mcp.repl-tools/complete "clojure.string/j") ; completions
```

- **`clojure_bash`** - Shell commands: `bash(command: "bb test")`

### Planning & Debugging

- **`clojure_scratch_pad`** - Task tracking and state management
  ```clojure
  scratch_pad(op: "set_path", path: ["tasks"], value: [
    {task: "Read code", done: false, priority: "high"},
    {task: "Design", done: false, priority: "high"}
  ], explanation: "Planning")

  scratch_pad(op: "set_path", path: ["tasks", 0, "done"], value: true, explanation: "Done")
  scratch_pad(op: "get_path", path: ["tasks"], explanation: "Status check")
  ```

- **`clojure_think`** - Log reasoning about complex decisions

## Decision Trees

### When to Edit

```
Editing Clojure file? → Top-level form? → YES → Use clojure_edit ⭐
                                       → NO  → Expression? → YES → Use clojure_edit_replace_sexp
                                                            → NO  → Use file_edit
                      → NOT Clojure → Replacing >50%? → YES → Use file_write
                                                      → NO  → Use file_edit
```

### When to Read

```
Need project overview? → clojure_inspect_project
Find files by name?    → glob_files
Find files by content? → grep
Read specific file?    → Clojure? → First time? → read_file (collapsed)
                                 → Need patterns? → read_file (with pattern)
                       → Non-Clojure → read_file
Unsure where to look?  → dispatch_agent
```

### When to Use REPL

**ALWAYS use REPL BEFORE editing files for**:
- New functions - prototype first
- Logic changes - test first
- Refactoring - verify first
- Bug fixes - reproduce and fix first
- Data exploration - inspect first

## Workflows

### Workflow 1: Add New Feature

```clojure
; 1. Plan
scratch_pad(op: "set_path", path: ["tasks"], value: [
  {task: "Understand existing code", done: false, priority: "high"},
  {task: "Design in REPL", done: false, priority: "high"},
  {task: "Implement", done: false, priority: "high"},
  {task: "Test", done: false, priority: "high"}
], explanation: "Planning")

; 2. Understand existing code
clojure_inspect_project(explanation: "Project structure")
read_file(path: "src/myapp/core.clj")
read_file(path: "src/myapp/core.clj", name_pattern: "related-.*")
scratch_pad(op: "set_path", path: ["tasks", 0, "done"], value: true, explanation: "Done")

; 3. Design in REPL
clojure_eval(code: """
(require '[myapp.core :reload])
(in-ns 'myapp.core)

(def test-data {:email "user@example.com" :name "Test"})

(defn welcome-user [user]
  (if-let [name (:name user)]
    (str "Welcome, " name "!")
    "Welcome, Guest!"))

(welcome-user test-data)        ; => "Welcome, Test!"
(welcome-user {:name nil})      ; => "Welcome, Guest!"
(welcome-user {})               ; => "Welcome, Guest!"
""")

scratch_pad(op: "set_path", path: ["tasks", 1, "done"], value: true, explanation: "Designed")

; 4. Save to file
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "defn",
  form_identifier: "existing-function",
  operation: "insert_after",
  content: "(defn welcome-user [user] (if-let [name (:name user)] (str \"Welcome, \" name \"!\") \"Welcome, Guest!\"))")

; Verify saved code
clojure_eval(code: """
(require '[myapp.core :reload])
(welcome-user {:name "Alice"})
""")

scratch_pad(op: "set_path", path: ["tasks", 2, "done"], value: true, explanation: "Saved")

; 5. Add tests
clojure_edit(
  file_path: "test/myapp/core_test.clj",
  form_type: "deftest",
  form_identifier: "existing-test",
  operation: "insert_after",
  content: "(deftest welcome-user-test (is (= \"Welcome, Alice!\" (welcome-user {:name \"Alice\"}))) (is (= \"Welcome, Guest!\" (welcome-user {}))))")

; 6. Run tests
bash(command: "bb test")
scratch_pad(op: "set_path", path: ["tasks", 3, "done"], value: true, explanation: "All done")
```

### Workflow 2: Debug a Problem

```clojure
; 1. Reproduce in REPL
clojure_eval(code: """
(require '[myapp.core :reload])
(in-ns 'myapp.core)

(try
  (validate-email "invalid-email")
  (catch Exception e {:error (.getMessage e)}))
""")

; 2. Inspect intermediate values
clojure_eval(code: """
(let [email "invalid-email"]
  {:input email
   :type (type email)
   :contains-at? (clojure.string/includes? email "@")})
""")

; 3. Analyze problem
think(thought: "Email missing '@'. Current regex requires '@' and '.'. Should return boolean instead of throwing.")

; 4. Test fix in REPL
clojure_eval(code: """
(defn validate-email [email]
  (and (string? email)
       (not (empty? email))
       (clojure.string/includes? email "@")
       (some? (re-matches #".+@.+\\..+" email))))

(validate-email "valid@example.com")    ; => true
(validate-email "invalid-email")        ; => false
(validate-email nil)                    ; => false
""")

; 5. Save fix
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "defn",
  form_identifier: "validate-email",
  operation: "replace",
  content: "(defn validate-email [email] (and (string? email) (not (empty? email)) (clojure.string/includes? email \"@\") (some? (re-matches #\".+@.+\\\\..+\" email))))")

; 6. Verify
clojure_eval(code: """
(require '[myapp.core :reload])
(validate-email "invalid-email")    ; => false now
(validate-email "valid@example.com") ; => true
""")

; 7. Add regression test
clojure_edit(
  file_path: "test/myapp/core_test.clj",
  form_type: "deftest",
  form_identifier: "validate-email-test",
  operation: "replace",
  content: "(deftest validate-email-test (is (true? (validate-email \"user@example.com\"))) (is (false? (validate-email \"invalid-email\"))) (is (false? (validate-email nil))))")

; 8. Run tests
bash(command: "bb test")
```

### Workflow 3: Test-Driven Development (TDD)

```clojure
; 1. Write test first
clojure_eval(code: """
(require '[clojure.test :as t])
(in-ns 'myapp.core-test)

(t/deftest welcome-user-test
  (t/is (= "Welcome, Alice!" (welcome-user {:name "Alice"})))
  (t/is (= "Welcome, Guest!" (welcome-user {}))))
""")

; 2. Run test (RED - fails, expected)
clojure_eval(code: "(require '[kaocha.repl :as k]) (k/run 'myapp.core-test/welcome-user-test)")

; 3. Implement to pass (GREEN)
clojure_eval(code: """
(require '[myapp.core :reload])
(in-ns 'myapp.core)

(defn welcome-user [user]
  (if-let [name (:name user)]
    (str "Welcome, " name "!")
    "Welcome, Guest!"))

(require '[kaocha.repl :as k])
(k/run 'myapp.core-test/welcome-user-test)  ; Now passes
""")

; 4. Save implementation
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "defn",
  form_identifier: "existing-function",
  operation: "insert_after",
  content: "(defn welcome-user [user] (if-let [name (:name user)] (str \"Welcome, \" name \"!\") \"Welcome, Guest!\"))")

; 5. Verify all tests pass
clojure_eval(code: "(require '[kaocha.repl :as k]) (k/run 'myapp.core-test)")

; 6. Refactor if needed (keep tests passing)
```

### Workflow 4: Refactor Code

```clojure
; 1. Read current implementation
read_file(path: "src/myapp/core.clj", name_pattern: "process-order")

; 2. Understand usage
grep(pattern: "process-order", include: "**/*.clj")

; 3. Design in REPL
clojure_eval(code: """
(require '[myapp.core :reload])
(in-ns 'myapp.core)

(def test-order {:items [{:price 10} {:price 20}]})

; Current implementation
(defn process-order-old [order]
  (let [items (:items order)
        subtotal (reduce + (map :price items))
        tax (* subtotal 0.08)
        total (+ subtotal tax)]
    (assoc order :total total)))

; New modular implementation
(defn- calculate-subtotal [items]
  (reduce + (map :price items)))

(defn- calculate-tax [subtotal]
  (* subtotal 0.08))

(defn process-order-new [order]
  (let [subtotal (calculate-subtotal (:items order))
        tax (calculate-tax subtotal)]
    (assoc order :subtotal subtotal :tax tax :total (+ subtotal tax))))

; Verify same result
(process-order-old test-order)
(process-order-new test-order)
""")

; 4. Add helpers
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "defn",
  form_identifier: "process-order",
  operation: "insert_before",
  content: "(defn- calculate-subtotal [items] (reduce + (map :price items))) (defn- calculate-tax [subtotal] (* subtotal 0.08))")

; 5. Update main function
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "defn",
  form_identifier: "process-order",
  operation: "replace",
  content: "(defn process-order [order] (let [subtotal (calculate-subtotal (:items order)) tax (calculate-tax subtotal)] (assoc order :subtotal subtotal :tax tax :total (+ subtotal tax))))")

; 6. Verify
clojure_eval(code: """
(require '[myapp.core :reload])
(process-order {:items [{:price 10} {:price 20}]})
""")

; 7. Run tests
bash(command: "bb test")
```

## Debugging with scope-capture

**scope-capture** captures exact runtime context (all local variables) for later inspection in REPL.

### The Three Main Functions

1. **`sc.api/spy`** - Capture a moment, wrap expression:
```clojure
(defn my-function [x y z]
  (let [a (+ x y) b (* a z)]
    (sc.api/spy b)))  ; Wraps and captures when executed
```

2. **`sc.api/defsc`** - Restore as globals:
```clojure
(sc.api/defsc 7)  ; ep-id from spy output
a  ; => captured value
b  ; => captured value
```

3. **`sc.api/letsc`** - Restore as locals:
```clojure
(sc.api/letsc 7 (+ a b x))  ; Uses captured scope
```

### Complete Debugging Workflow

```clojure
; 1. Add spy to suspicious code
clojure_edit(
  file_path: "src/myapp/queries.clj",
  form_type: "defn",
  form_identifier: "process-user",
  operation: "replace",
  content: "(defn process-user [db user-id] (let [user (get-user db user-id) enriched (enrich-with-metadata db user) result (apply-business-rules enriched)] (sc.api/spy result)))")

; 2. Run code to capture context
clojure_eval(code: """
(require '[myapp.queries])
(process-user (:db system) 12345)
; Output: SPY [23 -5] ...
; ep-id is 23
""")

; 3. Restore context in REPL
clojure_eval(code: """
(require 'sc.api)
(sc.api/defsc 23)  ; Restore with ep-id

; Now inspect captured data
user      ; see actual user
enriched  ; see enriched result
db        ; have actual DB connection

; Test hypothesis about bug
(apply-business-rules (assoc enriched :status "active"))
(enrich-with-metadata db user)  ; test intermediate
""")

; 4. Implement fix once understood
clojure_edit(...)

; 5. Clean up
clojure_edit(...)  ; Remove spy
clojure_eval(code: "(sc.api/undefsc)")  ; Clean up globals
```

### Key Points

- **Installation**: Already in dev deps, `(require 'sc.api)`
- **Main use**: Capture runtime context, replay in REPL
- **Best for**: Complex bugs depending on runtime state

## Common Mistakes

### ❌ Mistake 1: Editing Before Testing

**Wrong**: Edit file immediately without REPL validation
```clojure
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "defn",
  form_identifier: "new-function",
  operation: "replace",
  content: "(defn new-function [x] (+ x 1))")  ; Hope it works!
```

**Right**: Test in REPL first
```clojure
clojure_eval(code: """
(require '[myapp.core :reload])
(in-ns 'myapp.core)
(defn new-function [x] (+ x 1))
(new-function 5)   ; => 6
(new-function -1)  ; => 0
""")
clojure_edit(...)  ; THEN edit file
```

**Why**: Catches syntax errors and logic bugs instantly before saving.

### ❌ Mistake 2: Forgetting to Reload

**Wrong**: Editing file, then testing without reload
```clojure
clojure_eval(code: "(in-ns 'myapp.core)")
clojure_eval(code: "(test-function)")  ; Testing OLD code!
```

**Right**: Always reload when editing
```clojure
clojure_eval(code: """
(require '[myapp.core :reload])
(in-ns 'myapp.core)
(test-function)
""")
```

**Why**: Avoids testing old code and thinking changes don't work.

### ❌ Mistake 3: Using file_edit for Clojure

**Wrong**: Text-based edit for .clj file
```clojure
file_edit(
  file_path: "src/myapp/core.clj",
  old_string: "(defn old-name ...)",
  new_string: "(defn new-name ...)")
```

**Right**: Use structural tool
```clojure
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "defn",
  form_identifier: "old-name",
  operation: "replace",
  content: "(defn new-name ...)")
```

**Why**: Structural matching is reliable; text matching is brittle. clojure_edit validates syntax.

### ❌ Mistake 4: Not Handling Reload Failures

**Wrong**: Reload failure silently continues
```clojure
(require '[myapp.core :reload])  ; Syntax error? File error? Keep going!
(my-function)
```

**Right**: Check reload success
```clojure
clojure_eval(code: """
(require '[myapp.core :reload])
(in-ns 'myapp.core)  ; Fails if reload failed
(my-function)
""")
```

**Why**: Prevents debugging with broken code.

### ❌ Mistake 5: Incomplete REPL Validation

**Wrong**: Test only happy path
```clojure
clojure_eval(code: """
(defn divide [x y] (/ x y))
(divide 10 2)  ; => 5 ✓
""")
```

**Right**: Test edge cases
```clojure
clojure_eval(code: """
(defn divide [x y] (/ x y))
(divide 10 2)      ; => 5
(divide 10 0)      ; Throws - need error handling!
(divide 0 10)      ; => 0
(divide nil 5)     ; Throws - need nil check!
""")
```

**Why**: Saves fixing bugs after save.

### ❌ Mistake 6: Inefficient Tool Selection

**Wrong**: Using dispatch_agent for simple file lookup
```clojure
task(prompt: "Find all test files")
```

**Right**: Use glob_files directly
```clojure
glob_files(pattern: "**/*_test.clj")
```

**Why**: Direct tools are faster and more token-efficient.

### ❌ Mistake 7: Forgetting defmethod Dispatch Values

**Wrong**: Missing dispatch value in identifier
```clojure
clojure_edit(
  file_path: "src/myapp/shapes.clj",
  form_type: "defmethod",
  form_identifier: "area",  ; ← Missing dispatch value!
  operation: "replace",
  content: "(defmethod area :circle [{:keys [radius]}] (* 3.14 radius radius))")
```

**Right**: Include dispatch value
```clojure
clojure_edit(
  file_path: "src/myapp/shapes.clj",
  form_type: "defmethod",
  form_identifier: "area :circle",  ; ← Dispatch value included
  operation: "replace",
  content: "(defmethod area :circle [{:keys [radius]}] (* 3.14 radius radius))")
```

**Why**: Tool must uniquely identify the specific method implementation.

## Quick Patterns

### Explore New File
```clojure
read_file(path: "src/myapp/module.clj")
read_file(path: "src/myapp/module.clj", name_pattern: "function-.*")
clojure_eval(code: """
(require '[myapp.module :reload])
(in-ns 'myapp.module)
; test functions
""")
```

### Add Function
```clojure
clojure_eval(code: """
(require '[myapp.core :reload])
(in-ns 'myapp.core)
(defn my-fn [x] (* x 2))
(my-fn 5)  ; Test
""")

clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "defn",
  form_identifier: "existing-fn",
  operation: "insert_after",
  content: "(defn my-fn [x] (* x 2))")

clojure_eval(code: "(require '[myapp.core :reload]) (my-fn 5)")
```

### Rename Symbol Across File
```clojure
clojure_edit_replace_sexp(
  file_path: "src/myapp/core.clj",
  match_form: "old-name",
  new_form: "new-name",
  replace_all: true)
```

### Update Namespace
```clojure
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "ns",
  form_identifier: "myapp.core",
  operation: "replace",
  content: "(ns myapp.core (:require [clojure.string :as str]))")
```

### Add Error Handling
```clojure
clojure_eval(code: """
(defn safe-fn [x]
  (try
    (risky-fn x)
    (catch Exception e
      (log/error e "Failed" {:input x})
      nil)))
(safe-fn "test")
""")

clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "defn",
  form_identifier: "risky-fn",
  operation: "replace",
  content: "(defn risky-fn [x] (try (. . .) (catch Exception e (log/error e \"Failed\" {:input x}) nil)))")
```

### Focus Tests During Development
```clojure
(require '[kaocha.repl :as k])

(k/run :unit
  {:kaocha/focus [".*my-feature.*"]
   :kaocha/fail-fast? true})

(k/run-all)  ; Full suite when done
```
