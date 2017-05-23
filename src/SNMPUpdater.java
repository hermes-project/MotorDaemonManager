import snmp.MDMIB;
import snmp.SNMPWrapper;

import java.util.Arrays;

/**
 * SNMP Updater
 */
public class SNMPUpdater extends Thread
{

    private final Connector parent;

    private double posX = 0;
    private double posY = 0;
    private double angle = 0;

    SNMPUpdater(Connector conn)
    {
        this.parent = conn;
    }

    public Double[] getPos()
    {
        Double[] res = new Double[3];
        res[0] = posX;
        res[1] = posY;
        res[2] = angle;

        return res;
    }

    @Override
    public void run()
    {
        System.out.println("SNMP Updater launched");
        while (parent.isConnected())
        {
            String[] vals = parent.getStatus().split(";");
            if(vals.length != 8)
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

                SNMPWrapper.setValue(Manager.snmpAgent, MDMIB.POS_X, vals[5]);
                posX = Double.parseDouble(vals[5]);

                SNMPWrapper.setValue(Manager.snmpAgent, MDMIB.POS_Y, vals[6]);
                posY = Double.parseDouble(vals[6]);

                SNMPWrapper.setValue(Manager.snmpAgent, MDMIB.ANGLE, vals[7]);
                angle = Double.parseDouble(vals[7]);

            }




            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                System.out.println("INTERRUPTED : Triggering force update");
            }
        }
    }

}
