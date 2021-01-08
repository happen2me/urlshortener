# Project root directory
PROJECT_ROOT = "/home/sakamoto/CBDP/urlshortener"

# Absolute path to the initial dataset for correctness testing
CORRECTNESS_TEST_INITIAL_DATASET = "/home/sakamoto/CBDP/cloud-based-data-processing-url-shortener-client/debug_workload_dataset.csv"

# Directory of the client GitLab checkout. Can also be a client build but then the task buildClient won't work.
CLIENT_DIRECTORY = "/home/sakamoto/CBDP/cloud-based-data-processing-url-shortener-client/"

# Packaged client to avoid rebuilding the client on each run
# CLIENT_DIRECTORY = "/home/per/workspace/YCSB-client/ycsb-0.18.0-SNAPSHOT"


# Four IPs for Amazon EC2 instances
AWS_IPS = ["52.3.251.114", "18.204.13.191", "100.25.41.111", "35.153.57.246"]
# Your Amazon key
AWS_PEM_FILE = "/home/sakamoto/.ssh/aws-edu.pem"


# Names of different workloads
CORRECTNESS_TEST_WORKLOAD = "workloads/workloadshortener_correctness_test"
DEBUG_WORKLOAD = "workloads/workloadshortener_debug"
CACHE_WORKLOAD = "workloads/workloadshortener_cache_test"

RUN_WORKLOAD = CACHE_WORKLOAD