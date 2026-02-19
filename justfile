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

# Run tests
test *args:
    bin/kaocha {{args}}

# Run tests and generate a coverage report
test-cov:
    bin/kaocha --plugin cloverage

# Build macOS uberjar with mac JavaFX deps
uber-mac:
    clj -T:build uber

# Build local unsigned macOS .app bundle (and zip)
package-mac:
    clj -T:build package-mac

# Zip existing app bundle for distribution
zip-mac:
    clj -T:build zip-mac

# Lint, format, and test
pre-pr:
    @just lint
    @just fmt
    @just test
