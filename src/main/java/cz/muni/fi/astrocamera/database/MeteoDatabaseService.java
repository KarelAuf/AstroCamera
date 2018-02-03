package cz.muni.fi.astrocamera.database;

import cz.muni.fi.astrocamera.entity.MeteoData;
import java.io.File;
import java.io.FileFilter;
import static java.lang.Thread.sleep;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import org.apache.log4j.Logger;

/**
 *
 * @author Karel Auf
 */
public class MeteoDatabaseService implements DatabaseService {

    private final static Logger logger = Logger.getLogger(MeteoDatabaseService.class);

    private Path path; //absolute path to DB file
    private MeteoData meteodata; //entity for keeping meteorogical data
    private Thread t;
    private volatile Boolean ready;
    private javax.swing.JTextArea textArea;
    private javax.swing.JProgressBar meteoProgressBar;
    private Date date;
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    public MeteoDatabaseService(Path path, MeteoData meteodata) {
        this.path = path;
        this.meteodata = meteodata;
        this.ready = false;
    }

    public JTextArea getTextArea() {
        return textArea;
    }

    public void setTextArea(JTextArea textArea) {
        this.textArea = textArea;
    }

    public MeteoDatabaseService() {
        this.ready = false;
    }

    public MeteoData getMeteodata() {
        return meteodata;
    }

    public void setMeteodata(MeteoData meteodata) {
        this.meteodata = meteodata;
    }

    public Boolean getReady() {
        return ready;
    }

    public void setReady(Boolean ready) {
        this.ready = ready;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public JProgressBar getMeteoProgressBar() {
        return meteoProgressBar;
    }

    public void setMeteoProgressBar(JProgressBar meteoProgressBar) {
        this.meteoProgressBar = meteoProgressBar;
    }

    @Override
    public void run() {
        retrieveData();
    }

    /**
     * Retrieves data from meteodatabase into set meteodata structure
     */
    private void retrieveData() {
        logger.debug("meteoDbService()");
        String stringPath = "";
        try {
            validate();
        } catch (IllegalArgumentException ex) {
            logger.error("Invalid parameters", ex);
        }
        File dbFile;
        dbFile = lastFileModified(path.toString());
        if (dbFile == null) {
            logger.error("No File Found");
            date = new Date();
            textArea.insert(sdf.format(date) + " " + ResourceBundle.getBundle("cz/muni/fi/astrocamera/ui/Bundle").getString("MeteoDatabaseService.error.db"), 0);
            return;
        }
        int errorCount = 0;
        stringPath = dbFile.getAbsolutePath();
        while (ready) {
            if (errorCount > 0) {
                errorCount--;
            }
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + stringPath)) {
                Statement statement1 = connection.createStatement();
                Statement statement2 = connection.createStatement();
                Statement statement3 = connection.createStatement();
                Statement statement4 = connection.createStatement();
                Statement statement5 = connection.createStatement();
                Statement statement6 = connection.createStatement();
                statement1.setQueryTimeout(15);  // set timeout to 15 sec.
                statement2.setQueryTimeout(15);
                statement3.setQueryTimeout(15);
                statement4.setQueryTimeout(15);
                statement5.setQueryTimeout(15);
                statement6.setQueryTimeout(15);

                // choose latest value in table
                ResultSet rsTempIn = statement1.executeQuery("select * from IN_GENE_TEMPERATURE_IN_DOME_AI order by TIME desc limit 1"); //temperature in dome                
                ResultSet rsPress = statement2.executeQuery("select * from IN_GENE_BAROMETRIC_PRESSURE_AI order by TIME desc limit 1"); //pressure
                ResultSet rsWind = statement3.executeQuery("select * from METEO_WIND_SPEED order by TIME desc limit 1"); //wind speed
                ResultSet rsHumi = statement4.executeQuery("select * from METEO_HUMIDITY order by TIME desc limit 1"); //humidity
                ResultSet rsPyrg = statement5.executeQuery("select * from IN_METEO_PYRGEOMETER_AI order by TIME desc limit 1"); //pyrgeometer
                ResultSet rsTempOut = statement6.executeQuery("select * from METEO_TEMPERATURE order by TIME desc limit 1"); //temperature in dome

                // read the result set and set values 
                meteodata.setHumidity(rsHumi.getString("VALUE"));
                meteodata.setPressure(rsPress.getString("VALUE"));
                meteodata.setPyrgeometer(rsPyrg.getString("VALUE"));
                meteodata.setTemperatureInside(rsTempIn.getString("VALUE"));
                meteodata.setTemperatureOutside(rsTempOut.getString("VALUE"));
                meteodata.setWind(rsWind.getString("VALUE"));
                meteodata.setTime(rsTempIn.getBigDecimal("TIME"));
                connection.close();
                logger.info("meteo db retrieved");
                meteoProgressBar.setValue(1);
                sleep(2000); //adjust this delay for more or less frequent updates
                meteoProgressBar.setValue(2);
            } catch (SQLException ex) {
                errorCount = errorCount + 2;
                if (errorCount > 2) { //to avoid excesive spamming when the error is just simultaneous access to DB
                    date = new Date();
                    textArea.insert(sdf.format(date) + " " + ResourceBundle.getBundle("cz/muni/fi/astrocamera/ui/Bundle").getString("MeteoDatabaseService.error.sql") + ex.getMessage() + "\n", 0);
                }
                logger.error("SQL Error", ex);
            } catch (InterruptedException ex) {
                textArea.insert(ex.toString() + "\n", 0);
                logger.error("Sleep error", ex);
            } finally {
                try {
                    sleep(3000);
                    dbFile = lastFileModified(path.toString());
                    if (!(dbFile.getAbsolutePath().length() < stringPath.length() - 1)
                            || !(dbFile.getAbsolutePath().length() > stringPath.length() + 1)) {
                        stringPath = dbFile.getAbsolutePath();
                    }
                } catch (InterruptedException ex) {
                    logger.error("Sleep error", ex);
                } catch (NullPointerException ex) {
                    logger.error("No File Found", ex);
                    date = new Date();
                    textArea.insert(sdf.format(date) + " " + ResourceBundle.getBundle("cz/muni/fi/astrocamera/ui/Bundle").getString("MeteoDatabaseService.error.db") + ex.getMessage() + "\n", 0);
                }
                meteoProgressBar.setValue(0);
            }
        }//while
        logger.debug("meteoDbService() - while cycle broke");
    }

    /**
     *
     * @param dir String representing directory path
     * @return file which was last modified
     */
    private File lastFileModified(String dir) {
        logger.debug("lastFileModified()");
        File fl = new File(dir);
        File[] files = fl.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isFile();
            }
        });
        long lastMod = Long.MIN_VALUE;
        File choice = null;
        for (File file : files) {
            if (file.lastModified() > lastMod) {
                choice = file;
                lastMod = file.lastModified();
            }
        }
        return choice;
    }

    private void validate() {
        if (meteodata == null) {
            throw new IllegalArgumentException("Meteodata is null.");
        }
        if (path == null) {
            throw new IllegalArgumentException("Path is null.");
        }
    }

    public void start() {
        if (t == null) {
            ready = true;
            t = new Thread(this);
            t.start();
        } else {
            if (ready == false && meteoProgressBar.getValue() == 0) {
                ready = true;
                t = new Thread(this);
                t.start();
            }
        }
    }

}
