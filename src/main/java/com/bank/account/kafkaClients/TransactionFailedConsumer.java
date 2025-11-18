package com.bank.account.kafkaClients;

import com.bank.account.dto.TransactionEvent;
import com.bank.account.service.AccountService;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.microprofile.config.ConfigProvider;

import java.io.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

@ApplicationScoped
@Startup
public class TransactionFailedConsumer {
    @Inject
    AccountService accountService;

    String kafkaUser = ConfigProvider.getConfig().getValue("kafka.user", String.class);
    String kafkaPassword = ConfigProvider.getConfig().getValue("kafka.password", String.class);

    private KafkaConsumer<String, TransactionEvent> consumer;

    private final String topic = "account-events";

    @PostConstruct
    public void init() throws Exception {

        File svcPem = getResourceAsTempFile("svc-pem.pem");
        File caPem = getResourceAsTempFile("ca-pem.pem");

        Properties props = new Properties();
        props.put("bootstrap.servers", "kafka-1bf98848-cloud-50a3.b.aivencloud.com:20752");
        props.put("security.protocol", "SSL");
        props.put("ssl.truststore.type", "PEM");
        props.setProperty("ssl.truststore.location", caPem.getAbsolutePath());
        props.setProperty("ssl.keystore.location", svcPem.getAbsolutePath());
        props.put("ssl.keystore.type", "PEM");
        props.put("key.deserializer", StringDeserializer.class.getName());
        props.put("value.deserializer", KafkaJsonSchemaDeserializer.class.getName());
        props.put("json.value.type", TransactionEvent.class.getName());
        props.setProperty("group.id", "account-events-rollback-consumer");
        props.setProperty("auto.offset.reset", "earliest");
        props.put("auto.register.schemas", false);
        props.put("schema.registry.url", "https://kafka-1bf98848-cloud-50a3.b.aivencloud.com:20755");
        props.put("basic.auth.credentials.source", "USER_INFO");
        props.put("basic.auth.user.info", kafkaUser+":"+kafkaPassword);

        consumer = new KafkaConsumer<String, TransactionEvent>(props);
        consumer.subscribe(Arrays.asList(topic));

        System.out.println("TransactionFailedConsumer started and listening…");

        // Start polling in a single thread
        Thread pollingThread = new Thread(this::startPolling);
        pollingThread.setName("TransactionFailedThread");
        pollingThread.start();
    }

    // Loop to continuously poll Kafka
    private void startPolling() {
        try {
            System.out.println("TransactionFailedConsumer started and listening…22222222222");
            while (true) {  // ← infinite loop keeps running
                var records = consumer.poll(Duration.ofMillis(500));
                for (var record : records) {
                    System.out.println("Received record: " + record);
                    TransactionEvent event = record.value();
                    System.out.println("Received event: " + event);
                    if (event != null && "TransactionFailed".equals(event.eventType)) {
                        System.out.println("Processing transaction: " + event.transactionId);
                        accountService.process(event);
                    }
                }
                consumer.commitSync(); // commit offsets after processing
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            consumer.close(); // only runs if the loop exits (like on shutdown)
        }
    }

    private File getResourceAsTempFile(String resourceName) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName);
        if (is == null) {
            throw new FileNotFoundException("Resource not found: " + resourceName);
        }
        File tempFile = File.createTempFile(resourceName, ".pem");
        tempFile.deleteOnExit(); // optional cleanup
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            is.transferTo(fos);
        }
        return tempFile;
    }
}
