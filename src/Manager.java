import org.freedesktop.gstreamer.Gst;
import snmp.MDMIB;
import snmp.SNMPAgent;
import snmp.SNMPWrapper;

import java.io.IOException;
import java.net.ServerSocket;


public class Manager
{

    static SNMPAgent snmpAgent;

    private static Connector client;
    private static Connector intechos;

    private static ServerSocket clientS;
    private final static int clientPort=  56987;

    private static ServerSocket intechosS;
    private final static int intechosPort= 56990;

    private final static int snmpPort=56991;

    static boolean videoOn = false;

    public static void main(String[] args) throws InterruptedException
    {
        Gst.init("MotorDaemonManager",args);

        client = new Connector();
        intechos = new Connector();

        try
        {
            Manager.snmpAgent =  new SNMPAgent("0.0.0.0/"+Integer.toString(snmpPort));
            Manager.snmpAgent.start();
            Manager.snmpAgent.unregisterManagedObject(Manager.snmpAgent.getSnmpv2MIB());
            SNMPWrapper.initMIB(Manager.snmpAgent);
        }
        catch (IOException e)
        {
            System.err.println("ERROR : Could not launch SNMP agent on port "+Integer.toString(snmpPort));
            e.printStackTrace();
        }

        Runnable clientSeeker = () -> {
            while (true)
            {
                if (clientS == null) {
                    try {
                        clientS = new ServerSocket(clientPort);
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }
                }

                if (!client.isConnected()) {
                    try {
                        System.out.println("Waiting for client");
                        client.setInfos(clientS.accept(), intechos, true);
                        intechos.setTarget(client);
                        System.out.println("Client connected.");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };

        Thread t = new Thread(clientSeeker);
        t.start();

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

            if(!intechos.isConnected())
            {
                try {
                    SNMPWrapper.setValue(Manager.snmpAgent, MDMIB.STATE, "false");
                    System.out.println("Waiting for MotorDaemon");
                    intechos.setInfos(intechosS.accept(), client, false);
                    System.out.println("MotorDaemon connected.");
                    SNMPWrapper.setValue(Manager.snmpAgent, MDMIB.STATE, "true");
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
            }

            Thread.sleep(500);
        }
    }
}
