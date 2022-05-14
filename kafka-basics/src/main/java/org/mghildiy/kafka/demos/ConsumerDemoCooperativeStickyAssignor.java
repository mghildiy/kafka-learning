package org.mghildiy.kafka.demos;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class ConsumerDemoCooperativeStickyAssignor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerDemoCooperativeStickyAssignor.class.getSimpleName());
    private static final String TOPIC = "kafka_basics_demo";
    private static final String GROUP = "kafka_basics_demo_consumer";

    public static void main(String[] args) {
        LOGGER.info("Consumer demo starts");

        KafkaConsumer<String, String> consumer = new KafkaConsumer(loadConfig(null));
        consumer.subscribe(List.of(TOPIC));

        Thread mainThread = Thread.currentThread();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Detected a shutdown. Let's wakeup consumer....");
            consumer.wakeup();

            try {
                mainThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));

        try {
            while (true) {
                LOGGER.info("Polling starts");
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                //counter = counter + records.count();
                records.forEach(record -> {
                    LOGGER.info("KEY: " + record.key() + ", VALUE: " + record.value());
                    LOGGER.info("Partition: " + record.partition());
                    LOGGER.info("Offset: " + record.offset());
                });
            }
        } catch (WakeupException e) {
            LOGGER.info("Wakeup exception");
        } catch (Exception e) {
            LOGGER.error("Unexpected exception", e);
        } finally {
            consumer.close();
        }

        LOGGER.info("Consumer demo ENDS");
    }

    private static Properties loadConfig(String configFile) {
        final Properties cfg = new Properties();
        if (configFile != null) {
            InputStream inputStream = ConsumerDemoCooperativeStickyAssignor.class.getClassLoader().getResourceAsStream(configFile);
            if (inputStream == null)
                throw new IllegalArgumentException("file not found! " + configFile);
            try {
                cfg.load(inputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            cfg.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        }
        cfg.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        cfg.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        cfg.setProperty(ConsumerConfig.GROUP_ID_CONFIG, GROUP);
        cfg.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        cfg.setProperty(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, CooperativeStickyAssignor.class.getName());

        return cfg;
    }
}
