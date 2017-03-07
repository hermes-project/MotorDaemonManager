import java.io.*;

import java.net.Socket;
import java.util.Arrays;

/**
 * Class representing any system connected to the manager
 */
public class Connector extends Thread
{
    private Socket socket;

    private DataInputStream input;

    private DataOutputStream output;

    private Connector target;

    private boolean canOrder = false;

    Connector(Socket s, Connector target) throws IOException
    {
        this.target = target;
        this.socket = s;

        input = new DataInputStream(socket.getInputStream());
        output = new DataOutputStream(socket.getOutputStream());

        if(!checkConnection())
        {
            System.out.println("BAD SYSTEM TRIED TO CONNECT : "+socket.getInetAddress().toString());
            socket.close();
            return;
        }

        this.start();
    }

    Connector(Socket s, Connector target, boolean canOrder) throws IOException
    {
        this.target = target;
        this.socket = s;
        this.canOrder = canOrder;

        input = new DataInputStream(socket.getInputStream());
        output = new DataOutputStream(socket.getOutputStream());

        if(!checkConnection())
        {
            System.out.println("BAD SYSTEM TRIED TO CONNECT : "+socket.getInetAddress().toString());
            socket.close();
            return;
        }

        this.start();
    }

    private synchronized boolean checkConnection()
    {
        byte[] b = new byte[11];

        try {
            input.readFully(b);
            String s = new String(b);

            return s.contains("motordaemon");

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void run()
    {
        while(socket.isConnected())
        {
            try
            {
                byte[] in = new byte[1024];
                int rbytes = input.read(in);

                String s = new String(in);
                s = s.replace("\0", "");

                if(target != null && target.isConnected() && !specialTreatment(s))
                {
                    target.write(s.getBytes());
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
                try {
                    socket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                return;
            }
        }
    }

    public boolean isConnected()
    {
        return socket.isConnected();
    }

    public synchronized void write(byte[] b)
    {
        if(!socket.isConnected()) return;

        try {
            output.write(b);
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setTarget(Connector s)
    {
        this.target = s;
    }

    private boolean specialTreatment(String order)
    {
        if(!canOrder) return false;

        if(order.contains("goto"))
        {
            Double[] pos = target.getPosition();

            String[] args = order.split(" ");

            double gotoX = Double.parseDouble(args[1]);
            double gotoY = Double.parseDouble(args[2]);
            double gotoA = Double.parseDouble(args[3]);

            //TODO call to PF & followpath

            return true;
        }

        return false;
    }

    public Double[] getPosition()
    {
        String[] sl = sendAndReceive("p", 1);

        if(sl == null) return null;

        String[] vals = sl[0].split(";");

        double x = Double.parseDouble(vals[0]);
        double y = Double.parseDouble(vals[1]);
        double o = Double.parseDouble(vals[2].replace("\r", "").replace("\n", ""));

        return new Double[]{x,y,o};
    }

    public synchronized void send(String s)
    {
        try {
            byte[] r = Arrays.copyOfRange(s.getBytes(), 0, 1024);

            output.write(r);
            output.flush();

            Thread.sleep(20);
        } catch (IOException|InterruptedException e) {
            e.printStackTrace();
        }
    }

    public synchronized String[] sendAndReceive(String toSend, int numberOfLines)
    {
        send(toSend);

        String[] out = new String[numberOfLines];

        try {

            BufferedReader iss = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            for(int i=0 ; i<numberOfLines ; i++)
            {
                out[i] = iss.readLine().replace("\0","");
                Thread.sleep(20);
            }

        } catch (IOException|InterruptedException e) {
            e.printStackTrace();
        }

        return out;
    }
}
