import com.github.minecraftedu.network.SimpleWebSocketServer;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
// 試験用の起動口（ポートは引数で受け取る）
// 標準入力に STOP の行が来たら受け口を止めて STOPPED と出す（Minecraft を止めるときと同じ stop() を呼ぶ試験のため）
public class TestMain {
    public static void main(String[] a) throws Exception {
        SimpleWebSocketServer server = new SimpleWebSocketServer(Integer.parseInt(a[0]), null, null);
        server.start();
        System.out.println("READY");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        String line;
        while ((line = in.readLine()) != null) {
            if (line.trim().equals("STOP")) {
                server.stop();
                System.out.println("STOPPED");
            }
        }
        // 標準入力が閉じても（試験の側が渡さないときも）、殺されるまで動き続ける
        Thread.sleep(Long.MAX_VALUE);
    }
}
