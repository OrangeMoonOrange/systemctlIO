package testSelectv2;

/**
 * @Author: xk
 * @Date: 2020/12/26 10:56 上午
 * @Desc:
 */
public class MainThread {
    public static void main(String[] args) {

        //1，创建IO Thread （一个或者多个）
        SelectorThreadGroup boss = new SelectorThreadGroup(1);//boss有自己的线程组
        SelectorThreadGroup worker = new SelectorThreadGroup(3);//work也有自己的线程组

        //2，应该吧监听的 server 注册到某一个selector上

        boss.setWorker(worker);//但是 boss得多持有work的引用
        /**
         * boss里面选择一个线程注册listener 触发bind，从而，这个选中的线程得持有workerGroup的引用
         * 因为未来listen 一旦accept 后得去worker中next 出一个线程分配
         */
        boss.bind(9999);

    }
}
