#!/usr/bin/env bash
set -e # exit if any command fails

FLINK_DIR=~/dev/flink-1.6.1/bin

mvn clean package -U -Pbuild-jar

# Cancel all running jobs
${FLINK_DIR}/flink list | grep RUNNING | awk '{print $4}' | xargs ${FLINK_DIR}/flink cancel

# Run newly built job
JOB_ID=$(${FLINK_DIR}/flink run -c me.florianschmidt.replication.baseline.StreamingJob --detached target/flink-fault-tolerance-baseline-1.0-SNAPSHOT.jar | grep submitted | awk '{print $7}')

# Open running job in browser
open http://localhost:8081/#/jobs/${JOB_ID}
