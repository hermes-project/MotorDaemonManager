package snmp;

import org.snmp4j.smi.OID;

/**
 * Wrapper for SNMP
 */
public class SNMPWrapper
{
    public static void setValue(SNMPAgent agent, MDMIB what, Object value)
    {
        agent.updateManagedObject(MOCreator.createReadOnly(new OID(what.getOid()), value));
    }

    public static void initMIB(SNMPAgent agent)
    {
        agent.registerManagedObject(MOCreator.createReadOnly(new OID(MDMIB.STATE.getOid()), "false"));
        agent.registerManagedObject(MOCreator.createReadOnly(new OID(MDMIB.POS_X.getOid()), "0"));
        agent.registerManagedObject(MOCreator.createReadOnly(new OID(MDMIB.POS_Y.getOid()), "0"));
        agent.registerManagedObject(MOCreator.createReadOnly(new OID(MDMIB.BATT.getOid()), "Unknown"));
        agent.registerManagedObject(MOCreator.createReadOnly(new OID(MDMIB.CAMERA.getOid()), "false"));
        agent.registerManagedObject(MOCreator.createReadOnly(new OID(MDMIB.CPU_LOAD.getOid()), "Unknown"));
        agent.registerManagedObject(MOCreator.createReadOnly(new OID(MDMIB.CPU_TEMP.getOid()), "Unknown"));
        agent.registerManagedObject(MOCreator.createReadOnly(new OID(MDMIB.RAM_USED.getOid()), "Unknown"));
        agent.registerManagedObject(MOCreator.createReadOnly(new OID(MDMIB.RAM_TOTAL.getOid()), "Unknown"));
        agent.registerManagedObject(MOCreator.createReadOnly(new OID(MDMIB.SPEED.getOid()), "Unknown"));
        agent.registerManagedObject(MOCreator.createReadOnly(new OID(MDMIB.ANGLE.getOid()), "0"));
    }
}
