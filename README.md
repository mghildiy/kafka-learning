### Topic
* sequence of message
* any kind og message format json, avro, protobuf, binary etc
* Partitioned data
* Can't be queried
* Use producers to write dats, consumer to read data
* Data is immutable
* Every message has an id, also called offset, which determines its order in the partition
* Data within a partition is ordered, but not across partitions
* Message id goes on incrementing as new messages are written
* Data is bydefault available for 1 week, but period is configurable
* A message can go to any of the partition by default, but can be controlled using partition key

### Producer
* Writes data to topic
* Many-to-many relationship with brokers
* If a kafka server fails, producer knows how to recover
* Producer decides which broker to write to(load balancing is involved), not kafka
* Producer can add a key to message(null by default, meaning load balancing decides partition) which is hashed to a partition
* Value in message can also be null
* Message contains: key(may be null), value(may be null), compression type, headers(key-value pairs,optional), partition+offset, timestamp
* Kafka partitioner takes in a message and determines the partition for it
* Kafka partitioner uses hashing algorithm(default algo is murmur2) to map a key to a partition

### Kafka message serializer
* Kafka only accepts bytes as input and only sends output as bytes
* Producers use serializers for converting messages to bytes
* Message serilalizers perform task of converting messages to bytes, and act on key and value
* All sorts of serlializers are available, string serializer, int serializer, avro serializer, protobuf serializer

### Consumer
* Pulls data from broker
* Many-to-many relationship with brokers
* If a kafka server fails, consumer knows how to recover
* Consumer knows which broker to read from
* Data is read in order from start to end
* Consumer uses deserializers to convert message bytes to objects
* A topic must never change datatype of messages as consumers are bound to the type, create new topic for it
* Consumer group is a group of consumers, and no 2 consumers from a group can read from same partition
* A consumer can read from multiple partitions, or consumers from diff groups can read from same partition
* Kafka consumers commit offsets read so far which kafka stores in a topic called __consumer__offsets,
  so that when a consumer comes back after failure it knows where to start reading from

### Three semantics of read based on offsets commit
* At least once: Consumer processes message and only then commits offset. So if failure happens during processing, 
  then consumer reads same message again.
* So its important that processing is idompotent so that multiple processing of same message doesnt impact system adversely
* At most once: Consumer commits offset as soon as it arrivees, so if processing results in consumer failure the message is lost
* Exactly once: Message processing and offset commit are part of same transaction(using transational API)

### Brokers
* Kafka cluster is a group of kaska servers called brokers
* Clients send connection request + metadata to any one of the broker, called bootstrap broker, and receives back all the information for cluster like list of brokers, topics, partitions
* Clients can then use this information to connect to any one of the broker it wants
* Each partition is replicated number of times decided by replication factor, thus providing availability and resiliency against failures of partitions
* So a broker can contain replicas of one or more partitions
* For a partition, one of the broker acts a leader, and a producer only writes to leader of the partition and data is 
  replicated to other replicas of that partition
* Similarly, consumer only reads from leader of the partition
* If a leader node fails, one of the replica is made leader
* Post Kafka version 2.4+, consumers can read from replicas closest, helping reducing latency

### Acknowledgement and durability
* Producers can opt for diff modes of acknowledgement
* ack = 0 means producer doesnt wait for any knoweldgement, and it may lead to data loss
* ack = 1 means producer waits for acknowledgement from partition leader, and it means limited data loss
* ack = all means producer waits for acknowledgement from all replicas, and it guarantees no data loss
* If a partition has N replicas then it can withstand failure of N-1 replicas

### Zookeeper
* A software used to manage kafka cluster
* Performs actions like leader selection for partitions, topology change notifications to brokers like addition/deletion of topics, broker added/removed etc
* Mandatory till 2.x, optional for 3.x, won't be there in 4.x
* Works with odd number of servers
* Works on master-slave model, with master for write and slaves for read
* Zookeeper doesn't store any consumer data after version 0.10, prior to which consumer offsets were stored in zookeeper
* As a client, never use zookeeper for configurations(was done in old versions of kafka), and directly connect to kafka brokers
* Zookeeper is being replaced by k-raft as part of KIP-500, as zookeeper is less secure than kafka and has scaling issues
  when number of partitions grow

### Starting kafka server with kraft(ie without zookeeper)		
https://www.conduktor.io/kafka/how-to-install-apache-kafka-on-windows-without-zookeeper-kraft-mode
* Generate cluster ID(only needed once): path-to-kafka-installation-root/bin/kafka-storage.sh random-uuid
* Format the storage(only needed once):  path-to-kafka-installation-root/kafka-storage.sh format -t <uuid-from-step-above> -c path-to-kafka-installation-root/config/kraft/server.properties
* Launch kafka server: path-to-kafka-installation-root/bin/kafka-server-start.sh path-to-kafka-installation-root/config/kraft/server.properties

## Kafka cli

### kafka topics:
* list topics: kafka-topics.sh --bootstrap-server <host:port> --list
* create topic: kafka-topics.sh --bootstrap-server <host:port> --create --topic <name of the topic> --partitions <number of partitions> --replication-factor <number of replicas>
* describe topics: kafka-topics.sh --bootstrap-server localhost:9092 --describe
* kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic <name of the topic>			
* delete a topic: kafka-topics.sh --bootstrap-server localhost:9092 --delete --topic <name of the topic>

###  kafka producer:
* without any property: kafka-console-producer.sh --bootstrap-server <host:port> --topic <name of the topic>
* with a property: kafka-console-producer.sh --bootstrap-server <host:port> --topic <name of the topic> --producer-property acks=all
* with key: kafka-console-producer.sh --bootstrap-server <host:port> --topic first_topic --property parse.key=true --property key.separator=:

### kafka-consumer:
* Read from end: kafka-console-consumer.sh --bootstrap-server <host:port> --topic <name of the topic>
* Read from beginning: kafka-console-consumer.sh --bootstrap-server <host:port> --topic <name of the topic> --from beginning
* As part of an explicit consumer group(if not mentioned, a default group is created):
* kafka-console-consumer.sh --bootstrap-server <host:port> --topic <name of the topic> --group <name of the group>

### kafka-consumer-groups:
* To list all consumer groups: kafka-consumer-groups.sh --bootstrap-server <host:port> --list
* Describe a group:
* kafka-consumer-groups.sh --bootstrap-server <host:port> --describe --group <group name>
* This command would give details about state of consumption of data from all the partitions in the topic consumer group is associated to.
* Log-end offset tells how many total data points are there in that aprtition, and current offset tells how far from start consumer group has read.

### Resetting offsets(there must be no consumer running):
* kafka-consumer-groups.sh --bootstrap-server <host:port> --group <group name> --reset-offsets --to-earliest --execute [--topic <topic name> OR --all-topics]
* This would reset the current offset to 0 for all partitions this group is reading from.
* kafka-consumer-groups.sh --bootstrap-server <host:port> --group <group name> --reset-offsets --shift-by <offset amount> --execute [--topic <topic name> OR --all-topics]
* This would shift current offset by given amount for all partitions this group is reading from.

