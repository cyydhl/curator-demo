package com.itcast;

import java.util.concurrent.CountDownLatch;

/**
 * Created by Administrator on 2019/7/18.
 */
public class DistributeDemo {
    public static void main(String[] args) {
        final CountDownLatch countDownLatch = new CountDownLatch(10);
        for(int i=0;i<10;i++){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        countDownLatch.await();
                        DistributeLock distributeLock = new DistributeLock();
                        distributeLock.lock();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            },"Thread-"+i).start();
            countDownLatch.countDown();
        }
    }
}
