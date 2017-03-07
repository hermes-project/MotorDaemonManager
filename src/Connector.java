import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.net.Socket;

/**
 * Class representing any system connected to the manager
 */
public class Connector extends Thread
{
    private Socket socket;

    private DataInputStream input;

    private DataOutputStream output;

    private Connector target;

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

                if(target != null && target.isConnected())
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setTarget(Connector s)
    {
        this.target = s;
    }
}
