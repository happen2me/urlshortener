# kill old servers
if [[ "$OSTYPE" == "msys"* ]]; then
    pid1=$(netstat -ano | findstr 8080 | awk '{print $5}') && taskkill -PID $pid1 -F
    pid2=$(netstat -ano | findstr 8081 | awk '{print $5}') && taskkill -PID $pid2 -F
    pid3=$(netstat -ano | findstr 8082 | awk '{print $5}') && taskkill -PID $pid3 -F 
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    fuser -k -n tcp 8080
    fuser -k -n tcp 8081
    fuser -k -n tcp 8082
fi

# run 3 servers in background
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