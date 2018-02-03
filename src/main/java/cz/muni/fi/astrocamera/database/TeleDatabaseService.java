/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.muni.fi.astrocamera.database;

import cz.muni.fi.astrocamera.entity.TeleData;
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
public class TeleDatabaseService implements DatabaseService {

    private final static Logger logger = Logger.getLogger(TeleDatabaseService.class);
    private Path path; //absolute path to DB file
    private TeleData teledata; //entity for keeping meteorogical data
    private Thread t;
    private volatile Boolean ready;
    private javax.swing.JTextArea textArea;
    private javax.swing.JProgressBar teleProgressBar;
    private Date date;
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    public TeleDatabaseService(Path path, TeleData teledata) {
        this.path = path;
        this.teledata = teledata;
        this.ready = false;
    }

    public JTextArea getTextArea() {
        return textArea;
    }

    public void setTextArea(JTextArea textArea) {
        this.textArea = textArea;
    }

    public TeleDatabaseService() {
        this.ready = false;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public TeleData getTeledata() {
        return teledata;
    }

    public void setTeledata(TeleData teledata) {
        this.teledata = teledata;
    }

    public Boolean getReady() {
        return ready;
    }

    public void setReady(Boolean ready) {
        this.ready = ready;
    }

    public JProgressBar getTeleProgressBar() {
        return teleProgressBar;
    }

    public void setTeleProgressBar(JProgressBar teleProgressBar) {
        this.teleProgressBar = teleProgressBar;
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

    /**
     * Retrieves data from teledatabase into set teledata structure
     */
    private void retrieveData() {
        logger.debug("teleDbService()");
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
            textArea.insert(sdf.format(date) + " " + ResourceBundle.getBundle("cz/muni/fi/astrocamera/ui/Bundle").getString("TeleDatabaseService.error.db"), 0);
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
                statement1.setQueryTimeout(15);  // set timeout to 15 sec.
                statement2.setQueryTimeout(15);

                // choose latest value in table
                ResultSet rsDec = statement1.executeQuery("select * from TELE_SKY_DEC_ACT_TREND order by TIME desc limit 1"); //declination
                ResultSet rsRa = statement2.executeQuery("select * from TELE_SKY_RA_ACT_TREND order by TIME desc limit 1"); //right ascencion                

                // read the result set and set values 
                teledata.setDecNum(rsDec.getString("VALUE"));
                teledata.setRaNum(rsRa.getString("VALUE"));

                connection.close();
                logger.info("tele db retrieved");

                teleProgressBar.setValue(1);
                sleep(2000); //adjust this delay for more or less frequent updates
                teleProgressBar.setValue(2);
            } catch (SQLException ex) {
                errorCount = errorCount + 2;
                if (errorCount > 2) { //to avoid excesive spamming when the error is just simultaneous access to DB
                    date = new Date();
                    textArea.insert(sdf.format(date) + " " + ResourceBundle.getBundle("cz/muni/fi/astrocamera/ui/Bundle").getString("TeleDatabaseService.error.sql") + ex.getMessage() + "\n", 0);
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
                    textArea.insert(sdf.format(date) + " " + ResourceBundle.getBundle("cz/muni/fi/astrocamera/ui/Bundle").getString("TeleDatabaseService.error.db") + ex.getMessage() + "\n", 0);
                }
                teleProgressBar.setValue(0);
            }
        }//while
        logger.debug("teleDbService() - while cycle broke");
    }

    private void validate() {
        if (teledata == null) {
            throw new IllegalArgumentException("Meteodata is null.");
        }
        if (path == null) {
            throw new IllegalArgumentException("Path is null.");
        }
    }

    @Override
    public void run() {
        retrieveData();
    }

    public void start() {
        if (t == null) {
            ready = true;
            t = new Thread(this);
            t.start();
        } else {
            if (ready == false && teleProgressBar.getValue() == 0) {
                ready = true;
                t = new Thread(this);
                t.start();
            }
        }
    }

}
