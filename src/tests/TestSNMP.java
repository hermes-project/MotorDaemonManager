package tests;

import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.PropertyConfigurator;
import org.snmp4j.asn1.BER;
import org.snmp4j.log.Log4jLogFactory;
import org.snmp4j.log.LogAdapter;
import org.snmp4j.log.LogFactory;
import org.snmp4j.log.LogLevel;
import org.snmp4j.smi.OID;

import snmp.MOCreator;
import snmp.SNMPAgent;
import snmp.SNMPManager;

/**
 * Created by discord on 23/03/17.
 */

public class TestSNMP {

    static final OID sysDescr = new OID("1.3.6.1.2.1.1.1.0");

    public static void main(String[] args) throws IOException {
        TestSNMP client = new TestSNMP("udp:127.0.0.1/161");
        client.init();
    }

    SNMPAgent agent = null;
    /**
     * This is the client which we have created earlier
     */
    SNMPManager client = null;

    String address = null;

    /**
     * Constructor
     *
     * @param add
     */
    public TestSNMP(String add) {
        address = add;
    }

    private void init() throws IOException {
        LogFactory.setLogFactory(new Log4jLogFactory());
        PropertyConfigurator.configure("log4j.properties");
        BER.setCheckSequenceLength(false);

        LogFactory.getLogFactory().getRootLogger().setLogLevel(LogLevel.DEBUG);

        agent = new SNMPAgent("0.0.0.0/8079");
        agent.start();

        // Since BaseAgent registers some MIBs by default we need to unregister
        // one before we register our own sysDescr. Normally you would
        // override that method and register the MIBs that you need
        agent.unregisterManagedObject(agent.getSnmpv2MIB());

        // Register a system description, use one from you product environment
        // to test with
        agent.registerManagedObject(MOCreator.createReadOnly(sysDescr,
                "SUUS !"));

        // Setup the client to use our newly started agent
      //  client = new SNMPManager("udp:127.0.0.1/8079");
       // client.start();
        // Get back Value which is set
        //System.out.println(client.getAsString(sysDescr));

        while(true)
        {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}