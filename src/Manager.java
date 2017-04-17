import org.freedesktop.gstreamer.Gst;
import snmp.MDMIB;
import snmp.SNMPAgent;
import snmp.SNMPWrapper;

import java.io.IOException;
import java.net.ServerSocket;


public class Manager
{

    private static SNMPAgent snmpAgent;

    private static Connector client;
    private static Connector intechos;

    private static ServerSocket clientS;
    private final static int clientPort=  56987;

    private static ServerSocket intechosS;
    private final static int intechosPort= 56990;

    private final static int snmpPort=56991;

    public static void main(String[] args) throws InterruptedException
    {
        Gst.init("MotorDaemonManager",args);

        try
        {
            System.out.println("Starting SNMP Agent ..");
            Manager.snmpAgent =  new SNMPAgent("0.0.0.0/"+Integer.toString(snmpPort));
            System.out.println("Starting SNMP Agent ...");
            Manager.snmpAgent.start();
            System.out.println("Successfully started SNMP Agent !");
            Manager.snmpAgent.unregisterManagedObject(Manager.snmpAgent.getSnmpv2MIB());
            SNMPWrapper.initMIB(Manager.snmpAgent);
            System.out.println("Successfully configured !");
        }
        catch (IOException e)
        {
            System.err.println("ERROR : Could not launch SNMP agent on port "+Integer.toString(snmpPort));
            e.printStackTrace();
        }

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
                    SNMPWrapper.setValue(Manager.snmpAgent, MDMIB.STATE, "false");
                    System.out.println("Waiting for INTechOS");
                    intechos = new Connector(intechosS.accept(), null);
                    System.out.println("INTechOS connected.");
                    SNMPWrapper.setValue(Manager.snmpAgent, MDMIB.STATE, "true");
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
