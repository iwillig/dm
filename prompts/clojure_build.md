# Clojure Development Guide for LLM Agents

**PURPOSE**: This guide ensures LLM agents use the correct tools for Clojure development and follow best practices.

---

## 🎯 CRITICAL RULES - ALWAYS FOLLOW THESE

### Rule 1: NEVER use `file_write` or `file_edit` for Clojure files (.clj, .cljs, .cljc, .bb)
❌ **WRONG:**
```
file_write(file_path: "src/myapp/core.clj", content: "...")
file_edit(file_path: "src/myapp/core.clj", old_string: "...", new_string: "...")
```

✅ **RIGHT:**
```
clojure_edit(file_path: "src/myapp/core.clj", form_type: "defn", form_identifier: "my-func", operation: "replace", content: "...")
```

**WHY**: `clojure_edit` provides structural matching, syntax validation, and linting feedback. Text-based tools are brittle and error-prone.

---

### Rule 2: ALWAYS validate code in REPL BEFORE saving to file
❌ **WRONG:**
```
clojure_edit(file_path: "src/myapp/core.clj", form_type: "defn", form_identifier: "my-func", operation: "replace", content: "(defn my-func [x] (+ x 1))")
; Hope it works!
```

✅ **RIGHT:**
```
; Step 1: Test in REPL first
clojure_eval(code: """
(require '[myapp.core :reload])
(in-ns 'myapp.core)
(defn my-func [x] (+ x 1))
(my-func 5)    ; => 6 ✓
(my-func -1)   ; => 0 ✓
""")

; Step 2: Save to file after validation
clojure_edit(file_path: "src/myapp/core.clj", form_type: "defn", form_identifier: "my-func", operation: "replace", content: "(defn my-func [x] (+ x 1))")

; Step 3: Verify the saved code reloads correctly
clojure_eval(code: """
(require '[myapp.core :reload])
(my-func 5)    ; => 6 ✓
""")
```

**WHY**: Catches syntax errors, logic bugs, and compilation errors instantly. Saves time and prevents broken code.

---

### Rule 3: ALWAYS use `clojure_edit` with correct form_type and form_identifier
✅ **CORRECT PATTERNS:**

**For functions:**
```
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "defn",
  form_identifier: "my-function",
  operation: "replace",
  content: "(defn my-function [x] (* x 2))")
```

**For namespaces:**
```
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "ns",
  form_identifier: "myapp.core",
  operation: "replace",
  content: "(ns myapp.core (:require [clojure.string :as str]))")
```

**For defmethod (MUST include dispatch value):**
```
clojure_edit(
  file_path: "src/myapp/shapes.clj",
  form_type: "defmethod",
  form_identifier: "area :circle",
  operation: "replace",
  content: "(defmethod area :circle [{:keys [radius]}] (* 3.14 radius radius))")
```

**For def (constants):**
```
clojure_edit(
  file_path: "src/myapp/config.clj",
  form_type: "def",
  form_identifier: "api-key",
  operation: "replace",
  content: "(def api-key (System/getenv \"API_KEY\"))")
```

**For tests:**
```
clojure_edit(
  file_path: "test/myapp/core_test.clj",
  form_type: "deftest",
  form_identifier: "my-function-test",
  operation: "replace",
  content: "(deftest my-function-test (is (= 10 (my-function 5))))")
```

---

### Rule 4: Use operation "insert_before" or "insert_after" to add new code, NOT replace
❌ **WRONG** - Replacing entire file:
```
clojure_edit(file_path: "src/myapp/core.clj", form_type: "defn", form_identifier: "existing-func", operation: "replace", content: "entire file content")
```

✅ **RIGHT** - Inserting after existing function:
```
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "defn",
  form_identifier: "existing-func",
  operation: "insert_after",
  content: "(defn new-func [x] (* x 2))")
```

**WHY**: Preserves existing code, prevents accidental deletions.

---

### Rule 5: Use `clojure_edit_replace_sexp` for changing expressions WITHIN functions
✅ **CORRECT:**
```
; Rename a symbol throughout
clojure_edit_replace_sexp(
  file_path: "src/myapp/core.clj",
  match_form: "old-name",
  new_form: "new-name",
  replace_all: true)

; Change an expression
clojure_edit_replace_sexp(
  file_path: "src/myapp/core.clj",
  match_form: "(+ x 2)",
  new_form: "(* x 2)")

; Wrap in error handling
clojure_edit_replace_sexp(
  file_path: "src/myapp/core.clj",
  match_form: "(risky-operation)",
  new_form: "(try (risky-operation) (catch Exception e (log/error e)))")
```

**WHY**: Syntax-aware matching ignores formatting differences, handles complex expressions reliably.

---

## ✅ WORKFLOW: How to Implement Code

### Pattern: Add New Feature

```clojure
; 1. UNDERSTAND - Read existing code
clojure_read_file(path: "src/myapp/core.clj")
clojure_read_file(path: "src/myapp/core.clj", name_pattern: "related-func.*")

; 2. DESIGN - Test in REPL
clojure_eval(code: """
(require '[myapp.core :reload])
(in-ns 'myapp.core)

; Design and test the function
(defn my-new-feature [arg1 arg2]
  (let [result (process arg1 arg2)]
    (validate result)))

; Test with various inputs
(my-new-feature "test1" "test2")     ; => expected result
(my-new-feature "" "test")           ; => handles edge case
(my-new-feature nil nil)             ; => handles nil
""")

; 3. SAVE - Use clojure_edit, not file_write
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "defn",
  form_identifier: "existing-function",
  operation: "insert_after",
  content: "(defn my-new-feature [arg1 arg2] (let [result (process arg1 arg2)] (validate result)))")

; 4. VERIFY - Reload and test
clojure_eval(code: """
(require '[myapp.core :reload])
(in-ns 'myapp.core)
(my-new-feature "test1" "test2")   ; Should work ✓
""")

; 5. TEST - Add test case
clojure_edit(
  file_path: "test/myapp/core_test.clj",
  form_type: "deftest",
  form_identifier: "existing-test",
  operation: "insert_after",
  content: "(deftest my-new-feature-test (is (= expected (my-new-feature \"test1\" \"test2\"))))")

; 6. VALIDATE - Run all tests
clojure_eval(code: "(require '[kaocha.repl :as k]) (k/run-all)")
```

---

## 🔧 Tool Selection Guide

### When Reading Code

| Goal | Tool | Example |
|------|------|---------|
| Get project overview | `clojure_inspect_project` | `clojure_inspect_project(explanation: "Project structure")` |
| Find files by name | `clojure_glob_files` | `clojure_glob_files(pattern: "**/*_test.clj")` |
| Find files by content | `clojure_grep` | `clojure_grep(pattern: "defn my-func", include: "src/**/*.clj")` |
| Read Clojure file first time | `clojure_read_file` (collapsed) | `clojure_read_file(path: "src/myapp/core.clj")` |
| Expand specific functions | `clojure_read_file` (with pattern) | `clojure_read_file(path: "src/myapp/core.clj", name_pattern: "get-.*")` |
| Search within function | `clojure_read_file` (content) | `clojure_read_file(path: "src/myapp/core.clj", content_pattern: "try\|catch")` |

### When Editing Code

| Situation | Tool | Why |
|-----------|------|-----|
| **Creating new top-level form** | `clojure_edit` with `insert_after` | Structural, validated, idiomatic |
| **Replacing top-level form** | `clojure_edit` with `replace` | Structural matching, syntax validation |
| **Changing expression within form** | `clojure_edit_replace_sexp` | Syntax-aware, ignores whitespace |
| **Editing markdown/YAML/config** | `file_edit` or `file_write` | Not Clojure code |

### When Testing Code

| Goal | Tool | Example |
|------|------|---------|
| Evaluate expression | `clojure_eval` | `clojure_eval(code: "(+ 1 2)")` |
| Test function with sample data | `clojure_eval` | `clojure_eval(code: "(my-func test-data)")` |
| Run entire test suite | `clojure_eval` | `clojure_eval(code: "(require '[kaocha.repl :as k]) (k/run-all)")` |
| Run specific test | `clojure_eval` | `clojure_eval(code: "(require '[kaocha.repl :as k]) (k/run 'myapp.core-test/my-test)")` |
| Run tests in namespace | `clojure_eval` | `clojure_eval(code: "(require '[kaocha.repl :as k]) (k/run *ns*)")` |

---

## 📋 Complete Example: Refactoring Species Queries

This example shows the CORRECT way to do a refactoring task.

```clojure
; ============================================
; PHASE 1: READ & UNDERSTAND
; ============================================

; Read the entire monolithic file
clojure_read_file(path: "src/dm/queries.clj", collapsed: false)

; Read just the species section with pattern
clojure_read_file(path: "src/dm/queries.clj", name_pattern: "species")

; Find which files use species queries
clojure_grep(pattern: "get-all-species|get-species-by-code", include: "**/*.clj")

; ============================================
; PHASE 2: UNDERSTAND DEPENDENCIES
; ============================================

; Check if we can create query_helpers
clojure_read_file(path: "src/dm/queries.clj", name_pattern: "query|execute")

; ============================================
; PHASE 3: DESIGN IN REPL
; ============================================

clojure_eval(code: """
(require '[honey.sql :as sql])
(require '[next.jdbc :as jdbc])
(require '[next.jdbc.result-set :as rs])

; Test that helpers work
(defn query [db sql-map]
  (jdbc/execute! db (sql/format sql-map) {:builder-fn rs/as-unqualified-lower-maps}))

; Verify the helper works
(println "Query helper designed and tested")
""")

; ============================================
; PHASE 4: CREATE FILES USING clojure_edit
; ============================================

; This is CRITICAL - use clojure_edit, NOT file_write
clojure_edit(
  file_path: "src/dm/db/query_helpers.clj",
  form_type: "ns",
  form_identifier: "dm.db.query-helpers",
  operation: "insert_after",
  content: """(defn query
  \"Execute a SELECT query returning multiple rows.\"
  [db sql-map]
  (jdbc/execute! db (sql/format sql-map) {:builder-fn rs/as-unqualified-lower-maps}))""")

; Create species namespace
clojure_edit(
  file_path: "src/dm/db/species.clj",
  form_type: "ns",
  form_identifier: "dm.db.species",
  operation: "insert_after",
  content: """(defn get-all-species
  \"Get all species ordered by name.\"
  [db _params]
  (qh/query db {:select [:*] :from [:species] :order-by [:name]}))""")

; ============================================
; PHASE 5: VERIFY IN REPL
; ============================================

clojure_eval(code: """
(require '[dm.db.query-helpers :as qh])
(require '[dm.db.species :as species])
(println \"✅ Both namespaces loaded successfully\")
""")

; ============================================
; PHASE 6: RUN TESTS
; ============================================

clojure_eval(code: """
(require '[kaocha.repl :as k])
(k/run-all)
; Verify all tests still pass
""")
```

---

## ❌ ANTI-PATTERNS - DO NOT DO THESE

### ❌ Anti-pattern 1: Using file_write for Clojure code
```
file_write(file_path: "src/myapp/core.clj", content: "(defn my-func [x] (* x 2))")
```
**Problem**: No syntax validation, no structural matching, breaks indentation
**Use instead**: `clojure_edit`

---

### ❌ Anti-pattern 2: Using file_edit for Clojure code
```
file_edit(
  file_path: "src/myapp/core.clj",
  old_string: "  (defn old-name [x] (+ x 1))",
  new_string: "  (defn new-name [x] (+ x 1))")
```
**Problem**: Brittle text matching, indentation issues, whitespace sensitivity
**Use instead**: `clojure_edit`

---

### ❌ Anti-pattern 3: Saving without testing
```
clojure_edit(file_path: "src/myapp/core.clj", form_type: "defn", form_identifier: "my-func", operation: "replace", content: "(defn my-func [x] ...)")
; Immediately move to next task without testing
```
**Problem**: Syntax errors discovered later, breaks test suite
**Use instead**: Always test in REPL first

---

### ❌ Anti-pattern 4: Not reloading before testing saved code
```
clojure_eval(code: "(my-func 5)")
; Testing old version of code!
```
**Problem**: Tests pass but changes aren't actually being tested
**Use instead**: `(require '[myapp.core :reload])`

---

### ❌ Anti-pattern 5: Using wrong form_type
```
clojure_edit(
  file_path: "src/myapp/core.clj",
  form_type: "defn",  ; WRONG - this is a def, not defn
  form_identifier: "api-key",
  operation: "replace",
  content: "(def api-key \"secret\")")
```
**Problem**: clojure_edit can't find the form to edit
**Use instead**: Match the actual form type (`def`, not `defn`)

---

### ❌ Anti-pattern 6: Forgetting dispatch value in defmethod
```
clojure_edit(
  file_path: "src/myapp/shapes.clj",
  form_type: "defmethod",
  form_identifier: "area",  ; WRONG - missing dispatch value
  operation: "replace",
  content: "(defmethod area :circle [...] ...)")
```
**Problem**: Can't uniquely identify which implementation to edit
**Use instead**: Include dispatch value: `"area :circle"`

---

### ❌ Anti-pattern 7: Creating files without prototype
```
clojure_edit(file_path: "src/myapp/new_module.clj", form_type: "ns", form_identifier: "myapp.new-module", operation: "replace", content: "entire complex file")
```
**Problem**: Large edits without testing, high chance of errors
**Use instead**: 
1. Prototype and test in REPL
2. Build file incrementally using `insert_before` or `insert_after`
3. Test frequently

---

## 🎓 Decision Tree: Which Tool to Use

```
Is it a Clojure file (.clj, .cljs, .cljc, .bb)?
  ├─ YES: Are you creating/editing a top-level form (defn, def, deftest, ns, etc)?
  │   ├─ YES: Use clojure_edit ⭐ (with form_type and form_identifier)
  │   └─ NO: Are you changing an expression WITHIN a form?
  │       ├─ YES: Use clojure_edit_replace_sexp ⭐
  │       └─ NO: Use clojure_edit for the parent form
  └─ NO: Is it markdown/YAML/JSON/config?
      ├─ YES: Use file_edit or file_write
      └─ NO: Use appropriate tool for that file type

Need to read code?
  ├─ Want overview? → clojure_inspect_project
  ├─ Know the file? → clojure_read_file (collapsed by default)
  ├─ Find by name? → clojure_glob_files
  ├─ Find by content? → clojure_grep
  └─ Need to search? → clojure_grep with regex

Need to test code?
  └─ Use clojure_eval ⭐ (ALWAYS test before saving)
```

---

## 📝 Checklist Before Using clojure_edit

- [ ] Did I test this code in the REPL first?
- [ ] Did I use the correct `form_type`? (defn, def, deftest, ns, etc)
- [ ] Did I include the correct `form_identifier`? (function name, dispatch value if defmethod)
- [ ] Is my `content` valid Clojure code?
- [ ] Am I using `insert_before`/`insert_after` to add new code, not replacing existing?
- [ ] For defmethod: Did I include the dispatch value in the identifier?
- [ ] Did I verify the code loads and works after editing?

---

## 🚀 Quick Reference

| Need | Tool | Command |
|------|------|---------|
| Test code | `clojure_eval` | `clojure_eval(code: "...")` |
| Read file | `clojure_read_file` | `clojure_read_file(path: "...")` |
| Find files | `clojure_glob_files` | `clojure_glob_files(pattern: "...")` |
| Search files | `clojure_grep` | `clojure_grep(pattern: "...", include: "...")` |
| Create/edit function | `clojure_edit` | `clojure_edit(file_path: "...", form_type: "defn", form_identifier: "...", operation: "replace", content: "...")` |
| Edit expression | `clojure_edit_replace_sexp` | `clojure_edit_replace_sexp(file_path: "...", match_form: "...", new_form: "...")` |
| Edit config/markdown | `file_edit` or `file_write` | For non-Clojure files only |

---

## ⚠️ Summary: The One Thing to Remember

**For Clojure code: Use `clojure_edit`. For everything else: Use `file_edit` or `file_write`.**

If you find yourself reaching for `file_write` or `file_edit` on a `.clj` file, STOP and use `clojure_edit` instead.
