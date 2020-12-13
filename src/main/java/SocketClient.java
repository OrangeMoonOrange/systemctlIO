import java.io.*;
import java.net.Socket;

/**
 * @author:
 * @create:
 */
public class SocketClient {

    public static void main(String[] args) {

        try {
            Socket client = new Socket("192.168.150.11",9090);

            client.setSendBufferSize(20);
            /**
             * 攒 的话 会有延时  ，但是会组织成一个比较大的包发送
             * 不攒 的话  只要有数据就发
                    */
            client.setTcpNoDelay(true);//赞不赞东西？？

            OutputStream out = client.getOutputStream();

            InputStream in = System.in;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            while(true){
                String line = reader.readLine();
                if(line != null ){
                    byte[] bb = line.getBytes();
                    for (byte b : bb) {
                        out.write(b);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
