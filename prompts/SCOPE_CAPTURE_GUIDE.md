# scope-capture Debugging Guide

> Quick Review of scope-capture (vvvvalvalval/scope-capture)

## What is scope-capture?

**scope-capture** is a Clojure debugging library that solves one fundamental problem:

> How do I debug a function when it's hard to recreate the exact runtime conditions?

### The Problem It Solves

```clojure
;; Your code fails in production with some complex context:
(defn process-order [db order]
  (let [user (fetch-user db (:user-id order))
        items (fetch-items db (:item-ids order))
        tax (calculate-tax (:region order) items)
        total (+ (:subtotal order) tax)]
    ;; Result is wrong! But how do I debug this?
    ;; All these values depend on real DB state...
    (save-order db total)))

;; To debug, you'd need to:
;; 1. Connect to production DB
;; 2. Fetch the exact same order
;; 3. Fetch the exact same user
;; 4. Fetch the exact same items
;; 5. Recreate the exact state...
;; This is tedious and error-prone!
```

### The Solution

```clojure
;; Just wrap the problem with spy:
(defn process-order [db order]
  (let [user (fetch-user db (:user-id order))
        items (fetch-items db (:item-ids order))
        tax (calculate-tax (:region order) items)
        total (+ (:subtotal order) tax)]
    (sc.api/spy total)))  ; ← Just add this!

;; When it runs:
(process-order my-db my-order)

;; Output:
;; SPY [23 -5] /path/to/file.clj:42
;;   At Execution Point 23, saved scope with locals [db order user items tax total]

;; Now in REPL, restore the EXACT state:
(sc.api/defsc 23)  ; 23 is the Execution Point

;; All these are now available in REPL:
db      ; => the actual database object
order   ; => the exact order that failed
user    ; => the exact user from DB
items   ; => the exact items from DB
tax     ; => the calculated tax
total   ; => the wrong result

;; Now you can debug interactively:
(calculate-tax (:region order) items)  ; test the tax calc
(keys user)  ; inspect user structure
(type total)  ; what type did we get?
```

---

## Installation

scope-capture is already in your project's dev dependencies. Just require it:

```clojure
(require 'sc.api)
```

---

## The Three Main APIs

### 1. `sc.api/spy` - Capture Context

**Wraps an expression and saves all local variables when executed.**

```clojure
;; Basic usage
(sc.api/spy expression)

;; Example
(defn my-func [x y z]
  (let [a (+ x y)
        b (* a z)
        result (- b a)]
    (sc.api/spy result)))  ; ← Captures when this line runs

;; With options
(sc.api/spy
  {:sc/dynamic-vars [*out* *in*]}  ; Optionally capture dynamic vars
  (some-expression))
```

**Output** (in REPL or logs):
```
SPY <-3> /path/to/file.clj:42
  At Code Site -3, will save scope with locals [x y z a b result]

SPY [7 -3] /path/to/file.clj:42
  At Execution Point 7 of Code Site -3, saved scope with locals [x y z a b result]
SPY [7 -3]
result
=>
42
```

Key points:
- Code Site ID: `-3` (permanent, assigned at compile time)
- Execution Point ID: `7` (changes each time code runs)
- You use the **Execution Point ID** to restore context

### 2. `sc.api/defsc` - Restore as Global Vars

**Recreates the captured scope by `def`-ing each variable as a global.**

```clojure
;; After spy captured execution point 7:
(sc.api/defsc 7)

;; Now in your REPL, all locals are global:
a      ; => original value of a
b      ; => original value of b
result ; => original value of result

;; You can now:
(+ a b)      ; => expression using the real values
(type result) ; => inspect the type

;; The author's preferred way for experimentation
```

**Advantages:**
- Easy to use with editor "evaluate form at REPL" command
- Quick exploration and experimentation
- All values immediately available

**Caveats:**
- Overwrites global Vars with same name as locals
- Won't work if global Var was `defonce`'d
- Uses `def`, so values persists until you `undefsc`

### 3. `sc.api/letsc` - Restore as Local Vars

**Recreates the captured scope using `let` (cleaner, more isolated).**

```clojure
;; Evaluate expression in the captured context:
(sc.api/letsc 7
  (+ a b))  ; => uses the real a and b values

;; Or get multiple values at once:
(sc.api/letsc 7
  [a b result])  ; => [val1 val2 val3]

;; For ClojureScript (needs both IDs):
(sc.api/letsc [7 -3]
  (+ a b))
```

**Advantages:**
- More isolated (doesn't pollute global namespace)
- Cleaner (no need to `undefsc` afterward)
- Better for ClojureScript

**Use when:** You want to test a specific expression without permanently defining globals.

---

## Complete Debugging Workflow

### Scenario: Wrong Database Query Results

Your character creation query returns incomplete data. You need to debug it.

#### Step 1: Add spy to suspicious code

```clojure
;; src/dm/queries.clj
(defn get-character-with-details [db {:keys [id]}]
  (let [character (get-character-by-id db {:id id})
        attributes (get-character-attributes db {:character_id id})
        skills (get-character-skills db {:character_id id})
        result (assoc character
                      :attributes attributes
                      :skills skills)]
    (sc.api/spy result)))  ; ← Just add this!
```

#### Step 2: Run your code (could be production, tests, or REPL)

```clojure
(require '[dm.queries :as q])
(require '[dm.db :as db])

(q/get-character-with-details my-db {:id 123})

;; Output in REPL or logs:
;; SPY <-15> /home/ivan/dev/dm/src/dm/queries.clj:92
;;   At Code Site -15, will save scope with locals [db id character attributes skills result]
;; SPY [42 -15] /home/ivan/dev/dm/src/dm/queries.clj:92
;;   At Execution Point 42 of Code Site -15, saved scope with locals [db id character attributes skills result]
;; SPY [42 -15]
;; result
;; =>
;; {:id 123, :name "Aragorn", ...}
```

#### Step 3: Restore the context in REPL

```clojure
(require 'sc.api)

;; Restore all captured variables
(sc.api/defsc 42)  ; 42 is the Execution Point ID

;; Now you have all the real values from the failing execution:
character   ; => the actual character record from DB
attributes  ; => the actual attributes that were fetched
skills      ; => the actual skills that were fetched
db          ; => the actual database connection!
id          ; => the exact ID that was queried

;; Inspect the results
(keys character)      ; what fields?
(count attributes)    ; how many attributes?
(empty? skills)       ; are skills empty?

;; Test hypothesis about the bug
(type attributes)     ; is it a sequence?
(first attributes)    ; what does one look like?

;; Check if the issue is in assembly:
{:attributes attributes, :skills skills}

;; Or in the query:
(q/get-character-attributes db {:character_id id})  ; Does it match?
```

#### Step 4: Test your fix in REPL

```clojure
;; Once you've identified the problem, test the fix:

;; If problem is in get-character-attributes, fix it:
(defn get-character-attributes-fixed [db {:keys [character_id]}]
  ;; ... improved logic ...
)

;; Test it with the real data:
(get-character-attributes-fixed db {:character_id id})

;; Verify it matches expected structure:
(keys (first (get-character-attributes-fixed db {:character_id id})))
```

#### Step 5: Save the fix to file

```clojure
clojure_edit(
  file_path: "src/dm/queries.clj",
  form_type: "defn",
  form_identifier: "get-character-attributes",
  operation: "replace",
  content: "(defn get-character-attributes [db {:keys [character_id]}]
  ;; improved implementation
  ...)")
```

#### Step 6: Clean up

```clojure
;; Remove spy from code
clojure_edit(
  file_path: "src/dm/queries.clj",
  form_type: "defn",
  form_identifier: "get-character-with-details",
  operation: "replace",
  content: "(defn get-character-with-details [db {:keys [id]}]
  (let [character (get-character-by-id db {:id id})
        attributes (get-character-attributes db {:character_id id})
        skills (get-character-skills db {:character_id id})
        result (assoc character
                      :attributes attributes
                      :skills skills)]
    result))  ; ← Remove spy")

;; Undefine the globals you created
(sc.api/undefsc)
```

---

## Common Patterns

### Pattern 1: Debug a deep function call

```clojure
;; Problem: update-character returns wrong data
;; But you don't know which step is wrong

;; Solution: wrap the result
(defn update-character [db id updates]
  (let [existing (get-character db id)
        validated (validate-updates existing updates)
        merged (merge existing validated)
        saved (save-character db merged)]
    (sc.api/spy saved)))  ; Capture result

;; In REPL after it runs:
(sc.api/defsc ep-id)
existing    ; what was in DB?
validated   ; what did validation produce?
merged      ; what was the merge result?
saved       ; what was saved?

;; Test each step:
(validate-updates existing updates)  ; produces what?
(merge existing validated)            ; is merged correct?
```

### Pattern 2: Debug with complex state

```clojure
;; Problem: character creation works sometimes, fails others
;; Depends on complex state (loaded rules, config, etc.)

;; Solution: capture everything at the failure point
(defn create-character [db spec]
  (let [validated (validate-spec spec)
        species-rules (load-species-rules (:species validated))
        class-rules (load-class-rules (:class validated))
        attributes (initialize-attributes species-rules)
        result (assoc validated :attributes attributes)]
    (sc.api/spy result)))

;; In REPL:
(sc.api/defsc ep-id)

;; Now you have ALL the context:
spec            ; what was passed in
species-rules   ; what rules were loaded?
class-rules     ; were class rules correct?
attributes      ; how were attributes initialized?

;; You can test each piece independently:
(load-species-rules "elf")  ; works?
(initialize-attributes species-rules)  ; correct?
```

### Pattern 3: Inspect intermediate values

```clojure
;; Problem: need to understand how a calculation works

;; Solution: add spy at various points
(defn calculate-attack [character monster]
  (let [ability (get-in character [:attributes :strength])
        modifier (calculate-modifier ability)
        base-attack (+ ability modifier)
        bonuses (get-combat-bonuses character monster)
        final-attack (sc.api/spy (+ base-attack bonuses))]
    final-attack))

;; In REPL:
(sc.api/defsc ep-id)

;; See each calculation:
ability        ; base strength
modifier       ; how much was modified?
base-attack    ; before bonuses
bonuses        ; what bonuses were applied?
final-attack   ; final result

;; Very helpful for understanding complex formulas
```

---

## When to Use scope-capture

### ✅ Perfect Use Cases

- **Bugs with complex state dependencies** - Hard to manually recreate
- **Production issues** - Can't run locally
- **Integration bugs** - Depend on specific DB state, external APIs
- **Flaky tests** - Only fail sometimes with certain data
- **Onboarding** - New developers understand how code works
- **Exploration** - Understanding someone else's code

### ❌ Not Suitable For

- **Static errors** - Compilation/syntax errors (won't execute)
- **Runtime errors** - Code that throws exceptions (won't reach spy)
- **Logic that never runs** - Dead code (never gets to spy)

---

## Pro Tips

### Tip 1: Use `defsc` for exploration, `letsc` for isolation

```clojure
;; During debugging - use defsc, very convenient:
(sc.api/defsc ep-id)
(+ a b c)
(keys result)

;; When done - use undefsc to clean up:
(sc.api/undefsc)

;; OR - use letsc for single expressions:
(sc.api/letsc ep-id (+ a b c))  ; no cleanup needed
```

### Tip 2: Capture intermediate values

```clojure
;; Multiple spy points to narrow down where bug is:
(defn complex-func [a b c]
  (let [step1 (sc.api/spy (process1 a b))]
    (let [step2 (sc.api/spy (process2 step1 c))]
      (sc.api/spy (process3 step2)))))

;; Then you can restore any of them:
(sc.api/defsc ep-for-step1)  ; inspect after step1
(sc.api/defsc ep-for-step2)  ; inspect after step2
(sc.api/defsc ep-for-step3)  ; inspect after step3
```

### Tip 3: Capture dynamic vars if needed

```clojure
;; If you need to capture *out*, *err*, or custom dynamic vars:
(sc.api/spy
  {:sc/dynamic-vars [*out* *print-fn* my.app/*config*]}
  (some-operation))

;; Then restore:
(sc.api/defsc ep-id)
*out*  ; what was bound to *out*?
my.app/*config*  ; what config was active?
```

### Tip 4: Combine with REPL workflow

```clojure
;; Full debugging workflow:
1. Add (sc.api/spy ...) to code
2. Run code (could be tests, REPL, even production!)
3. (require 'sc.api)
4. (sc.api/defsc execution-point-id)
5. Explore/test in REPL
6. Once you understand the bug, fix it
7. (sc.api/undefsc)
8. Remove the spy from code
9. Test the fix
```

---

## Common Pitfalls

### ❌ Forgetting to reload after adding spy

```clojure
;; WRONG:
(defn foo [x] (+ x 1))
;; Now add spy in editor
;; Then in REPL, call without reloading:
(require 'myapp)
(foo 5)  ; Won't capture! Code not reloaded

;; RIGHT:
(require '[myapp :reload])  ; Force reload
(foo 5)  ; Now spy will execute
```

### ❌ Trying to capture code that never runs

```clojure
;; WRONG:
(if (never-true-condition)
  (sc.api/spy result))  ; Never executes, nothing captured

;; RIGHT:
(let [result (complex-calculation)]
  (if (some-condition result)
    (do
      (sc.api/spy result)  ; Place it where code definitely runs
      result)
    result))
```

### ❌ Not using execution point ID correctly

```clojure
;; WRONG:
(sc.api/defsc -5)  ; Code Site ID, not Execution Point ID!

;; RIGHT:
(sc.api/defsc 42)  ; Execution Point ID from the log output
```

### ❌ Forgetting to undefsc

```clojure
;; After debugging:
(sc.api/defsc ep-id)
;; ... lots of experimentation ...
;; Now your REPL is polluted with temporary defs!

;; CLEAN UP:
(sc.api/undefsc)
;; OR just start fresh REPL session
```

---

## Integration with dm Project

In the dm project, scope-capture is perfect for:

### Debugging Query Issues

```clojure
;; In src/dm/queries.clj
(defn get-character-with-details [db {:keys [id]}]
  (let [char (get-character-by-id db {:id id})
        attrs (get-character-attributes db {:character_id id})]
    (sc.api/spy (assoc char :attributes attrs))))

;; Then in REPL:
(require '[dm.queries :as q])
(require 'sc.api)

(q/get-character-with-details db {:id 1})
;; Output shows execution point ID...

(sc.api/defsc 23)  ; restore context
db        ; see the actual db connection
id        ; see what ID was queried
char      ; see what character was fetched
attrs     ; see what attributes were returned
```

### Debugging Route Handlers

```clojure
;; In src/dm/routes.clj
(defn index [db]
  (liberator/resource
   :available-media-types ["text/html"]
   :handle-ok (fn [ctx]
     (let [result (dm.html/index-page db ctx)]
       (sc.api/spy result)))))  ; Capture what gets sent to browser

;; Debug in REPL after accessing route:
(sc.api/defsc ep-id)
db      ; inspect database state
result  ; what was actually sent?
```

### Debugging Component Lifecycle

```clojure
;; In src/dm/components.clj
(defn start [self]
  (if (:datasource self)
    self
    (let [spec (assoc dm.db/db-config :dbname (:db-path self))
          ds (jdbc/get-datasource spec)]
      (sc.api/spy
        {:message "Database started"
         :path (:db-path self)
         :spec spec
         :datasource ds})
      (assoc self :datasource ds))))

;; Then debug in REPL:
(sc.api/defsc ep-id)
spec  ; what was the database config?
ds    ; was datasource created correctly?
```

---

## Resources

- **GitHub**: https://github.com/vvvvalvalval/scope-capture
- **Documentation**: https://cljdoc.org/d/vvvvalvalval/scope-capture/CURRENT
- **Tutorial**: https://github.com/vvvvalvalval/scope-capture/blob/master/doc/Tutorial.md
- **Tips & Tricks**: https://github.com/vvvvalvalval/scope-capture/blob/master/doc/Tips-and-Tricks.md
- **Original Blog Post**: http://blog.cognitect.com/blog/2017/6/5/repl-debugging-no-stacktrace-required

---

## Quick Reference

| What | How |
|------|-----|
| **Capture context** | `(sc.api/spy expression)` |
| **Restore as globals** | `(sc.api/defsc execution-point-id)` |
| **Restore as locals** | `(sc.api/letsc execution-point-id expr)` |
| **Clean up globals** | `(sc.api/undefsc)` |
| **Get info about capture** | `(sc.api/ep-info execution-point-id)` |
| **Pause execution** | `(sc.api/brk value)` |
| **Resume paused code** | `(sc.api/loose-with! new-value)` |

---

**Key Insight**: scope-capture turns difficult debugging into a mechanical process. Instead of "How do I recreate this context?", you just say "Capture it for me!" and debug interactively in the REPL.
