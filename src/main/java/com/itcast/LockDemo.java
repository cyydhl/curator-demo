package com.itcast;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;

/**
 * Created by Administrator on 2019/7/17.
 */
public class LockDemo {
    private static String CONNECTION_STR = "172.16.200.128:2181,172.16.200.129:2181,172.16.200.130:2181";
    public static void main(String[] args) {
        CuratorFramework curatorFramework = CuratorFrameworkFactory
                .builder()
                .connectString(CONNECTION_STR)
                .sessionTimeoutMs(50000000)
                .retryPolicy(new ExponentialBackoffRetry(1000,3))
                .build();
        curatorFramework.start();

        final InterProcessMutex lock = new InterProcessMutex(curatorFramework,"/lock");

        for(int i=0;i < 10;i++){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    System.out.println(Thread.currentThread().getName()+"尝试获得锁");
                    try {
                        lock.acquire();
                        System.out.println(Thread.currentThread().getName()+"成功获得了锁");

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        Thread.sleep(400000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }finally {
                        try {
                            lock.release();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            },"Thread-"+i).start();
        }
    }
}
