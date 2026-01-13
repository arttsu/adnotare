default:
    @just --list

# Run the app
run:
    clj -M:dev -m adnotare.main

# Run REPL
repl:
    clj -M:dev

# Run cljfmt fix
fmt:
    clj -M:dev -m cljfmt.main fix deps.edn src/

# Lint via clj-kondo
lint:
    clj -M:dev -m clj-kondo.main --lint src
