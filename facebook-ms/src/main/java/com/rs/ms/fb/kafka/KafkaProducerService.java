package com.rs.ms.fb.kafka;
import com.google.common.io.Resources;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Properties;
/**
 * 
 * @author ManasC
 *
 */
@Service
public class KafkaProducerService {

	@Async
	public void publishUpdates(String msg, String topic) throws Exception {
		
        KafkaProducer<String, String> producer = null;
        try (InputStream props = Resources.getResource("producer.props").openStream()) {
            Properties properties = new Properties();
            properties.load(props);
            producer = new KafkaProducer<>(properties);
            
            producer.send(new ProducerRecord<String, String>(
            		topic, msg));
            System.out.println("--> Message [" + msg + "] sent.Check message on Consumer's program console");
        } catch (Throwable throwable) {
            System.out.println(throwable.getStackTrace());
        } finally {
            producer.close();
        }

	}
	
}
