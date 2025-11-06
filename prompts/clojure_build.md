# Clojure MCP Agent Guide for LLM Assistants

**Purpose**: This guide enables LLM agents to effectively use the
Clojure MCP (Model Context Protocol) toolset for Clojure development.

---

## Quick Start (5 Minutes)

### The Golden Rule: REPL → Validate → Save

**ALWAYS follow this pattern**:

1. **Prototype in REPL** using `clojure_eval`
2. **Validate it works** with test data
3. **Save to file** using `clojure_edit`
4. **Verify** by reloading and testing

**Example**:
```clojure
// 1. Prototype
clojure_eval(code: """
(require '[myapp.core :reload])
(in-ns 'myapp.core)
(defn validate-email [email]
  (re-matches #\".+@.+\" email))
(validate-email "test@example.com")  ; => "test@example.com"
(validate-email "invalid")            ; => nil
""")

// 2. Save (only after validation)
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "defn",
  form_identifier: "validate-email",
  operation: "replace",
  content: "(defn validate-email [email] (re-matches #\".+@.+\" email))")
```

### Essential Tool Selection

```
┌─────────────────────────────────────────────────────────┐
│ READING CODE                                            │
├─────────────────────────────────────────────────────────┤
│ clojure-mcp_read_file     → Read Clojure files         │
│ clojure-mcp_grep          → Search file contents       │
│ clojure-mcp_glob_files    → Find files by pattern      │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ EDITING CODE                                            │
├─────────────────────────────────────────────────────────┤
│ clojure-mcp_clojure_edit  → Edit top-level forms       │
│   ↑ PREFER THIS FOR ALL CLOJURE EDITS                  │
│ clojure-mcp_clojure_edit_replace_sexp → Edit within fn │
│ clojure-mcp_file_edit     → Fallback for non-Clojure   │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ TESTING & VALIDATION                                    │
├─────────────────────────────────────────────────────────┤
│ clojure-mcp_clojure_eval  → REPL evaluation            │
│   ↑ USE THIS CONSTANTLY                                │
│ clojure-mcp_bash          → Run tests (bb test)        │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ PLANNING & STATE                                        │
├─────────────────────────────────────────────────────────┤
│ clojure-mcp_scratch_pad   → Track tasks & state        │
│ clojure-mcp_think         → Log reasoning               │
└─────────────────────────────────────────────────────────┘
```

---

## Table of Contents

1. [Core Philosophy](#core-philosophy)
2. [Tool Reference](#tool-reference)
3. [Decision Trees](#decision-trees)
4. [Complete Workflows](#complete-workflows)
5. [Example Bank](#example-bank)
6. [Common Mistakes](#common-mistakes)

---

## Core Philosophy

### Why REPL-First Development Matters

**Traditional Approach** (DON'T):
```
Write code → Save to file → Run tests → Debug → Repeat
❌ Slow feedback loop
❌ Many iterations needed
❌ Syntax errors caught late
```

**REPL-First Approach** (DO):
```
Try in REPL → Validate immediately → Refine → Save working code
✅ Instant feedback
✅ Fewer iterations
✅ Syntax errors caught immediately
✅ Test edge cases before committing
```

### The Three Principles

1. **Tiny Steps with Rich Feedback** - Each step should be validated before proceeding
2. **REPL as Source of Truth** - If it works in REPL, then save it
3. **Structure-Aware Editing** - Use tools that understand Clojure syntax

---

## Tool Reference

### Read Tools

#### `clojure-mcp_read_file` - Smart Clojure File Reader

**Default behavior**: Collapsed view (shows only function signatures)

```clojure
// Quick overview
read_file(path: "src/myapp/core.clj")
// Shows:
// (defn validate-email ...)
// (defn process-order ...)
// (defn calculate-tax ...)

// Expand specific functions
read_file(path: "src/myapp/core.clj", name_pattern: "validate.*")
// Shows full implementation of validate-* functions

// Find functions with specific content
read_file(path: "src/myapp/core.clj", content_pattern: "try|catch")
// Shows full implementation of functions containing try or catch
```

**When to use**:
- ✅ First step exploring unfamiliar code
- ✅ Finding specific functions
- ✅ Understanding file structure

**⚠️ Common mistake**: Reading with `collapsed: false` immediately
- Use collapsed view first to understand structure
- Then expand specific functions

#### `clojure-mcp_grep` - Content Search

```clojure
// Find function definitions
grep(pattern: "defn validate-", include: "*.clj")

// Find error handling
grep(pattern: "try|catch|throw", include: "src/**/*.clj")

// Find TODO comments
grep(pattern: "TODO|FIXME")
```

**When to use**:
- ✅ Finding files containing specific patterns
- ✅ Locating function calls across codebase
- ✅ Searching for TODO/FIXME comments

#### `clojure-mcp_glob_files` - File Pattern Matching

```clojure
// Find test files
glob_files(pattern: "**/*_test.clj")

// Find all ClojureScript files
glob_files(pattern: "src/**/*.cljs")
```

**When to use**:
- ✅ Finding files by name pattern
- ✅ Locating test files
- ✅ Finding all files of specific type

#### `clojure-mcp_clojure_inspect_project` - Project Analysis

```clojure
clojure_inspect_project(explanation: "Understanding project structure")
```

**Returns**:
- Project dependencies
- Source paths
- Namespaces
- Environment info

**When to use**:
- ✅ Starting work on unfamiliar project
- ✅ Understanding project organization
- ✅ Finding available dependencies

---

### ✏️ Edit Tools

#### `clojure-mcp_clojure_edit` - Primary Editing Tool

**⭐ ALWAYS PREFER THIS FOR CLOJURE FILES**

**Why it's better**:
- Matches by form type + identifier (not brittle text matching)
- Built-in syntax validation
- Automatic parenthesis balancing
- Provides linting feedback

**Operations**:
- `replace` - Replace entire form
- `insert_before` - Add new form before target
- `insert_after` - Add new form after target

**Examples**:

```clojure
// Replace a function
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "defn",
  form_identifier: "validate-email",
  operation: "replace",
  content: "(defn validate-email
  \"Validates email format.\"
  [email]
  (and (string? email)
       (re-matches #\".+@.+\\..+\" email)))")

// Add helper function before another function
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "defn",
  form_identifier: "process-order",
  operation: "insert_before",
  content: "(defn- calculate-tax [amount] (* amount 0.08))")

// Update namespace declaration
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "ns",
  form_identifier: "myapp.core",
  operation: "replace",
  content: "(ns myapp.core
  (:require [clojure.string :as str]
            [clojure.java.io :as io]))")

// Edit defmethod (include dispatch value!)
clojure_edit(
  file_path: "src/myapp/shapes.clj",
  form_type: "defmethod",
  form_identifier: "area :rectangle",  // ← dispatch value required
  operation: "replace",
  content: "(defmethod area :rectangle [{:keys [width height]}]
  (* width height))")

// Edit namespace-qualified defmethod
clojure_edit(
  file_path: "src/myapp/shapes.clj",
  form_type: "defmethod",
  form_identifier: "shape/area :circle",  // ← namespace + dispatch value
  operation: "replace",
  content: "(defmethod shape/area :circle [{:keys [radius]}]
  (* Math/PI radius radius))")
```

**⚠️ Special cases**:

For `defmethod` forms:
- Include dispatch value in `form_identifier`: `"area :rectangle"`
- Include namespace if qualified: `"shape/area :circle"`
- For vector dispatch: `"convert [:feet :inches]"`

#### `clojure-mcp_clojure_edit_replace_sexp` - Expression-Level Editing

**When to use**:
- Changing expressions within functions (not whole functions)
- Renaming symbols across file
- Wrapping code in try-catch
- Removing debug statements

**Key feature**: Syntax-aware matching (ignores whitespace)

```clojure
// Rename symbol throughout file
clojure_edit_replace_sexp(
  file_path: "src/myapp/core.clj",
  match_form: "old-name",
  new_form: "new-name",
  replace_all: true)

// Change calculation
clojure_edit_replace_sexp(
  file_path: "src/myapp/core.clj",
  match_form: "(+ x 2)",
  new_form: "(* x 2)")

// Wrap in try-catch
clojure_edit_replace_sexp(
  file_path: "src/myapp/core.clj",
  match_form: "(risky-operation)",
  new_form: "(try
  (risky-operation)
  (catch Exception e
    (log/error e)))")

// Remove debug println
clojure_edit_replace_sexp(
  file_path: "src/myapp/core.clj",
  match_form: "(println \"Debug:\" x)",
  new_form: "")
```

**⚠️ Requirements**:
- `match_form` must be complete Clojure expression(s)
- Both forms must be valid, parseable Clojure
- Incomplete forms like `(defn foo [x]` will error

#### `clojure-mcp_file_edit` - Text-Based Editing

**When to use**:
- ❌ NOT for Clojure files (use clojure_edit instead)
- ✅ Markdown, config files, documentation
- ✅ Very simple edits where structural tools are overkill

```clojure
file_edit(
  file_path: "README.md",
  old_string: "## Old Section\nOld content here",
  new_string: "## New Section\nNew content here")
```

**⚠️ Constraints**:
- Must match text EXACTLY (including whitespace)
- Old string must appear exactly once in file

---

### 🧪 Test & Validation Tools

#### `clojure-mcp_clojure_eval` - REPL Evaluation

**⭐ YOUR MOST IMPORTANT TOOL**

**Critical rules**:
1. Always reload with `:reload`
2. Switch to working namespace with `in-ns`
3. Validate before saving to files

```clojure
// Basic pattern - ALWAYS start with this
clojure_eval(code: """
(require '[myapp.core :reload])
(in-ns 'myapp.core)
""")

// Test a function
clojure_eval(code: "(validate-email \"test@example.com\")")

// Multi-step exploration
clojure_eval(code: """
(require '[myapp.core :reload])
(in-ns 'myapp.core)

;; Define test data
(def test-order {:items [{:price 10} {:price 20}]})

;; Prototype function
(defn calculate-total [order]
  (reduce + (map :price (:items order))))

;; Test it
(calculate-total test-order)  ; => 30

;; Test edge cases
(calculate-total {:items []})  ; => 0
(calculate-total {:items [{:price 5}]})  ; => 5
""")
```

**Built-in helper functions**:

```clojure
// Explore namespaces
clojure_eval(code: "(clj-mcp.repl-tools/list-ns)")

// List functions in namespace
clojure_eval(code: "(clj-mcp.repl-tools/list-vars 'clojure.string)")

// Show documentation
clojure_eval(code: "(clj-mcp.repl-tools/doc-symbol 'map)")

// Show source code
clojure_eval(code: "(clj-mcp.repl-tools/source-symbol 'map)")

// Find symbols
clojure_eval(code: "(clj-mcp.repl-tools/find-symbols \"seq\")")

// Completions
clojure_eval(code: "(clj-mcp.repl-tools/complete \"clojure.string/j\")")
```

**⚠️ Common mistakes**:

❌ Forgetting to reload:
```clojure
clojure_eval(code: "(in-ns 'myapp.core)")
clojure_eval(code: "(test-function)")  // Using OLD code!
```

✅ Always reload:
```clojure
clojure_eval(code: """
(require '[myapp.core :reload])
(in-ns 'myapp.core)
(test-function)
""")
```

#### `clojure-mcp_bash` - Shell Commands

```clojure
// Run tests
bash(command: "bb test")

// Check git status
bash(command: "git status")

// Run specific test namespace
bash(command: "bb test :unit")
```

**⚠️ Note**: Output truncated at 8500 characters

---

### 🗂️ Planning & State Tools

#### `clojure-mcp_scratch_pad` - Task Tracking & State

**⭐ USE THIS FOR ALL MULTI-STEP TASKS**

**Recommended task schema**:
```clojure
{
  task: "Description",
  done: false,
  priority: "high|medium|low",
  context: "Additional info"
}
```

**Complete example**:

```clojure
// 1. Create task list
scratch_pad(
  op: "set_path",
  path: ["tasks"],
  value: [
    {task: "Read existing code", done: false, priority: "high"},
    {task: "Design solution in REPL", done: false, priority: "high"},
    {task: "Implement in files", done: false, priority: "high"},
    {task: "Add tests", done: false, priority: "medium"},
    {task: "Run tests", done: false, priority: "high"}
  ],
  explanation: "Planning feature implementation")

// 2. Work on first task...

// 3. Mark complete
scratch_pad(
  op: "set_path",
  path: ["tasks", 0, "done"],
  value: true,
  explanation: "Completed reading existing code")

// 4. Check progress
scratch_pad(
  op: "get_path",
  path: ["tasks"],
  explanation: "Checking task status")

// 5. Add new task
scratch_pad(
  op: "set_path",
  path: ["tasks", 5],
  value: {task: "Update documentation", done: false, priority: "low"},
  explanation: "Adding documentation task")
```

**Operations**:
- `set_path` - Store value at path
- `get_path` - Retrieve value from path
- `delete_path` - Remove value at path
- `inspect` - View structure with depth limit

#### `clojure-mcp_think` - Reasoning Log

**When to use**:
- Planning complex refactoring
- Analyzing bug causes
- Evaluating multiple approaches

```clojure
think(thought: "The current approach has issues:
1. Validation is scattered across multiple functions
2. No clear separation of concerns
3. Hard to test

I should:
1. Extract validation into separate namespace
2. Use spec for data validation
3. Create pure functions that are easy to test

I'll start by prototyping the validation namespace in the REPL.")
```

---

## Decision Trees

### When to Edit Code

```
┌─────────────────────────────────────────────┐
│ Are you editing a Clojure file (.clj)?      │
└─────┬───────────────────────────────────────┘
      │
      ├─ YES ─┬─ Editing top-level form? (defn, def, ns, defmethod)
      │       │
      │       ├─ YES → Use clojure_edit ⭐
      │       │
      │       └─ NO ─┬─ Editing expression within function?
      │              │
      │              ├─ YES → Use clojure_edit_replace_sexp
      │              │
      │              └─ NO → Use file_edit
      │
      └─ NO ─┬─ Replacing >50% of file?
             │
             ├─ YES → Use file_write
             │
             └─ NO → Use file_edit
```

### When to Read Code

```
┌─────────────────────────────────────────────┐
│ What are you trying to find?                │
└─────┬───────────────────────────────────────┘
      │
      ├─ "I need project overview"
      │  → clojure_inspect_project
      │
      ├─ "I need to find files by name"
      │  → glob_files
      │
      ├─ "I need to find files containing X"
      │  → grep
      │
      ├─ "I need to read a specific file"
      │  └─┬─ Clojure file?
      │    │
      │    ├─ YES ─┬─ First time reading?
      │    │       │  → read_file (collapsed view)
      │    │       │
      │    │       └─ Need specific functions?
      │    │          → read_file (with name_pattern)
      │    │
      │    └─ NO → read_file (collapsed: false)
      │
      └─ "I need to explore/search but not sure where"
         → dispatch_agent
```

### When to Use REPL

```
ALWAYS use REPL BEFORE editing files for:

✅ New functions         → Prototype in REPL first
✅ Changing logic        → Test in REPL first
✅ Refactoring          → Verify in REPL first
✅ Bug fixes            → Reproduce and fix in REPL first
✅ Exploring data       → Inspect in REPL first

REPL workflow:
1. (require '[namespace :reload])
2. (in-ns 'namespace)
3. Test your code
4. Validate edge cases
5. Only then save to file
```

---

## Complete Workflows

### Workflow 1: Adding a New Feature

```clojure
// ========================================
// STEP 1: Plan with scratch_pad
// ========================================
scratch_pad(
  op: "set_path",
  path: ["feature-tasks"],
  value: [
    {task: "Understand existing code", done: false, priority: "high"},
    {task: "Design in REPL", done: false, priority: "high"},
    {task: "Implement in files", done: false, priority: "high"},
    {task: "Add tests", done: false, priority: "high"},
    {task: "Validate", done: false, priority: "high"}
  ],
  explanation: "Planning new feature implementation")

// ========================================
// STEP 2: Understand existing code
// ========================================
clojure_inspect_project(explanation: "Understanding project structure")
read_file(path: "src/myapp/core.clj")
read_file(path: "src/myapp/core.clj", name_pattern: "related-function")

// Mark task complete
scratch_pad(
  op: "set_path",
  path: ["feature-tasks", 0, "done"],
  value: true,
  explanation: "Completed code review")

// ========================================
// STEP 3: Design in REPL
// ========================================
clojure_eval(code: """
(require '[myapp.core :reload])
(in-ns 'myapp.core)

;; Create test data
(def test-data {:email "user@example.com" :name "Test User"})

;; Prototype v1
(defn welcome-user [user]
  (str "Welcome, " (:name user) "!"))

(welcome-user test-data)  ; => "Welcome, Test User!"

;; Test edge cases
(welcome-user {:name nil})  ; What happens?
(welcome-user {})           ; What happens?
""")

// Refine in REPL
clojure_eval(code: """
;; Prototype v2 with better error handling
(defn welcome-user [user]
  {:pre [(map? user)]}
  (if-let [name (:name user)]
    (str \"Welcome, \" name \"!\")
    \"Welcome, Guest!\"))

(welcome-user test-data)        ; => "Welcome, Test User!"
(welcome-user {:name nil})      ; => "Welcome, Guest!"
(welcome-user {})               ; => "Welcome, Guest!"
(welcome-user {:name \"Alice\"}) ; => "Welcome, Alice!"
""")

// Mark task complete
scratch_pad(
  op: "set_path",
  path: ["feature-tasks", 1, "done"],
  value: true,
  explanation: "Completed REPL design")

// ========================================
// STEP 4: Save to file
// ========================================
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "defn",
  form_identifier: "existing-function",  // Insert after this
  operation: "insert_after",
  content: "(defn welcome-user
  \"Generates welcome message for user.
  Returns 'Welcome, Guest!' if name is missing.\"
  [user]
  {:pre [(map? user)]}
  (if-let [name (:name user)]
    (str \"Welcome, \" name \"!\")
    \"Welcome, Guest!\"))")

// Verify saved code works
clojure_eval(code: """
(require '[myapp.core :reload])
(in-ns 'myapp.core)
(welcome-user {:name \"Alice\"})
""")

// Mark task complete
scratch_pad(
  op: "set_path",
  path: ["feature-tasks", 2, "done"],
  value: true,
  explanation: "Completed implementation")

// ========================================
// STEP 5: Add tests
// ========================================
clojure_edit(
  file_path: "test/myapp/core_test.clj",
  form_type: "deftest",
  form_identifier: "existing-test",
  operation: "insert_after",
  content: "(deftest welcome-user-test
  (testing \"welcome message with name\"
    (is (= \"Welcome, Alice!\"
           (welcome-user {:name \"Alice\"}))))

  (testing \"welcome message without name\"
    (is (= \"Welcome, Guest!\"
           (welcome-user {})))
    (is (= \"Welcome, Guest!\"
           (welcome-user {:name nil}))))

  (testing \"requires map input\"
    (is (thrown? AssertionError
                 (welcome-user \"not-a-map\")))))")

// Mark task complete
scratch_pad(
  op: "set_path",
  path: ["feature-tasks", 3, "done"],
  value: true,
  explanation: "Completed tests")

// ========================================
// STEP 6: Run tests
// ========================================
bash(command: "bb test")

// Mark task complete
scratch_pad(
  op: "set_path",
  path: ["feature-tasks", 4, "done"],
  value: true,
  explanation: "Tests passing")

// ========================================
// STEP 7: Review completion
// ========================================
scratch_pad(
  op: "get_path",
  path: ["feature-tasks"],
  explanation: "Final task review")
```

### Workflow 2: Debugging a Problem

```clojure
// ========================================
// STEP 1: Reproduce in REPL
// ========================================
clojure_eval(code: """
(require '[myapp.core :reload])
(in-ns 'myapp.core)

;; Reproduce the failing case
(def failing-data {:email \"invalid-email\"})

(try
  (validate-email (:email failing-data))
  (catch Exception e
    {:error (.getMessage e)
     :class (class e)
     :data (ex-data e)}))
""")

// ========================================
// STEP 2: Inspect intermediate values
// ========================================
clojure_eval(code: """
(let [email (:email failing-data)]
  {:input email
   :type (type email)
   :string? (string? email)
   :contains-at? (clojure.string/includes? email \"@\")
   :regex-match (re-matches #\".+@.+\\..+\" email)})
""")

// ========================================
// STEP 3: Analyze the problem
// ========================================
think(thought: "The issue is clear:
- Input: 'invalid-email' (string, no '@')
- Current regex requires '@' and '.'
- Function throws on no match instead of returning false

Fix:
- Change to return boolean instead of throwing
- Test edge cases: nil, empty string, valid email")

// ========================================
// STEP 4: Test fix in REPL
// ========================================
clojure_eval(code: """
;; New implementation
(defn validate-email-v2 [email]
  (and (string? email)
       (not (empty? email))
       (clojure.string/includes? email \"@\")
       (some? (re-matches #\".+@.+\\..+\" email))))

;; Test cases
(validate-email-v2 \"valid@example.com\")  ; => true
(validate-email-v2 \"invalid-email\")       ; => false
(validate-email-v2 \"\")                    ; => false
(validate-email-v2 nil)                    ; => false
(validate-email-v2 \"@\")                   ; => false
(validate-email-v2 \"user@domain.com\")    ; => true
""")

// ========================================
// STEP 5: Apply fix to file
// ========================================
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "defn",
  form_identifier: "validate-email",
  operation: "replace",
  content: "(defn validate-email
  \"Validates email address format.
  Returns true if valid, false otherwise.\"
  [email]
  (and (string? email)
       (not (empty? email))
       (clojure.string/includes? email \"@\")
       (some? (re-matches #\".+@.+\\..+\" email))))")

// ========================================
// STEP 6: Verify fix works
// ========================================
clojure_eval(code: """
(require '[myapp.core :reload])
(in-ns 'myapp.core)
(validate-email \"invalid-email\")  ; Should return false now
(validate-email \"valid@example.com\")  ; Should return true
""")

// ========================================
// STEP 7: Add regression test
// ========================================
clojure_edit(
  file_path: "test/myapp/core_test.clj",
  form_type: "deftest",
  form_identifier: "validate-email-test",
  operation: "replace",
  content: "(deftest validate-email-test
  (testing \"valid email addresses\"
    (is (true? (validate-email \"user@example.com\")))
    (is (true? (validate-email \"first.last@company.co.uk\"))))

  (testing \"invalid email addresses\"
    (is (false? (validate-email \"invalid-email\")))
    (is (false? (validate-email \"\")))
    (is (false? (validate-email nil)))
    (is (false? (validate-email \"@\")))))")

// ========================================
// STEP 8: Run all tests
// ========================================
bash(command: "bb test")
```

### Workflow 3: Refactoring Code

```clojure
// ========================================
// STEP 1: Read current implementation
// ========================================
read_file(path: "src/myapp/core.clj", name_pattern: "process-order")

// ========================================
// STEP 2: Understand usage
// ========================================
grep(pattern: "process-order", include: "**/*.clj")

// ========================================
// STEP 3: Design new approach in REPL
// ========================================
clojure_eval(code: """
(require '[myapp.core :reload])
(in-ns 'myapp.core)

;; Test data
(def test-order
  {:id 123
   :items [{:name \"Widget\" :price 10.00}
           {:name \"Gadget\" :price 20.00}]})

;; Current implementation (copy from file)
(defn process-order-old [order]
  (let [items (:items order)
        subtotal (reduce + (map :price items))
        tax (* subtotal 0.08)
        total (+ subtotal tax)]
    (assoc order :total total)))

;; New implementation - more modular
(defn- calculate-subtotal [items]
  (reduce + (map :price items)))

(defn- calculate-tax [subtotal]
  (* subtotal 0.08))

(defn- calculate-total [subtotal tax]
  (+ subtotal tax))

(defn process-order-new [order]
  (let [subtotal (calculate-subtotal (:items order))
        tax (calculate-tax subtotal)
        total (calculate-total subtotal tax)]
    (assoc order
           :subtotal subtotal
           :tax tax
           :total total)))

;; Verify both give same result
(process-order-old test-order)
(process-order-new test-order)
;; => Both should return same :total value

;; Test edge cases
(process-order-new {:items []})
(process-order-new {:items [{:price 5}]})
""")

// ========================================
// STEP 4: Apply refactoring
// ========================================

// First, add helper functions
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "defn",
  form_identifier: "process-order",
  operation: "insert_before",
  content: "(defn- calculate-subtotal
  \"Calculates order subtotal from items.\"
  [items]
  (reduce + (map :price items)))

(defn- calculate-tax
  \"Calculates tax on subtotal (8%).\"
  [subtotal]
  (* subtotal 0.08))

(defn- calculate-total
  \"Calculates final total from subtotal and tax.\"
  [subtotal tax]
  (+ subtotal tax))")

// Then, update main function
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "defn",
  form_identifier: "process-order",
  operation: "replace",
  content: "(defn process-order
  \"Processes order, calculating subtotal, tax, and total.\"
  [order]
  (let [subtotal (calculate-subtotal (:items order))
        tax (calculate-tax subtotal)
        total (calculate-total subtotal tax)]
    (assoc order
           :subtotal subtotal
           :tax tax
           :total total)))")

// ========================================
// STEP 5: Verify refactoring
// ========================================
clojure_eval(code: """
(require '[myapp.core :reload])
(in-ns 'myapp.core)
(process-order {:items [{:price 10} {:price 20}]})
""")

// ========================================
// STEP 6: Run existing tests (should pass)
// ========================================
bash(command: "bb test")
```

---

## Example Bank

### Copy-Paste Ready Patterns

#### Pattern: Explore New File

```clojure
// 1. Quick overview
read_file(path: "src/myapp/new_file.clj")

// 2. Expand interesting functions
read_file(path: "src/myapp/new_file.clj", name_pattern: "function-name")

// 3. Test in REPL
clojure_eval(code: """
(require '[myapp.new-file :reload])
(in-ns 'myapp.new-file)
;; ... test functions ...
""")
```

#### Pattern: Add Function to Existing File

```clojure
// 1. Design in REPL
clojure_eval(code: """
(require '[myapp.core :reload])
(in-ns 'myapp.core)

(defn my-new-function [x]
  (* x 2))

(my-new-function 5)  ; => 10
(my-new-function 0)  ; => 0
(my-new-function -5) ; => -10
""")

// 2. Save to file
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "defn",
  form_identifier: "existing-function",
  operation: "insert_after",
  content: "(defn my-new-function
  \"Doubles the input value.\"
  [x]
  (* x 2))")

// 3. Verify
clojure_eval(code: """
(require '[myapp.core :reload])
(my-new-function 5)
""")
```

#### Pattern: Update Namespace Requires

```clojure
// 1. Read current ns declaration
read_file(path: "src/myapp/core.clj", name_pattern: "^ns")

// 2. Update with new requires
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "ns",
  form_identifier: "myapp.core",
  operation: "replace",
  content: "(ns myapp.core
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [new.dependency :as dep]))")

// 3. Verify it loads
clojure_eval(code: "(require '[myapp.core :reload])")
```

#### Pattern: Rename Symbol Throughout File

```clojure
clojure_edit_replace_sexp(
  file_path: "src/myapp/core.clj",
  match_form: "old-function-name",
  new_form: "new-function-name",
  replace_all: true)
```

#### Pattern: Add Error Handling

```clojure
// 1. Find function to wrap
read_file(path: "src/myapp/core.clj", name_pattern: "risky-function")

// 2. Test wrapped version in REPL
clojure_eval(code: """
(require '[myapp.core :reload])
(in-ns 'myapp.core)

(defn safe-risky-function [x]
  (try
    (risky-function x)
    (catch Exception e
      (log/error e \"risky-function failed\" {:input x})
      nil)))

(safe-risky-function \"test\")
""")

// 3. Replace in file
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "defn",
  form_identifier: "risky-function",
  operation: "replace",
  content: "(defn risky-function
  \"Does risky operation with error handling.\"
  [x]
  (try
    ;; ... existing logic ...
    (catch Exception e
      (log/error e \"risky-function failed\" {:input x})
      nil)))")
```

#### Pattern: Multi-File Feature (Database + API + Tests)

```clojure
// 1. Plan
scratch_pad(
  op: "set_path",
  path: ["feature"],
  value: {
    files: ["src/myapp/db.clj", "src/myapp/api.clj", "test/myapp/api_test.clj"],
    tasks: [
      {file: "db.clj", task: "Add query function", done: false},
      {file: "api.clj", task: "Add endpoint", done: false},
      {file: "api_test.clj", task: "Add tests", done: false}
    ]
  },
  explanation: "Planning multi-file feature")

// 2. Database layer (first)
clojure_eval(code: """
(require '[myapp.db :reload])
(in-ns 'myapp.db)

(defn get-user-by-email [db email]
  ;; ... query logic ...
  )

;; Test with mock data
(get-user-by-email mock-db \"test@example.com\")
""")

clojure_edit(
  file_path: "src/myapp/db.clj",
  form_type: "defn",
  form_identifier: "existing-query",
  operation: "insert_after",
  content: "(defn get-user-by-email ...)"))

// 3. API layer (second)
clojure_eval(code: """
(require '[myapp.api :reload])
(in-ns 'myapp.api)
;; ... test endpoint ...
""")

clojure_edit(
  file_path: "src/myapp/api.clj",
  ...)

// 4. Tests (last)
clojure_edit(
  file_path: "test/myapp/api_test.clj",
  ...)

// 5. Run all tests
bash(command: "bb test")
```

---

## Common Mistakes

### ❌ Mistake 1: Editing Before Testing

**Wrong**:
```clojure
// Immediately editing file without REPL validation
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "defn",
  form_identifier: "new-function",
  operation: "replace",
  content: "(defn new-function [x] (+ x 1))")  // Hope it works!
```

**Right**:
```clojure
// First: Test in REPL
clojure_eval(code: """
(require '[myapp.core :reload])
(in-ns 'myapp.core)
(defn new-function [x] (+ x 1))
(new-function 5)   ; => 6
(new-function 0)   ; => 1
(new-function -1)  ; => 0
""")

// Then: Save after validation
clojure_edit(...)
```

**Why it matters**: You waste time fixing syntax errors and logic bugs that REPL would catch instantly.

---

### ❌ Mistake 2: Forgetting to Reload

**Wrong**:
```clojure
// Made changes to file, then:
clojure_eval(code: "(in-ns 'myapp.core)")
clojure_eval(code: "(test-function)")  // Testing OLD code!
```

**Right**:
```clojure
clojure_eval(code: """
(require '[myapp.core :reload])  ; ← CRITICAL
(in-ns 'myapp.core)
(test-function)
""")
```

**Why it matters**: You'll test old code and think your changes don't work.

---

### ❌ Mistake 3: Using file_edit for Clojure Files

**Wrong**:
```clojure
file_edit(
  file_path: "src/myapp/core.clj",
  old_string: "(defn old-fn [x]\n  (+ x 1))",
  new_string: "(defn new-fn [x]\n  (* x 2))")
// Brittle! Fails if whitespace doesn't match exactly
```

**Right**:
```clojure
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "defn",
  form_identifier: "old-fn",
  operation: "replace",
  content: "(defn new-fn [x] (* x 2))")
// Robust! Matches structurally, not textually
```

**Why it matters**: `file_edit` breaks on whitespace differences. `clojure_edit` understands code structure.

---

### ❌ Mistake 4: Reading Files Without Collapsed View

**Wrong**:
```clojure
// Immediately reading full file (1000+ lines!)
read_file(path: "src/myapp/core.clj", collapsed: false)
```

**Right**:
```clojure
// First: Get overview
read_file(path: "src/myapp/core.clj")
// Shows: (defn fn1 ...) (defn fn2 ...) (defn fn3 ...)

// Then: Expand what you need
read_file(path: "src/myapp/core.clj", name_pattern: "fn2")
// Shows: Full implementation of fn2 only
```

**Why it matters**: Collapsed view saves tokens and shows you structure first.

---

### ❌ Mistake 5: Not Planning Multi-Step Tasks

**Wrong**:
```clojure
// Just start coding without plan
// ... 30 minutes later ...
// "Wait, what was I supposed to do again?"
```

**Right**:
```clojure
scratch_pad(
  op: "set_path",
  path: ["tasks"],
  value: [
    {task: "Read existing code", done: false},
    {task: "Design in REPL", done: false},
    {task: "Implement", done: false},
    {task: "Test", done: false}
  ],
  explanation: "Planning work")

// ... work through tasks, marking each complete ...
```

**Why it matters**: You won't forget steps or lose track of progress.

---

### ❌ Mistake 6: Batching Edits Without Validation

**Wrong**:
```clojure
clojure_edit(...)  // Change 1
clojure_edit(...)  // Change 2
clojure_edit(...)  // Change 3
bash(command: "bb test")  // Hope everything works!
```

**Right**:
```clojure
clojure_edit(...)  // Change 1
clojure_eval(code: "(require '[myapp.core :reload])")
clojure_eval(code: "(test-change-1)")  // ✓ Works

clojure_edit(...)  // Change 2
clojure_eval(code: "(require '[myapp.core :reload])")
clojure_eval(code: "(test-change-2)")  // ✓ Works

bash(command: "bb test")  // Final verification
```

**Why it matters**: If tests fail, you know exactly which change broke things.

---

### ❌ Mistake 7: Using dispatch_agent for Simple Searches

**Wrong**:
```clojure
dispatch_agent(prompt: "Find the validate-email function")
// Wastes time and API calls
```

**Right**:
```clojure
grep(pattern: "defn validate-email", include: "**/*.clj")
// Fast and direct
```

**Why it matters**: Use simple tools for simple tasks.

---

### ❌ Mistake 8: Incomplete defmethod Identifiers

**Wrong**:
```clojure
clojure_edit(
  file_path: "src/shapes.clj",
  form_type: "defmethod",
  form_identifier: "area",  // ❌ Missing dispatch value!
  operation: "replace",
  content: "...")
// Will fail or match wrong method
```

**Right**:
```clojure
clojure_edit(
  file_path: "src/shapes.clj",
  form_type: "defmethod",
  form_identifier: "area :rectangle",  // ✅ Includes dispatch value
  operation: "replace",
  content: "(defmethod area :rectangle [{:keys [w h]}] (* w h))")
```

**Why it matters**: defmethod needs dispatch value to match the right implementation.

---

## Quick Reference Card

### Essential Commands

```clojure
// ============ REPL STARTUP ============
clojure_eval(code: """
(require '[myapp.core :reload])
(in-ns 'myapp.core)
""")

// ============ READ CODE ============
read_file(path: "src/myapp/core.clj")                           // Overview
read_file(path: "src/myapp/core.clj", name_pattern: "validate.*") // Specific fns
grep(pattern: "defn.*validate", include: "*.clj")               // Find files

// ============ EDIT CODE ============
clojure_edit(                                    // Top-level forms
  file_path: "src/myapp/core.clj",
  form_type: "defn",
  form_identifier: "function-name",
  operation: "replace",
  content: "(defn function-name [x] (* x 2))")

clojure_edit_replace_sexp(                       // Expressions within forms
  file_path: "src/myapp/core.clj",
  match_form: "(old-expr)",
  new_form: "(new-expr)")

// ============ TEST ============
clojure_eval(code: "(my-function test-data)")    // REPL test
bash(command: "bb test")                          // Run test suite

// ============ PLAN ============
scratch_pad(
  op: "set_path",
  path: ["tasks"],
  value: [{task: "...", done: false}])
```

### The Core Loop

```
1. REPL → Design & validate
2. SAVE → Use clojure_edit
3. TEST → Verify it works
4. REPEAT
```

---

**Remember**: REPL first, always. If it works in REPL, then save
it. This is the way.
