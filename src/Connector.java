import libpf.container.Container;
import libpf.exceptions.ContainerException;
import libpf.exceptions.PathfindingException;
import libpf.obstacles.types.Obstacle;
import libpf.pathfinding.astar.AStarCourbe;
import libpf.pathfinding.chemin.FakeCheminPathfinding;
import libpf.pathfinding.dstarlite.gridspace.PointGridSpace;
import libpf.robot.Cinematique;
import libpf.robot.CinematiqueObs;
import org.freedesktop.gstreamer.Bin;
import org.freedesktop.gstreamer.Bus;
import org.freedesktop.gstreamer.Pipeline;
import snmp.SNMPAgent;

import java.io.*;

import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Class representing any system connected to the manager
 */
public class Connector extends Thread
{
    private final String mutex = "mutex";

    private BufferedWriter out;

    private static final List<Obstacle> obstacles = new ArrayList<Obstacle>();

    private static Container container = null;

    private Socket socket = null;

    private DataInputStream input;

    private DataOutputStream output;

    private Connector target;

    private boolean started = false;

    private String status = "";

    private boolean connected = false;

    Pipeline pipe = new Pipeline();

    private boolean canOrder = false;

    SNMPUpdater updater;

    private SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    public String name;

    Connector()
    {
        if(out == null)
        {
            try {
                out = new BufferedWriter(new FileWriter("comm.log"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try
        {
            if(container == null) container = new Container(obstacles);
        }
        catch (ContainerException | InterruptedException e)
        {
            e.printStackTrace();
        }

        this.start();
    }

    synchronized void setInfos(String name, Socket s, Connector target, boolean canOrder) throws IOException
    {
        this.name = name;
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

        connected = true;
        started = true;
    }

    private synchronized boolean checkConnection()
    {
        if(socket == null) return false;

        byte[] b = new byte[65536];

        try {
            input.read(b);

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
        while(true)
        {
            if(!started)
            {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            if(!canOrder)
            {
                updater = new SNMPUpdater(this);
                updater.start();
            }

            while(socket.isConnected() && connected)
            {
                try
                {
                    byte[] in = new byte[65536];

                    int rbytes;

                    //synchronized (mutex)
                    //{
                    rbytes = input.read(in);
                    //}

                    if(rbytes < 0) throw new IOException();

                    String s = new String(in);
                    s = s.replace("\0", "");

                    if(specialTreatment(s))
                    {
                        out.write(sdfDate.format(new Date())+" : "+name+" -> MDM\n"+s+"\n\n");
                        out.flush();
                        continue;
                    }

                    if(target != null && target.isConnected())
                    {
                        out.write(sdfDate.format(new Date())+" : "+name+ " -> "+target.name+"\n"+s+"\n\n");
                        out.flush();

                        target.write(s.getBytes());
                    }
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    try {
                        socket.close();
                        pipe.stop();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    connected = false;
                    started = false;
                }
            }
            pipe.stop();
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            connected = false;
            socket = null;
            started = false;
        }
    }

    public boolean isConnected()
    {
        return socket != null && socket.isConnected() && connected && started;
    }

    public void write(byte[] b)
    {
        if(socket == null || !socket.isConnected()) return;

        synchronized (mutex)
        {
            try {
                output.write(b);
                output.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setTarget(Connector s)
    {
        this.target = s;
    }

    private boolean specialTreatment(String order)
    {
        if(!canOrder && !order.contains("MDSTATUS")) return false;

        if(order.contains("pos"))
        {
            Double[] actualPos = target.updater.getPos();
            try {
                out.write(sdfDate.format(new Date())+" : "+"MDM -> "+name+"\n"+(Double.toString(actualPos[0])+";"+Double.toString(actualPos[1])+";"+Double.toString(actualPos[2]))+"\\r\\n\n\n");
                out.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }
            write((Double.toString(actualPos[0])+";"+Double.toString(actualPos[1])+";"+Double.toString(actualPos[2])+"\r\n").getBytes());
            return true;
        }
        else if(order.contains("goto"))
        {
            String[] args = order.split(" ");

            if((args.length <= 4 || args[5].equals("1")) && !target.isConnected()) return true;

            // Forcing update
            target.updater.interrupt();

            // Waiting for update
            try
            {
                Thread.sleep(1500);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }

            double gotoX = Double.parseDouble(args[1]);
            double gotoY = Double.parseDouble(args[2]);
            double gotoA = Double.parseDouble(args[3]);

            try
            {
                FakeCheminPathfinding chemin = container.getService(FakeCheminPathfinding.class);
                AStarCourbe astar = container.getService(AStarCourbe.class);
                Cinematique arrivee = new Cinematique(gotoX, gotoY, gotoA, true, 0);
                Double[] actualPos = target.updater.getPos();
                Cinematique depart = new Cinematique(actualPos[0], actualPos[1], actualPos[2], true, 0);

                astar.initializeNewSearch(arrivee, true, depart);

                try {
                    astar.process(chemin);
                } catch (libpf.exceptions.PathfindingException e) {
                    e.printStackTrace();
                    // TODO gestion
                }
                System.out.println("The computed path is :");
                StringBuilder pathstr = new StringBuilder("followpath ");
                StringBuilder pathfeedback = new StringBuilder("");
                List<CinematiqueObs> path = chemin.getPath();
                int i = 0;
                boolean way = path.get(0).enMarcheAvant; // true = forward ; false = backward

                if(way) pathstr.append("way:forward;");
                else pathstr.append("way:backward;");

                for(CinematiqueObs c : path)
                {
                    if(way != c.enMarcheAvant)
                    {
                        way = c.enMarcheAvant;
                        if(way) pathstr.append("way:forward;");
                        else pathstr.append("way:backward;");
                        //i = 0;
                    }

                    System.out.println(c);
                    pathstr.append(String.format(Locale.US, "%d", (int) PointGridSpace.DISTANCE_ENTRE_DEUX_POINTS * ++i));
                    pathstr.append(":");
                    pathstr.append(String.format(Locale.US, "%d", (int)(1000./(c.courbureReelle+0.000000001))));
                    pathstr.append(";");

                    pathfeedback.append(String.format(Locale.US, "%f", c.getPosition().getX()));
                    pathfeedback.append(":");
                    pathfeedback.append(String.format(Locale.US, "%f", c.getPosition().getY()));
                    pathfeedback.append(";");

                }

                System.out.println("Sending to client : "+pathfeedback.toString().substring(0, pathfeedback.toString().length() - 1));

                if(args.length <= 4 || args[5].equals("1"))
                {
                    System.out.println("Sending to MD : "+pathstr.toString().substring(0, pathstr.toString().length() - 1));
                    target.send(pathstr.toString().substring(0, pathstr.toString().length() - 1));
                    out.write(sdfDate.format(new Date())+" : "+"MDM -> "+target.name+"\n"+pathstr.toString().substring(0, pathstr.toString().length() - 1)+"\n\n");
                }

                write((pathfeedback.toString().substring(0, pathfeedback.toString().length() - 1)+"\r\n").getBytes());
                out.write(sdfDate.format(new Date())+" : "+"MDM -> "+name+"\n"+(pathfeedback.toString().substring(0, pathfeedback.toString().length() - 1)+"\\r\\n\n\n"));
                out.flush();

            } catch (ContainerException | PathfindingException | InterruptedException | IOException e) {
                e.printStackTrace();
            }

            return true;
        }

        else if(order.contains("motordaemonstatus"))
        {
            write((Boolean.toString(target.isConnected())+"\r\n").getBytes());
            try {
                out.write(sdfDate.format(new Date())+" : "+"MDM -> "+name+"\n"+(Boolean.toString(target.isConnected())+"\\r\\n\n\n"));
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }

        else if(order.contains("MDSTATUS"))
        {
            status =  order.replaceAll("MDSTATUS", "");
            return true;
        }

        else if(order.contains("newmap"))
        {
            try {
                Connector.updateContainer(new Container(MapParser.parseMap(order.substring(6))));
            } catch (ContainerException | InterruptedException e) {
                e.printStackTrace();
            }

            target.send(order);
            try {
                out.write(sdfDate.format(new Date())+" : "+"MDM -> "+target.name+"\n"+order+"\n\n");
                out.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;
        }

        else if(order.contains("startwebcamera"))
        {
            if(!target.isConnected()) return true;

            System.out.println("Starting WEB Camera for Janus");

            String[] args = order.split(" ");
            if(args.length != 2) return true;

            target.send("startcamera");

            try {
                out.write(sdfDate.format(new Date())+" : "+"MDM -> "+target.name+"\nstartcamera\n\n");
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

            pipe = new Pipeline();
            Bin bin = Bin.launch("udpsrc port=56988 ! application/x-rtp, media=video, encoding-name=JPEG, clock-rate=90000, payload=26 ! rtpjitterbuffer ! rtpjpegdepay ! jpegdec ! timeoverlay ! videorate ! video/x-raw,framerate=20/1 ! videoconvert ! vp8enc cpu-used=16 end-usage=vbr target-bitrate=100000 token-partitions=3 static-threshold=1000 min-quantizer=0 max-quantizer=63 threads=2 error-resilient=1 ! rtpvp8pay ! udpsink host="+args[1]+" port=5004",true);
            pipe.add(bin);
            Bus bus = pipe.getBus();
            bus.connect((Bus.MESSAGE) (arg0, arg1) -> System.out.println(arg1.getStructure()));
            pipe.play();

            Manager.videoOn = true;

            return true;
        }

        return false;
    }

    public Double[] getPosition()
    {
        if(socket == null || !socket.isConnected()) return null;

        String[] sl = sendAndReceive("p", 1);

        if(sl == null) return null;

        String[] vals = sl[0].split(";");

        double x = Double.parseDouble(vals[0]);
        double y = Double.parseDouble(vals[1]);
        double o = Double.parseDouble(vals[2].replace("\r", "").replace("\n", ""));

        return new Double[]{x,y,o};
    }

    public void send(String s)
    {
        if(socket == null || !socket.isConnected()) return;

        synchronized (mutex)
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

    }

    public String[] sendAndReceive(String toSend, int numberOfLines)
    {
        synchronized (mutex)
        {
            send(toSend);

            if(numberOfLines <= 0) return new String[0];

            String[] out = new String[numberOfLines];

            try {

                BufferedReader iss = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                for(int i=0 ; i<numberOfLines ; i++)
                {
                    out[i] = iss.readLine().replace("\0","");
                    this.out.write(sdfDate.format(new Date())+" : "+name+" -> MDM"+"\n"+out[i]+"\n\n");
                    this.out.flush();
                    Thread.sleep(20);
                }

            } catch (IOException|InterruptedException e) {
                e.printStackTrace();
            }
            return out;
        }

    }

    public synchronized String getStatus()
    {
        send("status");

        while(status.equals(""))
        {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                System.out.println("INTERRUPTED : Triggering force update");
            }
        }

        String statusret = status;
        status = "";

        return statusret;
    }

    public synchronized static void updateContainer(Container c)
    {

    }

    public void fallbackToBase()
    {
        try
        {
            Thread.sleep(1000);

            FakeCheminPathfinding chemin = container.getService(FakeCheminPathfinding.class);
            AStarCourbe astar = container.getService(AStarCourbe.class);
            Cinematique arrivee = new Cinematique(MapParser.base.getX(), MapParser.base.getY(), MapParser.angleStart, true, 0);
            Double[] actualPos = target.updater.getPos();
            Cinematique depart = new Cinematique(actualPos[0], actualPos[1], actualPos[2], true, 0);

            astar.initializeNewSearch(arrivee, true, depart);

            try {
                astar.process(chemin);
            } catch (libpf.exceptions.PathfindingException e) {
                e.printStackTrace();
                // TODO gestion
            }
            System.out.println("The computed path is :");
            StringBuilder pathstr = new StringBuilder("followpath ");
            List<CinematiqueObs> path = chemin.getPath();
            int i = 0;
            boolean way = path.get(0).enMarcheAvant; // true = forward ; false = backward

            if(way) pathstr.append("way:forward;");
            else pathstr.append("way:backward;");

            for(CinematiqueObs c : path)
            {
                if(way != c.enMarcheAvant)
                {
                    way = c.enMarcheAvant;
                    if(way) pathstr.append("way:forward;");
                    else pathstr.append("way:backward;");
                    //i = 0;
                }

                System.out.println(c);
                pathstr.append(String.format(Locale.US, "%d", (int) PointGridSpace.DISTANCE_ENTRE_DEUX_POINTS * ++i));
                pathstr.append(":");
                pathstr.append(String.format(Locale.US, "%d", (int)(1000./(c.courbureReelle+0.000000001))));
                pathstr.append(";");

            }

            System.out.println("Sending to MD : "+pathstr.toString().substring(0, pathstr.toString().length() - 1));
            target.send(pathstr.toString().substring(0, pathstr.toString().length() - 1));
            out.write(sdfDate.format(new Date())+" : "+"MDM -> "+target.name+"\n"+pathstr.toString().substring(0, pathstr.toString().length() - 1)+"\n\n");
            out.flush();

        } catch (ContainerException | PathfindingException | InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

}
