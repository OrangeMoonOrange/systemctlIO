import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;


/**
 *整个逻辑：
 * 在while(true){}死循环里面
 *
 * 第一次：只有有一个listener的fd，就是服务端，也就是第一次一定会返回1，并且是接受事件，那么就一定会走 if (key.isAcceptable())
 *
 * 然后 acceptHandler(key); 走完之后，会再次走到大循环，while (selector.select() > 0)  等待fd 事件，如果这时候有客户端连接或者
 * 其他事件 （read/write），
 *
 *
 * accept和isread和iswrite 这些事件目前都是在一个线程里面完成，如果readHandle那么阻塞了很长时间，那么其他连接会出问题
 * 那么就需要保证了readHandle这个方法足够快，那么为什么提出了IOThread。也就是把读到的东西往后扔
 * 也就是这部分一定要想明白IO和处理
 */
public class SocketMultiplexingSingleThreadv1 {

    //马老师的坦克 一 二期
    private ServerSocketChannel server = null;
    private Selector selector = null;   //对linux 多路复用器的包装（select poll    epoll kqueue） nginx  event{}
    int port = 9090;

    public void initServer() {
        try {
            //下面这两个服务端都有的是哪个步骤
            server = ServerSocketChannel.open();
            server.configureBlocking(false);//设置非阻塞
            server.bind(new InetSocketAddress(port));


            //如果在epoll模型下，open--》  epoll_create -> fd3
            //会有优先选择多路复用器
            selector = Selector.open();  //  select  poll  *epoll  优先选择：epoll  但是可以 -D修正

            //server 等于 listen状态的 fd4（服务端嘛）
            /*
            register
            如果：在下面的不同模型
            select，poll：jvm里开辟一个数组把 fd 放进去
            epoll：  epoll_ctl(fd3,ADD,fd4,EPOLLIN)
             */
            server.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        initServer();
        System.out.println("服务器启动了。。。。。");
        try {
            while (true) {  //死循环
                Set<SelectionKey> keys = selector.keys();
                System.out.println(keys.size()+"   size");
                //做了哪些事情？不同的多路复用器模型是不用的
                //1,调用多路复用器(select,poll  or  epoll (epoll_wait))
                /*
                select()是啥意思：调用啥方法
                1，select，poll  其实  内核的select（fd4）  poll(fd4)
                2，epoll：  其实 内核的 epoll_wait()
                *, 参数可以带时间：没有时间或者是0 ：阻塞，有时间的话设置一个超时
                会有一个api:selector.wakeup()  结果返回0  //可以手动控制

                懒加载：
                其实再触碰到selector.select()调用的时候触发了epoll_ctl的调用

                 */

                //
                while (selector.select() > 0) {//有事件了或者有连接了就会大于0
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();  //返回的有状态的fd集合
                    Iterator<SelectionKey> iter = selectionKeys.iterator();

                    //so，管你啥多路复用器，你呀只能给我状态，我还得一个一个的去处理他们的R/W。同步（理解什么是同步还是异步）好辛苦！！！！！！！！
                    //  如果是NIO的话  自己对着每一个fd调用系统调用，浪费资源，那么你看，这里是不是调用了一次select方法，知道具体的那些可以R/W了？

                    //我前边可以强调过，socket有两种：  listen  和 通信 R/W
                    //一种就是两种连接 等待accept 另外一种就等待R/W
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next();
                        iter.remove(); //set  不移除会重复循环处理
                        if (key.isAcceptable()) {
                            //看代码的时候，这里是重点，如果要去接受一个新的连接
                            //语义上，accept接受连接且返回新连接的FD对吧？
                            //那新的FD怎么办？
                            //select，poll，因为他们内核没有空间，那么在jvm中保存和前边的fd4那个listen的一起
                            //epoll： 我们希望通过epoll_ctl把新的客户端fd注册到内核空间
                            acceptHandler(key);
                        } else if (key.isReadable()) {
                            readHandler(key);  //连read 还有 write都处理了 升级版本看1_1
                            //在当前线程，这个方法可能会阻塞  ，如果阻塞了十年，其他的IO早就没电了。。。
                            //所以，为什么提出了 IO THREADS
                            //redis  是不是用了epoll，redis是不是有个io threads的概念 ，redis是不是单线程的
                            //tomcat 8,9  异步的处理方式  IO  和   处理上  解耦
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void acceptHandler(SelectionKey key) {
        try {
            ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
            SocketChannel client = ssc.accept(); //来啦，目的是调用accept接受客户端，这里返回接受到的客户端的fd
            client.configureBlocking(false);

            ByteBuffer buffer = ByteBuffer.allocate(8192);  //前边讲过了

            // 0.0  我类个去
            //你看，调用了register，和前面的写法是一样的
            /*
            select，poll：jvm里开辟一个数组 fd7 放进去  ，方便下次调用select 的时候传参数
            epoll：  epoll_ctl(fd3,ADD,fd7,EPOLLIN
             */
            client.register(selector, SelectionKey.OP_READ, buffer);
            System.out.println("-------------------------------------------");
            System.out.println("新客户端：" + client.getRemoteAddress());
            System.out.println("-------------------------------------------");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void readHandler(SelectionKey key) {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        buffer.clear();
        int read = 0;
        try {
            while (true) {
                read = client.read(buffer);
                if (read > 0) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        client.write(buffer);//这里是把数据直接回写了
                    }
                    buffer.clear();
                } else if (read == 0) {
                    break;
                } else {//读到 -1， 例如对面客户端断开了连接，把客户端直接断开了
                    client.close();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    public static void main(String[] args) {
        SocketMultiplexingSingleThreadv1 service = new SocketMultiplexingSingleThreadv1();
        service.start();
    }
}
