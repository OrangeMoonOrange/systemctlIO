package testSelector1;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @Author: xk
 * @Date: 2020/12/26 10:57 上午
 * @Desc: 每个线程对应一个selector
 */
public class SelectorThread implements Runnable {
    /**
     * selector 之间不会有交互的问题
     */

    Selector selector = null;
    LinkedBlockingQueue<Channel> queue;
    SelectorThreadGroup stg;

    public SelectorThread(SelectorThreadGroup stg) {
        this.stg = stg;
        queue = new LinkedBlockingQueue<>();
        try {
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        //loop
        while (true) {
            try {
                //System.out.println(Thread.currentThread().getName() + " before select......  " + selector.keys().size());
                int nums = selector.select();

//                Thread.sleep(1000); 绝对不是解决方案
                //System.out.println(Thread.currentThread().getName() + " after select......   " + selector.keys().size());
                //如果不给出超时时间，那么有可能一直阻塞，因为会一直等着当初注册的那些fd，有事件
                if (nums > 0) {
                    //2,大于0 处理selectkeys

                    Set<SelectionKey> keys = selector.selectedKeys();
                    Iterator<SelectionKey> iter = keys.iterator();
                    while (iter.hasNext()) {//线性处理的过程
                        SelectionKey key = iter.next();
                        iter.remove();
                        if (key.isAcceptable()) {
                            //复杂，接受客户端的过程，新来的客户端 注册到哪一个selector?
                            acceptHandler(key);

                        } else if (key.isReadable()) {
                            readHandle(key);

                        } else if (key.isWritable()) {

                        }
                    }


                } else if (nums == 0) {


                } else {


                }
                //3，处理一些task

                //必须要在这里进行注册
                if (!queue.isEmpty()) {//队列为非空的时候
                    try {
                        Channel c = queue.take();//
                        if (c instanceof ServerSocketChannel) {
                            ServerSocketChannel server = (ServerSocketChannel) c;
                            server.register(selector, SelectionKey.OP_ACCEPT);
                            System.out.println(Thread.currentThread().getName()+" register listen");

                        } else if (c instanceof SocketChannel) {
                            SocketChannel client = (SocketChannel) c;
                            ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
                            client.register(selector, SelectionKey.OP_READ, buffer);
                            System.out.println(Thread.currentThread().getName()+" register client: " + client.getRemoteAddress());
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void readHandle(SelectionKey key) {
        System.out.println(Thread.currentThread().getName()+" read......");
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        SocketChannel client = (SocketChannel) key.channel();
        buffer.clear();
        while (true) {
            try {
                int num = client.read(buffer);
                if (num > 0) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        client.write(buffer);
                    }
                    buffer.clear();
                } else if (num == 0) {
                    break;
                } else if (num < 0) {
                    //有可能客户端 断开了
                    System.out.println("client: " + client.getRemoteAddress() + " is closed.");
                    key.cancel();
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void acceptHandler(SelectionKey key) {
        System.out.println( Thread.currentThread().getName()+" acceptHandler");
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        try {
            SocketChannel client = server.accept();
            client.configureBlocking(false);

            //选择一个selector 去注册
            stg.nextSelectorV2(client);


        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
