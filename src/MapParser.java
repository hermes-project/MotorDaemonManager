import libpf.obstacles.types.Obstacle;
import libpf.obstacles.types.ObstacleCircular;
import libpf.obstacles.types.ObstacleRectangular;
import libpf.utils.Vec2RO;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Tool to parse JSON object to obstacles for the pf map
 */
public class MapParser
{
    public static List<Obstacle> parseMap(String json)
    {
        String[] objects = json.split("}");

        ArrayList<Obstacle> res = new ArrayList<>();

        for(String obj : objects)
        {
            obj += "}";
            try
            {
                JSONObject js = new JSONObject(obj);

                if (js.getString("type").equals("rectangle")) {
                    ObstacleRectangular o = new ObstacleRectangular(
                            new Vec2RO(js.getInt("x"), js.getInt("y")),
                            js.getInt("width"), js.getInt("height"),
                            js.getDouble("angle"));
                    res.add(o);
                } else if (js.getString("type").equals("circle")) {
                    ObstacleCircular o = new ObstacleCircular(
                            new Vec2RO(js.getInt("x"), js.getInt("y")),
                            js.getInt("rayon"));
                    res.add(o);
                } else {
                    throw new JSONException("No good type");
                }
            }
            catch (JSONException e)
            {
                System.err.println("BAD MAP OBJECT RECEIVED : " + obj);
                e.printStackTrace();
                System.err.println(e.getMessage());
            }
        }

        return res;
    }
}
