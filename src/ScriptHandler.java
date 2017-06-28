import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Allows the user to create scripts on text files
 */
public class ScriptHandler
{

    private static final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");


    public static boolean scriptLaunch(Connector target, Connector feedback, String scriptname)
    {
        if(target == null)
        {
            System.err.println("TARGET IS NULL "+scriptname);
            return false;
        }
        else if(!target.isConnected())
        {
            System.err.println("TARGET IS NOT CONNECTED "+scriptname);
            return false;
        }

        try
        {
            BufferedReader in = new BufferedReader(new FileReader(scriptname));

            String line;

            while ((line = in.readLine()) != null)
            {
                if (target.specialTreatment(line))
                {
                    target.out.write(sdfDate.format(new Date()) + " : Script ("+scriptname+") -> MDM\n" + line + "\n\n");
                    target.out.flush();
                }
                else
                {
                    target.out.write(sdfDate.format(new Date()) + " : Script ("+scriptname+") -> " + target.name + "\n" + line + "\n\n");
                    target.out.flush();
                    target.write(line.getBytes());
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return true;
    }

    public static boolean scriptLaunch(Connector target, String filename)
    {
        return scriptLaunch(target, null,  filename);
    }
}
