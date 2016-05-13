package com.rs.ms.twitter;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.rs.ms.common.kafka.KafkaProducerServiceCommon;

import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;

/**
 * Created by babluj on 5/9/16.
 */
@Service
public class TwitterClient implements User {

    private final static Twitter TWITTER = TwitterFactory.getSingleton();

    //@Value("${paging.pages}")
    private Integer batches;

    //@Value("${paging.count.per.page}")
    private Integer batchSize;

    @Override
    public TimeLine getTimeLine() throws TwitterException {

        Paging paging = new Paging(1, 20);

        //fetch user home timeline and create an RS timeline
        return new TimeLine(TWITTER.getHomeTimeline(paging));
    }

    public static void main(String... args) throws Exception {
        new KafkaProducerServiceCommon().publishUpdates(new Gson().toJson(new TwitterClient().getTimeLine()), "rsmstw");
    }
}
