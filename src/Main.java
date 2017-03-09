import org.freedesktop.gstreamer.Gst;

import java.io.IOException;
import java.net.ServerSocket;


public class Main
{

    private static Connector client;
    private static Connector intechos;

    private static ServerSocket clientS;
    private final static int clientPort=  56987;

    private static ServerSocket intechosS;
    private final static int intechosPort= 56990;

    public static void main(String[] args) throws InterruptedException
    {
        Gst.init("MotorDaemonManager",args);

        while (true)
        {
            if(intechosS == null)
            {
                try {
                    intechosS = new ServerSocket(intechosPort);
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
            }

            if(intechos == null || !intechos.isConnected())
            {
                try {
                    System.out.println("Waiting for INTechOS");
                    intechos = new Connector(intechosS.accept(), null);
                    System.out.println("INTechOS connected.");
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
            }

            if(clientS == null)
            {
                try {
                    clientS = new ServerSocket(clientPort);
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
            }

            if(client == null || !client.isConnected())
            {
                try {
                    System.out.println("Waiting for client");
                    client = new Connector(clientS.accept(), intechos, true);
                    intechos.setTarget(client);
                    System.out.println("Client connected.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            Thread.sleep(500);
        }
    }
}
