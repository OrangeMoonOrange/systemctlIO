package test;

/**
 * @Author: xk
 * @Date: 2020/12/24 5:31 下午
 * @Desc:
 */
// 这里不做关于IO和业务的事情 只是启动其他线程
public class MainThread {
    public static void main(String[] args) {

        //1，创建IOthread （可以是一个或者多个）
        //一个还好  多个怎么分配呢？
        SelectorThreadGroup stg = new SelectorThreadGroup(1);

        //2，应该吧监听的server 注册到某一个selector上
        stg.bind(9999);

        //3，

    }
}
