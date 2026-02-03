default:
    @just --list

# Run the app
run:
    clj -J-Dadnotare.malli-dev=true -M:dev -m adnotare.main

# Run REPL
repl:
    clj -M:dev

# Run cljfmt fix
fmt:
    clj -M:dev -m cljfmt.main fix deps.edn src/ test/

# Lint via clj-kondo
lint:
    clj -M:dev -m clj-kondo.main --lint src test

test:
    bin/kaocha

test-cov:
    bin/kaocha --plugin cloverage

# Lint and format
pre-pr:
    @just lint
    @just fmt
    @just test
