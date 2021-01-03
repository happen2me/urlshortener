"""
Usage
=====

Usage should be straightforward but it will be much easier to debug your work if you understand this script and are able to change it.
I recommend going through it. I used the `fabric` and the `invoke` python package to build it. These are rather slim packages, so reading
the documentation will not take long. The most important information below.

Connection: represents a SSH and SCP connection to another server. `Connection.run` runs a commandline command on this server.
LocalContext: represents a commandline on the local machine. Same interface.
@task: marks a method as a task which can be executed by `invoke`

Install dependencies
====================

   pip3 install fabric
   pip3 install invoke

Configuration
=============

Change variables in `configure.py`. Try not to change other things if possible. Thanks!

Run Tasks
==========

    invoke <taskname> <parameter1> ...

 Tasks
######

 The following tasks exists:
 1. build          - builds your server
 2. buildClient    - builds the client
 3. runSingleLocal - runs a single instance of your server against the client locally
 4. runTwoLocal    - runs two instances of your server against the client locally
 5. runNLocal <n>  - runs <n> instances of your server against the client locally
 6. run3AWS        - runs 3 instances of your server against the client in AWS
 7. collectLogFiles- collects the log files from the AWS machine after a run. For successful runs this is done automatically after
 `run3AWS` but maybe not all runs go that smoothly ;)

Log file locations
==================

The script will put the log files in the same folder as the script. There will be a subfolder for each local instance `local_<instance>`
and a subfolder `awsLogs` for remote runs. These are for debugging purposes, please remove them before handing in.
For local executions the folder will also contain the database files.
"""

from time import sleep

import fabric
from invoke import task, UnexpectedExit

from configure import PROJECT_ROOT, CORRECTNESS_TEST_INITIAL_DATASET, CLIENT_DIRECTORY, AWS_IPS, AWS_PEM_FILE, RUN_WORKLOAD, DEBUG_WORKLOAD, CORRECTNESS_TEST_WORKLOAD

# JAR file location after calling `build`, relative to the project root
JAR_FILE_LOCAL = "/target/urlshortener-1.0-SNAPSHOT-jar-with-dependencies.jar"  #### Adjust, try not change too much else.

##############################################################################################################
# Try not to adjust things below here but simply use this setup if possible. Makes correcting easier. Thanks!#
##############################################################################################################

# Can be set to lower numbers to ease debugging. In particular, 1 is helpful because then all requests will be sequential.
CLIENT_THREADS = 10

# If false, the client sends all requests to a single server which eases debugging - only works for local mode.
TARGET_ALL_SERVERS = True

#AWS constants
##############

AWS_USER = "ec2-user"

AWS_CLIENT_DIR = "/home/ec2-user/client"

AWS_SERVER_JAR_DIRECTORY = "/home/ec2-user/urlshortener/"
AWS_SERVER_JAR = AWS_SERVER_JAR_DIRECTORY + "urlshortener.jar"
AWS_INITIAL_DATASET = "/home/ec2-user/urls.csv"
AWS_HTTP_PORT = 7000
AWS_URL_SHORTENER_INTERNAL_PORT = 8000


#### Helpers
############

def kill_old_server_instances(connection_or_context, port):
  try:
    r = connection_or_context.run("sudo lsof -ti tcp:%d" % port, hide=True)
    if r.stdout != "":
      pid = int(r.stdout)
      connection_or_context.run("sudo kill -9 %d" % pid)
  except UnexpectedExit as e:
    if e.result.exited == 1:
      pass
      # print("No former server found")
    else:
      raise e


def dict_to_argument_string(d):
  return " ".join(["--%s=%s" % (k, v) for k, v in d.items()])


#### Local helpers
############

def run_client_local(c, workload, threads, servers):
  with c.cd(CLIENT_DIRECTORY):
    c.run("./bin/ycsb run urlshortener -P %s -threads %d -p servers=%s" % (workload, threads, servers), echo=True)


def run_server_instance_local(c, folder="", **arguments):
  c.run("mkdir -p %s" % folder)
  with c.cd(folder):
    argumentString = dict_to_argument_string(arguments)
    c.run("java -jar %s App %s" % (PROJECT_ROOT + JAR_FILE_LOCAL, argumentString), asynchronous=True)

  #sleep(1)

def get_local_server_addresses(ports):
  if TARGET_ALL_SERVERS:
    return ",".join(["http://localhost:%d/" % p for p in ports])
  else:
    return "http://localhost:%d/" % ports[0]


#### Local tasks
##########


@task
def build(c):
  with c.cd(PROJECT_ROOT):
    c.run("mvn package")


@task
def buildClient(c):
  with(c.cd(CLIENT_DIRECTORY)):
    c.run("mvn package -DskipTests")


@task
def runSingleLocal(c):
  arguments = {"port": 7001,
               "ip": "localhost",
               "database": "./aliases.sqlite",
               "initial_dataset": CORRECTNESS_TEST_INITIAL_DATASET,
               "msg_port": 9001}
  kill_old_server_instances(c, 7001)
  kill_old_server_instances(c, 7002)
  kill_old_server_instances(c, 7003)

  run_server_instance_local(c, "local_1", **arguments)
  # run_client_local(c, DEBUG_WORKLOAD, CLIENT_THREADS, get_local_server_addresses([7001]))


@task
def runTwoLocal(c):
  arguments = {"port": 7001,
               "ip": "localhost",
               "database": "./aliases.sqlite",
               "initial_dataset": CORRECTNESS_TEST_INITIAL_DATASET,
               "servers": "localhost:8002,localhost:8003",
               "msg_port": "8003"
               }
  kill_old_server_instances(c, 7001)
  kill_old_server_instances(c, 7002)
  kill_old_server_instances(c, 7003)

  run_server_instance_local(c, "local_1", **arguments)

  arguments["port"] = "7002"
  arguments["msg_port"] = "8002"
  run_server_instance_local(c, "local_2", **arguments)

  run_client_local(c, DEBUG_WORKLOAD, CLIENT_THREADS, get_local_server_addresses([7001, 7002]))


@task
def runNLocal(c, n):
  n = int(n)
  webserverPorts = list(map(lambda i: i, range(7001, 7001 + n)))
  internalPorts = list(map(lambda i: str(i), range(8001, 8001 + n)))

  default_arguments = {"ip": "localhost",
               "database": "./aliases.sqlite",
               "initial_dataset": CORRECTNESS_TEST_INITIAL_DATASET,
               "servers": "localhost:" + ",localhost:".join(internalPorts)
               }

  for i in reversed(list(range(n))):  # Attention I start my servers with ports in descending order! There's no special reason to it,
    # just the way I built my replication service.
    kill_old_server_instances(c, webserverPorts[i])

    default_arguments["port"] = webserverPorts[i]
    default_arguments["msg_port"] = internalPorts[i]
    run_server_instance_local(c, "local_" + str(i), **default_arguments)

  sleep(1)

  run_client_local(c, CORRECTNESS_TEST_WORKLOAD, CLIENT_THREADS, get_local_server_addresses(webserverPorts))


#### Remote Helpers
###################

def install_software(connection):
  connection.run("sudo yum -y install java")


def push_client(connection):
  connection.put(CLIENT_DIRECTORY + "distribution/target/ycsb-0.18.0-SNAPSHOT.tar.gz", "client.tar.gz")
  connection.run("rm -rf %s" % AWS_CLIENT_DIR)
  connection.run("mkdir -p %s" % AWS_CLIENT_DIR)
  connection.run("tar -zxvf client.tar.gz --directory %s" % AWS_CLIENT_DIR, hide=True)
  connection.run("mv %s/ycsb-0.18.0-SNAPSHOT/* %s" % (AWS_CLIENT_DIR, AWS_CLIENT_DIR))


def push_url_shortener(connection):
  connection.run("mkdir -p %s" % AWS_SERVER_JAR_DIRECTORY)
  connection.put(PROJECT_ROOT + JAR_FILE_LOCAL, AWS_SERVER_JAR)


def push_initial_dataset(connection):
  connection.put(CORRECTNESS_TEST_INITIAL_DATASET, AWS_INITIAL_DATASET)


def run_server_remote(connection, serverIPs):
  print("Running on server: %s" % connection.host)
  arguments = {"ip": connection.host,
               "database": "./aliases.sqlite",
               "initial_dataset": AWS_INITIAL_DATASET,
               "servers": ",".join(map(lambda ip: ip + ":" + str(AWS_URL_SHORTENER_INTERNAL_PORT), serverIPs)),
               "port": AWS_HTTP_PORT,
               "my_address": connection.host + ":" + str(AWS_URL_SHORTENER_INTERNAL_PORT)
               }

  argumentString = dict_to_argument_string(arguments)

  connection.run("java -jar %s %s" % (AWS_SERVER_JAR, argumentString), asynchronous=True, echo=False)

  sleep(2)


def run_client_remote(connection, workload, threads, serverList):
  servers = ",".join(list(map(lambda a: "http://%s:%d/" % (a, AWS_HTTP_PORT), serverList)))
  with connection.cd(AWS_CLIENT_DIR):
    connection.run("./bin/ycsb run urlshortener -P %s -threads %d -p servers=%s" % (workload, threads, servers))


def collect_log_files_internal(local_context, connection, serverNumber):
  local_context.run("mkdir -p awsLogs/%d" % serverNumber)

  connection.get("reference.log", "./awsLogs/%d/reference.log" % serverNumber)
  connection.get("other.log", "./awsLogs/%d/other.log" % serverNumber)


#### Remote Tasks
#################


@task
def run3AWS(localContext):
  clientIP = AWS_IPS[0]
  serverIPs = list(sorted(AWS_IPS[1:]))

  clientConnection = fabric.Connection(clientIP, user=AWS_USER, connect_kwargs={"key_filename": AWS_PEM_FILE})
  serverConnections = [fabric.Connection(ip, user=AWS_USER, connect_kwargs={"key_filename": AWS_PEM_FILE}) for ip in serverIPs]
  groupAll = fabric.ThreadingGroup(*AWS_IPS, user=AWS_USER, connect_kwargs={"key_filename": AWS_PEM_FILE})

  print("Installing software")
  install_software(groupAll)

  print("Pushing client")
  push_client(clientConnection)

  print("Pushing url shortener and initial dataset")
  for c in serverConnections:
    push_url_shortener(c)
    push_initial_dataset(c)

  print("Killing former instances running")
  for c in serverConnections:
    kill_old_server_instances(c, AWS_HTTP_PORT)

  for c in reversed(serverConnections):  # I start the largest address first. No particular reason but how I implemented my CommunicationService
    run_server_remote(c, serverIPs)

  sleep(4)  # Waiting for everything to set up if you need longer you probably do something wrong.

  print("Collecting log files")
  for i, c in enumerate(serverConnections):
    collect_log_files_internal(localContext, c, i)

  print("Running client remotely")
  run_client_remote(clientConnection, RUN_WORKLOAD, CLIENT_THREADS, serverIPs)

  sleep(2)  # Waiting for everything to settle after client ended. Normally not needed but better to collect some logs more.

  print("Collecting log files")
  for i, c in enumerate(serverConnections):
    collect_log_files_internal(localContext, c, i)


@task
def collectLogFiles(localContext):
  serverIPs = list(sorted(AWS_IPS[1:]))
  serverConnections = [fabric.Connection(ip, user=AWS_USER, connect_kwargs={"key_filename": AWS_PEM_FILE}) for ip in serverIPs]
  for i, c in enumerate(serverConnections):
    collect_log_files_internal(localContext, c, i)


# TODO allow to run different workloads
# TODO parallelize pushing of client software
# TODO only pushing the client if it changed, rscync?
# TODO provide ability to shutdown servers, with server side implementation
# TODO provide signal that all servers have been started and connected, with server side implemenation
# TODO use address:port for local and remote

# TODO on client sanitize server addresses parameter: should end on /, needs to start with "HTTP", should include a port

# TODO change client such that it is never requesting a key for which the write hasn't returned yet