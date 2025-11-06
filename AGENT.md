# Agent Guide

This guide outlines the best practices for an AI agent to interact with this project.

## Primary Interaction: `dev/dev.clj`

The most effective way to interact with this project is by using the `clojure_eval` tool to call functions defined in `dev/dev.clj`. This provides a direct and powerful way to build, test, and manage the application.

## REPL Workflow

**IMPORTANT**: Before using any `dev` functions, you MUST switch to the `dev` namespace first.

### Switching to dev namespace:

```clojure
(fast-dev)
```

This function loads and switches to the `dev` namespace. Do NOT manually require and switch namespaces with `(require 'dev)` and `(in-ns 'dev)`.

### Available `dev` Functions

After calling `(fast-dev)`, you can use these functions:

- `(refresh)`: Reloads all source code files in `src`, `dev`, and `test` directories. Use this after making changes to the code.
- `(all-tests)`: Runs all tests in the project using Kaocha.
- `(lint)`: Lints the codebase using `clj-kondo` and prints the results.
- `(reset)`: Resets the system, reloading all code and restarting components. **This is the most common command** - it combines refresh + restart.
- `(start)`: Starts the system components.
- `(stop)`: Stops the system components.
- `system`: Access the running system map (note: it's a var, not a function, so use without parentheses).

## Running Tests from the REPL

The project uses Kaocha for testing (configured in `tests.edn`). The `kaocha.repl` namespace is already loaded as `k` in the dev namespace.

### Quick Test Commands

After calling `(fast-dev)`:

**Run all tests:**
```clojure
(all-tests)
;; or directly
(k/run-all)
```

**Run the `:unit` test suite:**
```clojure
(k/run :unit)
```

**Run a specific test namespace:**
```clojure
(k/run 'dm.db-test)
(k/run 'dm.api-test)
```

**Run a specific test var:**
```clojure
(k/run 'dm.db-test/next-temp-id-test)
```

**Run tests in current namespace:**
```clojure
(k/run *ns*)
;; or just
(k/run)
```

### TDD Workflow

Redefine and immediately run a test:

```clojure
(k/run
  (deftest my-new-test
    (is (= 2 (+ 1 1)))))
```

### Configuration Options

Pass config as a map:

```clojure
(k/run :unit {:kaocha/fail-fast? true})
```

### Diagnostic Functions

**View test configuration:**
```clojure
(k/config)
```

**Inspect test plan:**
```clojure
(k/test-plan)
```

## Babashka Tasks

The project also provides Babashka tasks for common operations. These can be executed using the `bash` tool (e.g., `bash "bb test"`).

- `bb test`: Runs all project tests using Kaocha.
- `bb lint`: Lints the codebase with `clj-kondo`.
- `bb ci`: Runs a full continuous integration check, including linting and testing.
- `bb fmt`: Formats the Clojure code.

## Project Overview

- **Purpose**: A D&D 5e style RPG character management application for Dungeon Masters.
- **Architecture**: A component-based Clojure application using `deps.edn`.
- **Technologies**:
    - **Web Server**: `http-kit`
    - **Routing**: `reitit` with `liberator`
    - **Database**: SQLite via `next-jdbc`
    - **Migrations**: `ragtime.next-jdbc`
    - **SQL Queries**: `HugSQL`
    - **HTML Generation**: `hiccup` with HTMX
    - **Component Lifecycle**: `com.stuartsierra/component`

## Working with Database Migrations

### Migration File Structure

Migrations are stored in `resources/migrations/` with the following naming convention:
- `NNN-description.up.sql` - Migration to apply
- `NNN-description.down.sql` - Migration to rollback

### SQL Statement Separators

Ragtime requires SQL statements to be separated with `-- ;;` on its own line:

```sql
CREATE TABLE species (...);
-- ;;

CREATE TABLE classes (...);
-- ;;
```

### Testing Migrations

After creating or modifying migrations:

1. **Switch to dev namespace**: `(fast-dev)`
2. **Reset the system**: `(reset)` - This will:
   - Reload all code
   - Stop the old system
   - Start a new system (which runs migrations automatically)
   - Start the HTTP server

3. **Verify migrations ran**:
```clojure
(require '[next.jdbc :as jdbc])
(let [db (dm.db/get-conn (:database system))]
  (jdbc/execute! db ["SELECT name FROM sqlite_master WHERE type='table' ORDER BY name"]))
```

4. **Check the system state**:
```clojure
system  ;; Note: no parentheses, it's a var not a function
```

### Component Lifecycle Pattern

When updating components:
- Components implement `com.stuartsierra.component/Lifecycle`
- Must have `start` and `stop` methods
- Use idempotent checks (e.g., `(if (:datasource self) self ...)`) to avoid double-starting
- Print debug messages during start/stop for visibility
- Clean up resources in `stop` (e.g., close connections, dissoc state)

### Component Dependencies

Components declare dependencies using `component/using`:

```clojure
(component/system-map
  :database (map->Database {...})
  :migrations (component/using
                (map->Ragtime {...})
                {:database :database})  ; depends on :database
  :http-server (component/using
                 (map->HTTPKit {...})
                 {:database :database}))
```

The Ragtime component must start AFTER Database but BEFORE HTTPKit.

### HugSQL Query Organization

SQL queries are organized by table in `resources/sql/`:
- One file per table (e.g., `species.sql`, `characters.sql`)
- HugSQL comments define query functions:
  - `-- :name query-name :? :*` - Select many
  - `-- :name query-name :? :1` - Select one
  - `-- :name query-name! :! :n` - Insert/Update/Delete
  - `-- :name query-name! :<! :1` - Insert returning generated key
- Always include `:doc` comments

### Database Schema Design Patterns

For D&D/RPG applications:
- Use **enumeration tables** with string primary keys for game data (species, classes, etc.)
- Store metadata in enum tables (descriptions, stats, proficiencies)
- Use foreign keys for referential integrity
- Use composite primary keys for many-to-many relationships
- Add `CHECK` constraints for game rules (e.g., attribute values 0-30)
- Use `ON DELETE CASCADE` for dependent data