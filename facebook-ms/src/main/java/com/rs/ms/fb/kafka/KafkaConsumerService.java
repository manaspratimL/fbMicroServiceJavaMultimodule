package com.rs.ms.fb.kafka;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;

import org.HdrHistogram.Histogram;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.rs.ms.fb.models.FacebookConfigModel;
import com.rs.ms.fb.service.base.FacebookUserDataService;

/**
 * 
 * @author ManasC
 *
 */
@Service
public class KafkaConsumerService extends Thread implements ApplicationListener<ContextRefreshedEvent> {
	
	@Autowired
	private  FacebookUserDataService fbUserDataService;
		
	
	public void run(){  
		System.out.println("thread is running..."); 
		try {
			consumeUpdates();
		} catch (IOException e) {
			e.printStackTrace();
		}
		}  
	
	@SuppressWarnings("resource")
	@Async
	private  void consumeUpdates() throws IOException{
		System.out.println("Kafka Consumer Started fb");
		
		// set up house-keeping
        ObjectMapper mapper = new ObjectMapper();
        Histogram stats = new Histogram(1, 10000000, 2);
        Histogram global = new Histogram(1, 10000000, 2);

        // and the consumer
        KafkaConsumer<String, String> consumer;
        try (InputStream props = Resources.getResource("consumer.props").openStream()) {
            Properties properties = new Properties();
            properties.load(props);
            if (properties.getProperty("group.id") == null) {
                properties.setProperty("group.id", "group-" + new Random().nextInt(100000));
            }
            consumer = new KafkaConsumer<>(properties);
        }
        consumer.subscribe(Arrays.asList("config"));
        int timeouts = 0;
        //noinspection InfiniteLoopStatement
        
        while (true) {
            // read records with a short timeout
            ConsumerRecords<String, String> records = consumer.poll(200);
            if (records.count() == 0) {
                timeouts++;
            } else {
                System.out.printf("Got %d records after %d timeouts\n", records.count(), timeouts);
                timeouts = 0;
            }
            for (ConsumerRecord<String, String> record : records) {
                switch (record.topic()) {
                    case "config":
                    	StringBuilder msg = new StringBuilder(record.value());
                    	msg=msg.deleteCharAt(0);
                    	msg=msg.deleteCharAt(msg.length()-1);
                    	String testString=msg.toString().replaceAll("\"", "'").replaceAll("\\\\", "");
                    	
                    	Gson gson = new Gson();
                    	JsonReader reader = new JsonReader(new StringReader(testString));
                    	reader.setLenient(true);
                    	
                    	FacebookConfigModel fbConfigModel= gson.fromJson(reader, FacebookConfigModel.class);
                    	
                    	//Process the new User
					try {
						fbUserDataService.processUser(fbConfigModel);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                        break;
                    case "other-markers":
                        break;
                    default:
                        throw new IllegalStateException("Shouldn't be possible to get message on topic " + record.topic());
                }
            }
        }
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent arg0) {
		
		//KafkaConsumerService kfService= new KafkaConsumerService();
		this.start();
	}
}
