# Huggingface Spark Cluster

## Start Cluster

```shell
# Builds the docker image the docker-coompose.yml file depends on.
sh build-docker-image.sh
sudo docker-compose up -d
```

## Prerequisites

### App

The `docker-compose.yml` file depends on the code in the `../python-translator/` directory, which defines the Huggingface
translator. Spark jobs are intended to be executed using the `../python-translator/translator.py` script.

### Model

The `../python-translator/translator.py` script that is used for the Spark jobs requires a pytorch model to be in the
`../raw-files/t5.1.1.lm100k.large/` directory so that it can be loaded and used for text generation.

## Execute Job Manually

```shell
# Connect to a worker
sudo docker exec -it spark-worker-1 bash

# Submits the job on the connected worker container
/opt/spark/bin/spark-submit \
--master spark://spark-master:7077 \
--total-executor-cores 2 \
--driver-memory 1G \
--executor-memory 8G \
/opt/spark-apps/translator.py \
"concert_singer | select t1.concert_name from concert as t1" \
"Get concerts with the largest stadiums. | concert_singer | stadium : stadium_id, location, name, capacity, highest, lowest, average | singer : singer_id, name, country, song_name, song_release_year, age, is_male | concert : concert_id, concert_name, theme, stadium_id, year | singer_in_concert : concert_id, singer_id"
```

## Drawbacks

This experiment was abandoned because it didn't seem possible to load the Torch model a single time and then use it
for all of our Spark jobs. This meant that the model needed to be loaded into memory for each text generation run and
therefore isn't viable. We need a cluster solution that can hold the model in memory and use it for subsequent runs.
