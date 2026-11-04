import com.github.minecraftedu.network.SimpleWebSocketServer;
// 試験用の起動口（ポートは引数で受け取る）
public class TestMain {
    public static void main(String[] a) throws Exception {
        new SimpleWebSocketServer(Integer.parseInt(a[0]), null, null).start();
        System.out.println("READY");
        Thread.sleep(Long.MAX_VALUE);
    }
}
