package test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @Author: xk
 * @Date: 2020/12/24 6:06 下午
 * @Desc:
 */
public class SelectorThreadGroup {
    SelectThread[] sts;
    ServerSocketChannel server = null;
    AtomicInteger xid = new AtomicInteger(0);

    //num 是线程数
    public SelectorThreadGroup(int num) {
        sts = new SelectThread[num];
        //对成员进行初始化
        for (int i = 0; i < num; i++) {
            sts[i] = new SelectThread();
            new Thread(sts[i]).start();
        }
    }

    //绑定端口号
    public void bind(int port) {
        try {
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));

            // 主线程 注册到哪一个selector上呢？

            nextSelector(server);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void nextSelector(Channel c) {
        SelectThread st = next();
        //重点 : c有可能是server 有可能是client
        ServerSocketChannel s = (ServerSocketChannel) c;
        try {
            st.selector.wakeup();//功能是让selector 的select()方法立刻返回 不阻塞
            s.register(st.selector, SelectionKey.OP_ACCEPT);


        } catch (ClosedChannelException e) {
            e.printStackTrace();
        }

    }

    //无论 serversocket还是 socket 都复用这个方法
    private SelectThread next() {
        int index = xid.incrementAndGet() % sts.length;
        return sts[index];
    }
}
