package test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

/**
 * @Author: xk
 * @Date: 2020/12/24 5:32 下午
 * @Desc: 突然发现idea在Mac上和winodws上的快捷键不一样？完了
 */
public class SelectThread implements Runnable { //也就是Netty中的EventGroup
    //每一个线程对应一个select 多线程情况下，该程序的并发客户端被
    //分配到多个selector上
    //注意 每个客户端 只绑定其中一个selector，他们不会有交互的情况
    Selector selector = null;

    SelectThread() {
        try {
            selector = Selector.open();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        //Loop
        while (true) {
            try {
                //1 ，select()

                //如果不给参数，那么会一直阻塞，直到那些注册的fd，有事件之后才会继续走，极大可能会有永久阻塞
                int nums = selector.select();//不传参数不会阻塞
                //2,处理 selectkeys
                if (nums > 0) {
                    //拿到事件
                    Set<SelectionKey> kesy = selector.selectedKeys();
                    Iterator<SelectionKey> iter = kesy.iterator();
                    while (iter.hasNext()) {//线程线性处理的过程
                        SelectionKey key = iter.next();
                        iter.remove();
                        if (key.isAcceptable()) {//复杂，接受客户端的过程（接受之后，要注册，多线程下，新的客户端注册到哪里呢？）
                            acceptHandler(key);
                        } else if (key.isReadable()) {
                            readHandler(key);

                        } else if (key.isWritable()) {

                        }
                    }

                }
                //3，处理一些task


            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private void readHandler(SelectionKey key) {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        buffer.clear();
        //剩下就是需要对其进行读取
        while (true) {
            try {
                int num = client.read(buffer);
                if (num > 0) {
                    buffer.flip();//将读到的内容翻转，然后直接写出
                    while (buffer.hasRemaining()) {
                        client.write(buffer);
                    }
                    buffer.clear();
                } else if (num == 0) {
                    break;
                } else {//num <0
                    //有可能客户端断开了 异常情况
                    System.out.println("client：" + client.getRemoteAddress() + "close。。。。。");
                    key.cancel();
                    break;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }


        }

    }

    private void acceptHandler(SelectionKey key) {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();

        try {
            SocketChannel client = server.accept();
            client.configureBlocking(false);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


}
