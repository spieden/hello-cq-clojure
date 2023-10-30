# README

A minimal example of using the CloudQuery Java SDK to make a plugin in Clojure that acts as a source.

Usage:

```
> asdf current java
java            openjdk-20.0.2  /Users/spieden/hello-cq-clojure/.tool-versions
> clojure -J--add-opens=java.base/java.nio=ALL-UNNAMED -M:run-cq serve
Started server on localhost:7777

# .. in a separate shell

> cloudquery sync config.yml
Loading spec(s) from config.yml
Starting sync for: hello-cq-clojure (grpc@localhost:7777) -> [sqlite (cloudquery@cloudquery/sqlite)]
Sync completed successfully. Resources: 2, Errors: 0, Warnings: 0, Time: 0s

> sqlite3 db.sqlite "select * from my_table"
2023-10-30 02:53:53.402024Z|hello-cq-clojure|Hello CQ!
2023-10-30 02:53:53.402024Z|hello-cq-clojure|Love, Clojure
```
