package com.harris.cinnato.outputs;

import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

// wt:
import java.sql.*;
import org.json.XML;
import org.json.JSONArray;
import org.json.JSONObject;

public class PostgOutput extends Output {
    java.sql.Connection pgconn = null;

    public PostgOutput(Config config) {
        super(config);
        // wt:
        String url = "jdbc:postgresql://localhost:5432/ciwsdb";
        try {
            pgconn = DriverManager.getConnection(url, "ciwsuser", "weather");
            pgconn.setAutoCommit(false);
            System.out.println("Connected to the PostgreSQL server successfully.");

        } catch (Exception e) {   // SQLException???
            System.err.println( "+++++++++++++++++++++++++++++++++++++++++"  );
            System.out.println("NOT Connected to the PostgreSQL server");
            System.err.println( e.getClass().getName()+"::" + e.getMessage() );
            System.err.println( "+++++++++++++++++++++++++++++++++++++++++"  );
            System.exit(1);
        }
    }

    @Override
    public void output(String message) {

        // convert xml object to json object
        JSONObject jmsg         = (JSONObject) XML.toJSONObject(message);
        JSONObject taTrack      = null;
        JSONArray  recordsArray = null;

        try {
            taTrack = (JSONObject) jmsg.get("ns2:TATrackAndFlightPlan");
        } catch (Exception e) {
            // System.out.println("msg is not a TATrackAndFlightPlan");
            return;
        }

        try {
            recordsArray= (JSONArray) taTrack.get("record");
        } catch (Exception e) {
            // System.out.println("msg is not a TATrackAndFlightPlan");
            return;
        }

        int cnt = 0;
        for(int k=0; k < recordsArray.length(); k++){
            JSONObject trackElem = null;

            JSONObject elem      = (JSONObject) recordsArray.get(k);
            try {
                trackElem     = (JSONObject) elem.get("track");
                Double lon    = (Double)     trackElem.get("lon");
                Double lat    = (Double)     trackElem.get("lat");
                Integer tnum  = (Integer)    trackElem.get("trackNum");
                String mrt    = (String)     trackElem.get("mrtTime");
                Integer alt   = (Integer)    trackElem.get("reportedAltitude");

                PreparedStatement s = pgconn.prepareStatement(
                    "INSERT INTO asdex (track, ptime, lon, lat, altitude, position) VALUES " +
                    "(" + tnum +
                    ", '" + mrt + "'" +
                    "," + lon + ',' + lat + ',' + alt +
                    ", ST_GeomFromEWKT('SRID=4326;POINT(" + lon + ' ' +  lat + ")'));");

                // System.out.println(s);

                int rows = s.executeUpdate();
                pgconn.commit();

                if (rows > 0) {
                    // System.out.println(" Successful insert! ");
                    // System.out.println(rows);
                } else {
                    System.out.println(" Failed insert!");
                }
                cnt += 1;

            } catch (Exception e) {  // either mal-formed asdex msg OR bad insert
                //System.err.println( "+++++++++++++++++++++++++++++++++++++++++"  );
                System.out.println("NOT inserted to PostgreSQL");
                System.err.println( e.getClass().getName()+"::" + e.getMessage() );
                //System.err.println( "+++++++++++++++++++++++++++++++++++++++++"  );
                //System.exit(0);
            }
        }
        System.out.println("TATrack msg inserted:" + cnt);
    }
}
