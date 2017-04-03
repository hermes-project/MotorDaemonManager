import snmp.MDMIB;
import snmp.SNMPWrapper;

import java.util.Arrays;

/**
 * SNMP Updater
 */
public class SNMPUpdater extends Thread
{

    private final Connector parent;

    SNMPUpdater(Connector conn)
    {
        this.parent = conn;
    }

    @Override
    public void run()
    {
        while (parent.isConnected())
        {
            String[] vals = parent.sendAndReceive("status",1)[0].split(";");
            if(vals.length != 5)
            {
                System.err.println("SNMPUpdater : BAD STATUS RECEIVED : "+ Arrays.toString(vals));
            }
            else
            {
                SNMPWrapper.setValue(Manager.snmpAgent, MDMIB.CPU_LOAD, vals[0]);
                SNMPWrapper.setValue(Manager.snmpAgent, MDMIB.CPU_TEMP, vals[1]);
                SNMPWrapper.setValue(Manager.snmpAgent, MDMIB.RAM_USED, Integer.toString(Integer.parseInt(vals[3]) - Integer.parseInt(vals[2])));
                SNMPWrapper.setValue(Manager.snmpAgent, MDMIB.RAM_TOTAL, vals[3]);
                SNMPWrapper.setValue(Manager.snmpAgent, MDMIB.SPEED, vals[4]);
                SNMPWrapper.setValue(Manager.snmpAgent, MDMIB.CAMERA, Boolean.toString(Manager.videoOn));
            }

            vals = parent.sendAndReceive("p",1)[0].split(";");
            if(vals.length != 3)
            {
                System.err.println("SNMPUpdater : BAD POS RECEIVED : "+ Arrays.toString(vals));
            }
            else
            {
                SNMPWrapper.setValue(Manager.snmpAgent, MDMIB.POS_X, vals[0]);
                SNMPWrapper.setValue(Manager.snmpAgent, MDMIB.POS_Y, vals[1]);
                SNMPWrapper.setValue(Manager.snmpAgent, MDMIB.ANGLE, vals[2]);
            }

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
