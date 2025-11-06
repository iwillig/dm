# Kaocha REPL Testing Guide

> Full featured next-gen Clojure test runner

## What is Kaocha?

**Kaocha** (考察 - "to inspect, to observe and study") is a modern test runner for Clojure that's designed for **interactive, REPL-driven development**.

Unlike traditional test runners that are CLI-only, **Kaocha gives you full programmatic control from the REPL**, allowing you to:
- Run tests interactively while developing
- Focus on specific tests
- Get beautiful, detailed output
- Watch files for changes
- Extend with plugins
- All without restarting

---

## Installation

Kaocha is already in your project's dev dependencies. Just require it:

```clojure
(require '[kaocha.repl :as k])
```

Or in your dev namespace (like dm project does):

```clojure
(ns dev
  (:require [kaocha.repl :as k]))
```

---

## Quick Start: Running Tests from REPL

### Basic Commands

```clojure
;; Run all tests
(k/run-all)

;; Run specific test suite (e.g., :unit)
(k/run :unit)

;; Run specific test namespace
(k/run 'dm.db-test)

;; Run specific test function
(k/run 'dm.db-test/test-characters-crud)

;; Run tests matching a pattern
(k/run :unit {:kaocha/focus [".*character.*"]})
```

### What Each Returns

```clojure
(k/run-all)
;; Returns: map with :tests, :failures, :passes, :errors, etc.
;; Example:
;; {:kaocha/tests 45
;;  :kaocha/failures 0
;;  :kaocha/errors 0
;;  :kaocha/passes 45}

;; Successful result - returns truthy
;; Failed result - returns falsy (useful for `if` statements)
```

---

## The Three Main Kaocha REPL Functions

### 1. `k/run-all` - Run Everything

Runs all configured test suites.

```clojure
(k/run-all)
;; Output:
;; ======= Testing ========
;; dm.api-test/test-okay-api
;;   ✓ test-okay-api
;; dm.db-test/test-attribute-names-crud
;;   ✓ Given: an empty attribute_names table
;;   ✓   When: we insert an attribute name
;;   ...
;; ...
;; 45 tests, 45 assertions, 0 failures
```

**When to use**: Full test suite validation, CI checks, before commits

---

### 2. `k/run` - Flexible Test Running

The most versatile function for REPL testing.

#### Run all tests (same as run-all)
```clojure
(k/run)
(k/run-all)  ; equivalent
```

#### Run specific suite by ID
```clojure
(k/run :unit)  ; run the :unit suite
```

#### Run specific namespace
```clojure
(k/run 'dm.db-test)          ; whole namespace
(k/run 'my.project.api-test) ; any namespace
```

#### Run specific test
```clojure
(k/run 'dm.db-test/test-characters-crud)
```

#### Run current namespace's tests (in REPL)
```clojure
(k/run *ns*)  ; runs tests in current namespace
(k/run)       ; shorthand (no args = current ns)
```

#### With options (configuration map as second arg)
```clojure
;; Fail fast (stop at first failure)
(k/run :unit {:kaocha/fail-fast? true})

;; Random order with seed
(k/run :unit {::k/randomize {:kaocha.plugin.randomize/seed 12345}})

;; Skip tests matching pattern
(k/run :unit {:kaocha/skip [".*slow.*"]})

;; Only run tests matching pattern
(k/run :unit {:kaocha/focus [".*character.*"]})

;; Combine options
(k/run :unit
  {:kaocha/fail-fast? true
   :kaocha/focus ["test-character.*"]})
```

---

### 3. `k/test-plan` - Inspect Test Configuration

Returns the test plan without running tests. Useful for understanding which tests will run.

```clojure
;; Get full test plan
(k/test-plan)

;; Get plan for specific suite
(k/test-plan {:suite :unit})

;; Returns a data structure describing all tests
;; Useful for debugging why tests aren't running
```

---

## Complete REPL Testing Workflows

### Workflow 1: TDD - Develop One Test at a Time

```clojure
(require '[clojure.test :as t]
         '[kaocha.repl :as k])

;; 1. Write a test
(t/deftest new-feature-test
  (t/is (= 42 (my-new-function))))

;; 2. Run just this test immediately
(k/run 'my.project-test/new-feature-test)
;; => {:kaocha/tests 1 :kaocha/failures 1 ...}  (fails, as expected)

;; 3. Implement the function
(defn my-new-function [] 42)

;; 4. Run test again (still in REPL)
(k/run 'my.project-test/new-feature-test)
;; => {:kaocha/tests 1 :kaocha/failures 0 ...}  (passes!)

;; 5. Run all related tests to make sure nothing broke
(k/run :unit)

;; 6. Once confident, commit
```

**Advantage**: Immediate feedback without leaving REPL, quick iteration

---

### Workflow 2: Debugging a Failing Test

```clojure
(require '[kaocha.repl :as k]
         '[sc.api])  ; scope-capture

;; 1. Your test is failing
(k/run 'dm.db-test/test-character-attributes-crud)
;; => Test fails with assertion error

;; 2. Add scope-capture to debug (in test or source)
(defn get-character-attributes [db {:keys [character_id]}]
  (let [result (query db character_id)]
    (sc.api/spy result)))  ; ← add this

;; 3. Run test again to capture the scope
(k/run 'dm.db-test/test-character-attributes-crud)

;; 4. Now restore context and debug
(require 'sc.api)
(sc.api/defsc execution-point-id)  ; from spy output

;; 5. Test your hypothesis in REPL
(keys result)
(count result)

;; 6. Fix the code once you understand the issue
(clojure_edit ...)  ; from main prompt

;; 7. Re-run test
(k/run 'dm.db-test/test-character-attributes-crud)
```

**Advantage**: Combine Kaocha + scope-capture for powerful debugging

---

### Workflow 3: Focus on Specific Tests While Developing

```clojure
(require '[kaocha.repl :as k])

;; You're working on character creation feature
;; You want to run only character-related tests, not the whole suite

;; Option 1: By test name pattern
(k/run :unit {:kaocha/focus [".*character.*"]})

;; Option 2: Run specific test namespace
(k/run 'dm.db-test)

;; Option 3: Run exact test
(k/run 'dm.db-test/test-characters-crud)

;; Option 4: Combine with other options
(k/run :unit
  {:kaocha/focus [".*character.*"]
   :kaocha/fail-fast? true})  ; stop on first failure

;; Do this in a loop:
;; 1. Edit code
;; 2. (k/run :unit {:kaocha/focus [".*character.*"]})
;; 3. See results immediately
;; 4. Repeat
```

**Advantage**: Fast feedback loop, only relevant tests run

---

### Workflow 4: Interactive Test Development with Spy

```clojure
(require '[kaocha.repl :as k]
         '[sc.api])

;; 1. Write test with spy to capture data
(t/deftest test-complex-calculation
  (let [result (my-complex-calc {:a 1 :b 2})]
    (is (= expected (sc.api/spy result)))))

;; 2. Run test
(k/run 'my-test)

;; 3. Restore captured context
(sc.api/defsc ep-id)

;; 4. Experiment with different inputs
(my-complex-calc {:a 10 :b 20})
(my-complex-calc {:a -5 :b 3})

;; 5. Verify fix works
(k/run 'my-test)
```

**Advantage**: Understand exactly what your test data is

---

## Advanced Kaocha REPL Features

### Using Options Map

The second parameter to `k/run` is a Kaocha configuration map:

```clojure
;; Common options:
{:kaocha/fail-fast? true          ; stop on first failure
 :kaocha/skip ["pattern"]         ; skip tests matching pattern
 :kaocha/focus ["pattern"]        ; only run tests matching pattern
 :kaocha/plugins [:plugin-name]   ; enable plugins
 :kaocha/color? true              ; colored output
}

;; Example: run character tests, fail fast
(k/run :unit
  {:kaocha/focus [".*character.*"]
   :kaocha/fail-fast? true})
```

### Checking Test Results

```clojure
;; Store result and inspect it
(def result (k/run :unit))

;; Check if tests passed
(if result
  (println "All tests passed!")
  (println "Some tests failed"))

;; Inspect detailed results
result
;; => {:kaocha/tests 45
;;     :kaocha/failures 2
;;     :kaocha/errors 0
;;     :kaocha/passes 43}

(:kaocha/failures result)  ; => 2
(:kaocha/errors result)    ; => 0
```

### Running Tests by Metadata

If your tests have metadata:

```clojure
(deftest ^:slow slow-test
  (is true))

(deftest ^:unit normal-test
  (is true))

;; Run only tests with :unit metadata
(k/run :unit {:kaocha/focus-meta [:unit]})

;; Skip tests with :slow metadata
(k/run :unit {:kaocha/skip-meta [:slow]})
```

---

## Kaocha in the dm Project

The dm project has Kaocha already configured. Here's how to use it:

### Setup (usually in dev/dev.clj)
```clojure
(ns dev
  (:require [kaocha.repl :as k]))

;; If using fast-dev pattern:
(defn all-tests []
  (k/run-all))

;; Or just use k/run directly
```

### Running dm Tests

```clojure
(require '[kaocha.repl :as k])

;; Run all tests
(k/run-all)

;; Run just unit tests
(k/run :unit)

;; Run database tests
(k/run 'dm.db-test)

;; Run character-related tests
(k/run :unit {:kaocha/focus [".*character.*"]})

;; Run with fail-fast
(k/run :unit {:kaocha/fail-fast? true})

;; Run specific test
(k/run 'dm.db-test/test-characters-crud)
```

### Typical dm Workflow

```clojure
;; 1. You're adding a new query function
;; 2. Write the test
(t/deftest test-new-query
  (let [result (query-new-thing db)]
    (is (valid? result))))

;; 3. Test fails (expected)
(k/run 'dm.db-test/test-new-query)

;; 4. Implement the query in dm/queries.clj
(clojure_edit ...)

;; 5. Reload and test again
(k/run 'dm.db-test/test-new-query)

;; 6. Add scope-capture to debug data if needed
(sc.api/spy result)
(k/run 'dm.db-test/test-new-query)
(sc.api/defsc ep-id)
;; inspect data...

;; 7. Run all db tests to ensure no breakage
(k/run 'dm.db-test)

;; 8. Run full test suite
(k/run-all)
```

---

## Comparing REPL Testing Approaches

| Approach | Speed | Feedback | Isolation | Use Case |
|----------|-------|----------|-----------|----------|
| `(k/run :unit)` | Fast | Immediate | Single suite | Most development |
| `(k/run 'namespace)` | Fast | Immediate | One namespace | Feature work |
| `(k/run 'namespace/test)` | Very fast | Immediate | One test | Debugging |
| `(k/run-all)` | Slower | Complete | All tests | Before commit |
| `bash bb test` | Slower | Separate window | All tests | CI/validation |

---

## Common Kaocha REPL Patterns

### Pattern 1: Watch and Iterate

```clojure
;; Keep running a specific test while developing
(defn test-my-feature []
  (k/run 'my.feature-test {:kaocha/focus ["test-main-logic"]}))

;; Then each time you change code:
(test-my-feature)

;; Reload if code changed
(require 'my.feature :reload)
(test-my-feature)
```

### Pattern 2: Conditional Testing

```clojure
;; Test with different configurations
(let [result (k/run :unit {:kaocha/focus [".*" ]})]
  (if result
    (println "Core tests passed, running integration tests...")
    (println "Core tests failed, skipping integration")))
```

### Pattern 3: Debugging with Spy

```clojure
;; Add spy to failing test
(t/deftest test-complex-feature
  (let [data (compute-something)
        result (process data)]
    (is (= expected (sc.api/spy result)))))

;; Run test to capture
(k/run 'my-test)

;; Restore and debug
(sc.api/defsc ep-id)
(keys result)  ; inspect structure
```

### Pattern 4: Performance Testing

```clojure
;; See which tests are slowest
(k/run :unit {:kaocha/plugins [:kaocha.plugin/profiling]})

;; This shows slowest tests at the end of output
```

### Pattern 5: Focused Development Loop

```clojure
;; 1. Define a test-focused REPL function
(defn test-characters []
  (k/run :unit {:kaocha/focus [".*character.*"]
                :kaocha/fail-fast? true}))

;; 2. Use repeatedly during development
(test-characters)  ; red
;; Edit code
(test-characters)  ; green
;; Refactor
(test-characters)  ; still green
```

---

## Troubleshooting Kaocha REPL

### Tests Not Running?

```clojure
;; Check test plan to see what would run
(k/test-plan)

;; Verify test namespace exists
(require 'dm.db-test)

;; Check if tests are discoverable
(k/run 'dm.db-test)  ; explicit namespace
```

### Output Too Verbose?

```clojure
;; Run with minimal reporter
(k/run :unit {:kaocha/reporter :kaocha.report.progress/report})
```

### Want to See Test Plan?

```clojure
;; Print configuration before running
(k/run :unit {:print-config? true})

;; Or just inspect
(k/test-plan {:suite :unit})
```

### Need to Reload Code?

```clojure
;; Kaocha automatically reloads, but you can force it
(require 'dm.db :reload)
(require 'dm.queries :reload)

;; Then run tests
(k/run 'dm.db-test)
```

---

## Integration with Other Tools

### With scope-capture (Debugging)

```clojure
(require '[kaocha.repl :as k]
         '[sc.api])

;; Add spy to code, run test
(k/run 'my-test)

;; Restore context
(sc.api/defsc ep-id)

;; Inspect and test
```

### With clojure_eval (from main prompt)

```clojure
clojure_eval(code: """
(require '[kaocha.repl :as k])
(k/run :unit {:kaocha/fail-fast? true})
""")
```

### With bb test (Command line)

```bash
# Run from REPL:
(k/run :unit)

# Or from command line:
bb test
```

Both work, use REPL for development, CLI for CI.

---

## Key Differences: REPL vs CLI

| Aspect | REPL | CLI |
|--------|------|-----|
| **Speed** | Instant | Startup time ~3-5s |
| **State** | Persists between runs | Fresh each time |
| **Control** | Programmatic, flexible | Command-line arguments |
| **Feedback** | Immediate | Separate window |
| **Use Case** | Development, debugging | CI, validation |

**Recommendation**: Use REPL during development (`k/run`), use CLI for final validation (`bb test`)

---

## Quick Reference

```clojure
;; Load Kaocha
(require '[kaocha.repl :as k])

;; Run everything
(k/run-all)

;; Run test suite
(k/run :unit)

;; Run namespace
(k/run 'dm.db-test)

;; Run one test
(k/run 'dm.db-test/test-db)

;; With options
(k/run :unit
  {:kaocha/focus [".*character.*"]
   :kaocha/fail-fast? true})

;; Inspect without running
(k/test-plan)

;; Check result
(let [result (k/run :unit)]
  (if result "✓ Passed" "✗ Failed"))
```

---

## Resources

- **GitHub**: https://github.com/lambdaisland/kaocha
- **Documentation**: https://cljdoc.org/d/lambdaisland/kaocha/CURRENT
- **REPL Guide**: https://cljdoc.org/d/lambdaisland/kaocha/CURRENT/doc/5-running-kaocha-from-the-repl
- **Focusing/Skipping**: https://cljdoc.org/d/lambdaisland/kaocha/CURRENT/doc/6-focusing-and-skipping

---

## Key Insight

**Kaocha transforms testing from a batch operation (run all, wait, check output) into an interactive conversation (what if I change this? run test, see result).**

This is the Clojure way: tight feedback loops, interactive development, immediate validation.
