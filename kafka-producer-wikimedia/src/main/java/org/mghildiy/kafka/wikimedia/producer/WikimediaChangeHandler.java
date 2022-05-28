package org.mghildiy.kafka.wikimedia.producer;

import com.launchdarkly.eventsource.EventHandler;
import com.launchdarkly.eventsource.MessageEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WikimediaChangeHandler implements EventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(WikimediaChangeHandler.class.getSimpleName());
    private final KafkaProducer<String, String> producer;
    private final String topic;

    public WikimediaChangeHandler(KafkaProducer<String, String> producer, String topic) {
        this.producer = producer;
        this.topic = topic;
    }

    @Override
    public void onOpen() {
        // do nothing
    }

    @Override
    public void onClosed() {
        this.producer.close();
    }

    @Override
    public void onMessage(String s, MessageEvent messageEvent) {
        LOGGER.info("Received data from wikimedia stream:"+messageEvent.getData());

        this.producer.send(new ProducerRecord<>(this.topic, messageEvent.getData()));
    }

    @Override
    public void onComment(String s) {
        // do nothing
    }

    @Override
    public void onError(Throwable throwable) {
        LOGGER.error("Error while reading form wikimedia:", throwable);
    }
}
