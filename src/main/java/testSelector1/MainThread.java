package testSelector1;

/**
 * @Author: xk
 * @Date: 2020/12/26 10:56 上午
 * @Desc:
 */
public class MainThread {
    public static void main(String[] args) {
        //1，创建IO Thread （一个或者多个）
        SelectorThreadGroup stg = new SelectorThreadGroup(3);
        // SelectorThreadGroup stg = new SelectorThreadGroup(1);

        //2，应该吧监听的 server 注册到某一个selector上

        stg.bind(9999);

    }
}
