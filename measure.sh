#!/usr/bin/env bash

set -e # exit if any command fails

cd local-cluster/
docker-compose down
docker-compose up --scale taskmanager=2 --build -d

echo "Sleeping for a while"
sleep 10

cd ..
./deploy.sh

open http://localhost:3000

echo "Sleeping for 180s to warm-up"
sleep 180

JOB_ID=$(docker exec -it local-cluster_jobmanager_1 curl localhost:8081/jobs/ | jq '.jobs[].id' | tr -d '"')
VERTICE_ID=$(docker exec -it local-cluster_jobmanager_1 curl localhost:8081/jobs/$JOB_ID | jq ".vertices[0].id" | tr -d '"')
HOST_ID=$(docker exec -it local-cluster_jobmanager_1 curl localhost:8081/jobs/$JOB_ID/vertices/$VERTICE_ID | jq ".subtasks[0].host" | tr -d '"' | awk -F ":" '{print $1}')

echo "First vertice is running on host $HOST_ID. Killing this host now"

docker stop $HOST_ID

echo "Sleeping for 5 minutes until cluster will be stopped"
sleep 300

echo "Stopping cluster"
cd local-cluster
docker-compose stop