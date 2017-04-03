package snmp;

/**
 * MotorDaemon's MIB
 */
public enum MDMIB
{
    STATE("1.3.6.1.4.1.56987.1.1"),
    POS_X("1.3.6.1.4.1.56987.2.1"),
    POS_Y("1.3.6.1.4.1.56987.3.1"),
    BATT("1.3.6.1.4.1.56987.4.1"),
    CAMERA("1.3.6.1.4.1.56987.5.1"),
    CPU_LOAD("1.3.6.1.4.1.56987.6.1"),
    CPU_TEMP("1.3.6.1.4.1.56987.7.1"),
    RAM_USED("1.3.6.1.4.1.56987.8.1"),
    RAM_TOTAL("1.3.6.1.4.1.56987.9.1"),
    SPEED("1.3.6.1.4.1.56987.10.1"),
    ANGLE("1.3.6.1.4.1.56987.11.1")
    ;

    private String oid;

    MDMIB(String oid) {this.oid=oid;}

    public String getOid() {return oid;}
}
