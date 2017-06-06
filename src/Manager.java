import config.Config;
import libpf.container.Container;
import libpf.exceptions.ContainerException;
import org.apache.log4j.PropertyConfigurator;
import org.freedesktop.gstreamer.Gst;
import org.snmp4j.asn1.BER;
import org.snmp4j.log.Log4jLogFactory;
import org.snmp4j.log.LogFactory;
import org.snmp4j.log.LogLevel;
import org.snmp4j.smi.OID;
import snmp.MDMIB;
import snmp.MOCreator;
import snmp.SNMPAgent;
import snmp.SNMPWrapper;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;


public class Manager
{

    static SNMPAgent snmpAgent;

    private static Connector client;
    private static Connector intechos;

    private static ServerSocket clientS;
    private static ServerSocket intechosS;

    private static boolean fallbackToBase = false;


    public static final Config config = new Config(MDMConfig.values(), "mdm_config.ini", true);

    static final OID sysDescr = new OID("1.3.6.1.2.1.1.1.0");

    static boolean videoOn = false;

    public static void main(String[] args) throws InterruptedException
    {
        Gst.init("MotorDaemonManager",args);

       /* LogFactory.setLogFactory(new Log4jLogFactory());
        PropertyConfigurator.configure("log4j.properties");
        BER.setCheckSequenceLength(false);

        LogFactory.getLogFactory().getRootLogger().setLogLevel(LogLevel.ERROR);*/

        client = new Connector();
        intechos = new Connector();

        if((args.length >= 2 && args[0].equals("-m")) || (args.length >= 3 && args[1].equals("-m")))
        {
            try
            {
                FileInputStream in = new FileInputStream(args[1]);
                StringBuilder s = new StringBuilder();
                while(in.available() > 0)
                {
                    s.append(in.read());
                }
                in.close();

                Connector.updateContainer(new Container(MapParser.parseMap(s.toString())));

            }
            catch (IOException | ContainerException e)
            {
                e.printStackTrace();
            }

            if(args.length >= 3 && (args[2].equals("--fallback") || args[0].equals("--fallback")))
            {
                Manager.fallbackToBase = true;
            }

        }

        try
        {
            System.out.println("Starting SNMP Agent ..");
            Manager.snmpAgent =  new SNMPAgent("0.0.0.0/"+Integer.toString(config.getInt(MDMConfig.SNMP_PORT)));
            System.out.println("Starting SNMP Agent ...");
            Manager.snmpAgent.start();
            System.out.println("Successfully started SNMP Agent !");
            Manager.snmpAgent.unregisterManagedObject(Manager.snmpAgent.getSnmpv2MIB());
            Manager.snmpAgent.registerManagedObject(MOCreator.createReadOnly(sysDescr, "MotorDaemonManager"));

            SNMPWrapper.initMIB(Manager.snmpAgent);
            System.out.println("Successfully configured !");
        }
        catch (IOException e)
        {
            System.err.println("ERROR : Could not launch SNMP agent on port "+Integer.toString(config.getInt(MDMConfig.SNMP_PORT)));
            e.printStackTrace();
        }

        Runnable clientSeeker = () -> {
            while (true)
            {
                if (clientS == null) {
                    try {
                        clientS = new ServerSocket(config.getInt(MDMConfig.CLIENT_PORT));
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }
                }

                if (!client.isConnected()) {
                    try {
                        System.out.println("Waiting for client");
                        client.setInfos("Client", clientS.accept(), intechos, true);
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
                    intechosS = new ServerSocket(config.getInt(MDMConfig.INTECHOS_PORT));
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
                    intechos.setInfos("MotorDaemon", intechosS.accept(), client, false);
                    System.out.println("MotorDaemon connected.");
                    SNMPWrapper.setValue(Manager.snmpAgent, MDMIB.STATE, "true");

                    if(fallbackToBase)
                    {
                        System.out.println("FALLING BACK TO BASE !");
                        intechos.fallbackToBase();
                        fallbackToBase = false;
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
            }

            Thread.sleep(500);
        }
    }
}
