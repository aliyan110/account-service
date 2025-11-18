package com.bank.account.kafkaClients;

import com.bank.account.dto.TransactionEvent;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.ConfigProvider;

import java.io.*;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@ApplicationScoped
public class AccountServiceProducer {

    String kafkaUser = ConfigProvider.getConfig().getValue("kafka.user", String.class);
    String kafkaPassword = ConfigProvider.getConfig().getValue("kafka.password", String.class);

    private final KafkaProducer<String, TransactionEvent> producer;
    private final String topic = "account-events";


    public AccountServiceProducer() throws Exception {
        // Load PEM files from resources
        File svcPem = getResourceAsTempFile("svc-pem.pem");
        File caPem = getResourceAsTempFile("ca-pem.pem");


        Properties props = new Properties();
        props.put("bootstrap.servers", "kafka-1bf98848-cloud-50a3.b.aivencloud.com:20752");
        props.put("security.protocol", "SSL");
        props.put("ssl.truststore.type", "PEM");
        props.setProperty("ssl.truststore.location", caPem.getAbsolutePath());
        props.setProperty("ssl.keystore.location", svcPem.getAbsolutePath());
        props.put("ssl.keystore.type", "PEM");
        props.put("ssl.endpoint.identification.algorithm", "");
        props.put("key.serializer", StringSerializer.class.getName());
        props.put("value.serializer", KafkaJsonSchemaSerializer.class.getName());
        props.put("auto.register.schemas", false);
        props.put("schema.registry.url", "https://kafka-1bf98848-cloud-50a3.b.aivencloud.com:20755");
        props.put("basic.auth.credentials.source", "USER_INFO");
        props.put("basic.auth.user.info",kafkaUser+":"+kafkaPassword);
        props.put("value.subject.name.strategy", "io.confluent.kafka.serializers.subject.TopicNameStrategy");

        this.producer = new KafkaProducer<>(props);
    }

    public void sendEvent(TransactionEvent event) throws ExecutionException, InterruptedException {
        try {
            Future<RecordMetadata> result = producer.send(new ProducerRecord<String, TransactionEvent>(topic, event));
            // wait until future is resolved to ensure it is sent.
            RecordMetadata recordData = result.get();
            System.out.printf("Message sent to offset: %d, partition: %d, value: %s ", recordData.offset(), recordData.partition(), event);
        }catch (InterruptedException | ExecutionException ex) {
            throw ex;
        } finally {
            producer.flush();
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
