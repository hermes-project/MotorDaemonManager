import libpf.obstacles.types.Obstacle;
import libpf.obstacles.types.ObstacleCircular;
import libpf.obstacles.types.ObstacleRectangular;
import libpf.utils.Vec2RO;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Tool to parse JSON object to obstacles for the pf map
 */
public class MapParser
{

    public static Vec2RO base;

    public static double angleStart;

    public static List<Obstacle> parseMap(String jsonString)
    {
        ArrayList<Obstacle> res = new ArrayList<>();

        JSONObject json = new JSONObject(jsonString);

        JSONObject map = json.getJSONObject("map");

        JSONArray objects = json.getJSONArray("obstacles");

        double scale = map.getDouble("scale");

        base = new Vec2RO((int)(map.getInt("xstart")*scale), (int)(map.getInt("ystart")*scale));

        angleStart = map.getDouble("anglestart");

        for(int i=0 ; i < objects.length() ; i++)
        {
            JSONObject js = objects.getJSONObject(i);

            try
            {
                if (js.getString("type").equals("rectangle")) {
                    ObstacleRectangular o = new ObstacleRectangular(
                            new Vec2RO(js.getInt("xcenter")*scale, js.getInt("ycenter")*scale),
                            (int)(js.getInt("width")*scale), (int)(js.getInt("height")*scale),
                            js.getDouble("angle"));
                    res.add(o);
                } else if (js.getString("type").equals("circle")) {
                    ObstacleCircular o = new ObstacleCircular(
                            new Vec2RO(js.getInt("xcenter")*scale, js.getInt("ycenter")*scale),
                            (int)(js.getInt("rayon")*scale));
                    res.add(o);
                } else {
                    throw new JSONException("No good type");
                }
            }
            catch (JSONException e)
            {
                System.err.println("BAD MAP OBJECT RECEIVED : " + js.toString());
                e.printStackTrace();
                System.err.println(e.getMessage());
            }
        }

        return res;
    }
}
