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
        while (parent.isUp())
        {
            synchronized (parent)
            {
                //TODO
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
