import com.github.unidbg.linux.file.TcpSocket;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Program {
    private static final String npaCode = "0EH0IZA1050J2", npaSN = "20790000027249267";

    public static void main(String[] args) throws Throwable {

        TcpSocket.proxy = new InetSocketAddress(InetAddress.getByName("localhost"), 8888);

        Ngsm ngsm = new Ngsm();

        ngsm.ngsmNativeInit();

        ngsm.ngsmNativeRun(npaCode, npaSN);

        System.out.println(ngsm.ngsmNativeGetNgsmToken(npaCode, npaSN));
    }
}
