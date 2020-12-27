# Project root directory
PROJECT_ROOT = "/home/per/workspace/CloudBasedDataProcessingURLShortener"

# Absolute path to the initial dataset for correctness testing
CORRECTNESS_TEST_INITIAL_DATASET = "/home/per/workspace/YCSB/urls.csv"

# Directory of the client GitLab checkout. Can also be a client build but then the task buildClient won't work.
CLIENT_DIRECTORY = "/home/per/workspace/YCSB/"

# Packaged client to avoid rebuilding the client on each run
# CLIENT_DIRECTORY = "/home/per/workspace/YCSB-client/ycsb-0.18.0-SNAPSHOT"


# Four IPs for Amazon EC2 instances
AWS_IPS = ["35.157.231.151", "52.59.242.196", "3.123.153.219", "18.193.222.13"]
# Your Amazon key
AWS_PEM_FILE = "/home/per/.ssh/cloud-based-my-account.pem"


# Names of different workloads
CORRECTNESS_TEST_WORKLOAD = "workloads/workloadshortener_correctness_test"
DEBUG_WORKLOAD = "workloads/workloadshortener_debug"
CACHE_WORKLOAD = "workloads/workloadshortener_cache_test"

RUN_WORKLOAD = CACHE_WORKLOAD