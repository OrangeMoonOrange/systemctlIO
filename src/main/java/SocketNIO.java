import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;

public class SocketNIO {

    public static void main(String[] args) throws Exception {
        LinkedList<SocketChannel> clients = new LinkedList<>();
        ServerSocketChannel ss = ServerSocketChannel.open();
        ss.bind(new InetSocketAddress(9090));
        ss.configureBlocking(false); //重点  OS  NONBLOCKING!!!
        ss.setOption(StandardSocketOptions.TCP_NODELAY, false);
        while (true) {
            //接受客户端连接
            Thread.sleep(1000);
            SocketChannel client = ss.accept(); //不会阻塞？  -1NULL
            //accept 调用内核：1.没有客户端连接进来，返回值？在BIO时期的时候一直卡着,但是在NIO，不卡着，返回-1，nULL
            //如果客户端的连接来了，accept 返回的是这个客户端的fd client object
            //NIO 就是代码能往下走 ，只不过有不同的情况
            if (client == null) {
                System.out.println("null.....");
            } else {
                client.configureBlocking(false);//重点 socket
                int port = client.socket().getPort();
                System.out.println("client...port: " + port);
                clients.add(client);
            }

            ByteBuffer buffer = ByteBuffer.allocateDirect(4096);  //可以在堆里   堆外

            //遍历已经连接进来的客户端能不能读写数据
            for (SocketChannel c : clients) {   //串行化！！！！  多线程！！
                //但是当客户端变多的时候，轮询的时候就很慢了
                int num = c.read(buffer);  // >0  -1  0   //不会阻塞
                if (num > 0) {
                    buffer.flip();
                    byte[] aaa = new byte[buffer.limit()];
                    buffer.get(aaa);

                    String b = new String(aaa);
                    System.out.println(c.socket().getPort() + " : " + b);
                    buffer.clear();
                }
            }
        }
    }
}
