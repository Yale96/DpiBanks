/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.util.ArrayList;
import java.util.List;
import messaging.requestreply.RequestReply;

/**
 *
 * @author Yannick van Leeuwen
 */
public class Gateway {
    private Producer producer;
    private Consumer consumer;
    
    public Gateway(String listenerQue){
        producer = new Producer();
        consumer = new Consumer(listenerQue){
            @Override
            public void messageReceive(RequestReply rr){
                messageReceived(rr);
            }
        };
    }

    public void postMessage(RequestReply rr) {
       producer.send(rr);
    }
    
    public void messageReceived(RequestReply rr){
        
    }
}
