# URL Shortener

Author: Yuanchun Shen

- Compile

With shell

```shell
mvn clean compile assembly:single
```

With python scripts

```shell
cd scripts
inv build
```

- Run locally

Run 3 servers in background with shell

```shell
mkdir -p generates/node1
mkdir -p generates/node2
mkdir -p generates/node3
cd generates/node1/
java -jar ../../target/urlshortener-1.0-SNAPSHOT-jar-with-dependencies.jar  App -p 8080 -i localhost -d :memory: -l 9080 -s localhost:9081,localhost:9082 -init ../../debug_workload_dataset.csv &
cd ../../
cd generates/node2/
java -jar ../../target/urlshortener-1.0-SNAPSHOT-jar-with-dependencies.jar  App -p 8081 -i localhost -d :memory: -l 9081 -s localhost:9080,localhost:9082 -init ../../debug_workload_dataset.csv &
cd ../../
cd generates/node3/
java -jar ../../target/urlshortener-1.0-SNAPSHOT-jar-with-dependencies.jar  App -p 8082 -i localhost -d :memory: -l 9082 -s localhost:9080,localhost:9081 -init ../../debug_workload_dataset.csv &
cd ../../
```

Using python scripts

```shell
# run single server
inv runSingleLocal
# run two servers
inv runTwoLocal
# run 10 servers
inv runNLocal 10
```

