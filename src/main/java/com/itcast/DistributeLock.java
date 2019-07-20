package com.itcast;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
import org.jboss.netty.util.internal.StringUtil;

import java.io.IOException;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Created by Administrator on 2019/7/18.
 */
public class DistributeLock implements Lock,Watcher{
    private String ROOT_LOCK = "/locks";
    private String WAIT_LOCK;
    private String CURRENT_LOCK;
    ZooKeeper zooKeeper = null;
    CountDownLatch countDown = null;

    public DistributeLock(){
        try {
            zooKeeper = new ZooKeeper("172.16.200.128:2181",4000,this);
            Stat stat = zooKeeper.exists(ROOT_LOCK, false);
            if(stat == null){
                zooKeeper.create(ROOT_LOCK, "0".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void lock() {
        if(tryLock()){
            System.out.println(Thread.currentThread().getName()+"获得锁成功");
        }else{
            waitForLock(WAIT_LOCK);

        }

    }

    private void waitForLock(String prev) {
        try {
            Stat stat = zooKeeper.exists(prev, true);
            if(stat != null){
                System.out.println(Thread.currentThread().getName()+"正在等待"+prev);
                countDown = new CountDownLatch(1);
                countDown.await();
                System.out.println(Thread.currentThread().getName()+"->获得锁成功");

            }
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    @Override
    public boolean tryLock() {
        //创建临时有序节点
        //获取locks下的所有节点，利用sortSet排序
        //判断当前节点是否为最小的节点，如果是，则返回true
        //如果不是，则将当前节点的上一个节点赋值给WAIT_LOCK
        try {
            //创建临时有序节点
            CURRENT_LOCK = zooKeeper.create(ROOT_LOCK+"/","0".getBytes(),ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode.EPHEMERAL_SEQUENTIAL);
            //获取locks下的所有节点，利用sortSet排序
            List<String> children = zooKeeper.getChildren(ROOT_LOCK, false);
            SortedSet<String> sortedSet = new TreeSet();
            for (String child : children) {
                sortedSet.add(ROOT_LOCK +"/" + child);
            }
            //判断当前节点是否为最小的节点，如果是，则返回true
            String first = sortedSet.first();
            if(CURRENT_LOCK.equals(first)){
                return true;
            }
            SortedSet<String> lessThanMe = sortedSet.headSet(CURRENT_LOCK);
            if(!lessThanMe.isEmpty()){
                WAIT_LOCK = lessThanMe.last();
            }

        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public void unlock() {
        System.out.println(Thread.currentThread().getName()+"->释放锁"+CURRENT_LOCK);
        try {
            zooKeeper.delete(CURRENT_LOCK,-1);
            CURRENT_LOCK=null;
            zooKeeper.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Condition newCondition() {
        return null;
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        if(this.countDown!=null){
            this.countDown.countDown();
        }

    }
}
