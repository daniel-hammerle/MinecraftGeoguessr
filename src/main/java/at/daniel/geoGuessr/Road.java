package at.daniel.geoGuessr;

import org.bukkit.util.Vector;

import java.util.Random;

public record Road(Vector[] waypoints, RoadKind kind, double[] lengths, double totalLength) {
    public static Road create(Vector[] waypoints, RoadKind kind) {
        double[] lengths =  new double[waypoints.length - 1];
        double totalLength = 0;
        for(int i = 1; i < waypoints.length; i++) {
           Vector previous = waypoints[i-1];
           Vector current = waypoints[i];

           double length = previous.distance(current);
           totalLength += length;
           lengths[i - 1] = length;
        }

        return new Road(waypoints, kind, lengths, totalLength);
    }

    public Vector randomPoint(Random random) {
        double result = random.nextDouble()*totalLength;

        //choose a length / waypoint

        int i = 0;
        for (double acc = 0; i < lengths.length; ++i) {
            acc += lengths[i];
            if (result < acc) {
                break;
            }
        }

        Vector beg = waypoints[i];
        Vector end = waypoints[i + 1];
        Vector dir = end.subtract(beg);

        double factor = random.nextDouble() * lengths[i] / dir.length();

        return beg.add(dir.multiply(factor));
    }

}
