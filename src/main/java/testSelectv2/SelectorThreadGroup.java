package testSelectv2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: xk
 * @Date: 2020/12/26 11:12 上午
 * @Desc: st.selector.wakeup(); 在上面的时候 同样会阻塞
 * 因为太快了 ，即使wakeup 但是server没有把后面监听注册的fd
 */
public class SelectorThreadGroup {

    SelectorThread[] sts;
    ServerSocketChannel server;
    AtomicInteger xid = new AtomicInteger(0); //用来轮询的数字

    SelectorThreadGroup stg = this;//如果是boss 就是只想自己，如果是work 则是指向work

    public void setWorker(SelectorThreadGroup stg) {
        this.stg = stg;
    }


    //num 是线程数
    public SelectorThreadGroup(int num) {
        sts = new SelectorThread[num];//初始化
        for (int i = 0; i < num; i++) {
            sts[i] = new SelectorThread(this);

            /**
             * 注意：只要这里的SelectorThreadGroup 跑起来 ，
             * 那么SelectorThread就跑起来了，那么selector.select()就会被调用
             * 那么selector.select(); 就会阻塞住
             */
            new Thread(sts[i]).start();
        }
    }

    public void bind(int port) {
        try {
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));

            //注册到哪一个selector上呢？
            //  nextSelector(server);
            nextSelectorV3(server);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void nextSelectorV3(Channel c) {
        try {
            if (c instanceof ServerSocketChannel) {
                SelectorThread st = next();//listen选择 boss组中的一些线程后，要更新这个线程的work组
                st.queue.put(c);
                st.setWorker(stg);
                st.selector.wakeup();
            } else {
                SelectorThread st = nextv3();
                st.queue.put(c);
                st.selector.wakeup();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    //为什么是Channel ，因为 有可能是客户端 或者服务端  （混杂模式）
    public void nextSelector(Channel c) {
        SelectorThread st = next();//在主线程中 。取到堆里的selectoreThread对象
        //重点 ： 选择了一个selector c有可能是server 有可能是client ，要把c注册在selector上


        //1， 通过队列传递数据 消息
        st.queue.add(c);
        //2，通过打破阻塞，让对应的线程去
        st.selector.wakeup();


        //不对的代码
//        //加入是server F
//        ServerSocketChannel s = (ServerSocketChannel) c;
//        //呼应上    int nums = selector.select();
//        try {
//            //放上 放下都有问题 大家在多线程 情况下  是需要沟通的 ，用一个队列
////            st.selector.wakeup();//因为selector.select(); 立刻返回 ，不阻塞，但是这个放在上面还是下面呢？
//            s.register(st.selector, SelectionKey.OP_ACCEPT);//要和开始呼应上，会被阻塞的
////            st.selector.wakeup();
//        } catch (ClosedChannelException e) {
//            e.printStackTrace();
//        }
    }


    //从数组中选择一个SelectorThread，并且返回
    private SelectorThread next() {
        int index = xid.incrementAndGet() % sts.length; //轮询会有数据倾斜的问题
        return sts[index];
    }

    private SelectorThread nextv3() {
        int index = xid.incrementAndGet() % stg.sts.length; //动用worker的线程分配
        return stg.sts[index];
    }


}
