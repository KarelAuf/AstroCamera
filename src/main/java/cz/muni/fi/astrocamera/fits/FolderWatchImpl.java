package cz.muni.fi.astrocamera.fits;

import cz.muni.fi.astrocamera.coordinates.AstroObjectServiceImpl;
import cz.muni.fi.astrocamera.entity.MeteoData;
import cz.muni.fi.astrocamera.entity.TeleData;
import java.io.IOException;
import static java.lang.Thread.sleep;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;
import javax.swing.JCheckBox;
import javax.swing.JTextArea;
import org.apache.log4j.Logger;

/**
 * This class provides watch over selected folder for newly created files
 *
 * @author Karel Auf
 */
public class FolderWatchImpl implements FolderWatch {

    private final static Logger logger = Logger.getLogger(AstroObjectServiceImpl.class);
    private Path path;
    private MeteoData meteodata;
    private TeleData teledata;
    private String objectName;
    private Thread t;
    private volatile Boolean ready;
    private javax.swing.JTextArea textArea;
    private javax.swing.JCheckBox altFileName;
    private Date date;
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    public FolderWatchImpl(Path path, MeteoData meteodata, TeleData teledata, String name) {
        this.path = path;
        this.meteodata = meteodata;
        this.teledata = teledata;
        this.objectName = name;
        this.ready = false;
    }

    public FolderWatchImpl() {
        this.ready = false;
        objectName = "";
    }

    public JTextArea getTextArea() {
        return textArea;
    }

    public void setTextArea(JTextArea textArea) {
        this.textArea = textArea;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public MeteoData getMeteodata() {
        return meteodata;
    }

    public void setMeteodata(MeteoData meteodata) {
        this.meteodata = meteodata;
    }

    public TeleData getTeledata() {
        return teledata;
    }

    public void setTeledata(TeleData teledata) {
        this.teledata = teledata;
    }

    public String getName() {
        return objectName;
    }

    public void setName(String name) {
        this.objectName = name;
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

    public JCheckBox getAltFileName() {
        return altFileName;
    }

    public void setAltFileName(JCheckBox altFileName) {
        this.altFileName = altFileName;
    }    
    

    @Override
    public void run() {
        watchDirectory();
    }

    /**
     * checks set folder in loop for new file creation if the new file is fits
     * file it will update it's header
     */
    private void watchDirectory() {
        logger.debug("watchDirectory()");
        //check if set path is folder
        try {
            validate();
        } catch (IOException ex) {
            // Folder does not exists
            date = new Date();
            textArea.insert(sdf.format(date) + " " + ResourceBundle.getBundle("cz/muni/fi/astrocamera/ui/Bundle").getString("FolderWatchImpl.error.file"), 0);
            logger.error("Folder does not exist. ", ex);
        } catch (IllegalArgumentException ex) {
            date = new Date();
            textArea.insert(sdf.format(date) + " " + ResourceBundle.getBundle("cz/muni/fi/astrocamera/ui/Bundle").getString("FolderWatchImpl.error.parameters"), 0);
            logger.error("Invalid parameters: ", ex);
        }

        logger.info("Watching path: " + path);
        date = new Date();
        textArea.insert(sdf.format(date) + " " + ResourceBundle.getBundle("cz/muni/fi/astrocamera/ui/Bundle").getString("FolderWatchImpl.info.start") + path + "\n", 0);
        //obtain filesystem on given path
        FileSystem fs = path.getFileSystem();
        FitsFileUpdateImpl ffu;

        // create new WatchService
        try (WatchService service = fs.newWatchService()) {
            //register path to WatchService
            path.register(service, ENTRY_CREATE);

            //start checking in loop
            WatchKey key = null;
            while (ready) {
                key = service.take();
                // Dequeueing events
                WatchEvent.Kind<?> kind = null;
                for (WatchEvent<?> watchEvent : key.pollEvents()) {
                    // Get the type of the event
                    kind = watchEvent.kind();
                    if (kind == OVERFLOW) {
                        continue; // loop
                    } else if (kind == ENTRY_CREATE) {
                        // A new Path was created
                        Path newPath = ((WatchEvent<Path>) watchEvent).context();
                        // Output
                        if (!(newPath.toString().contains(".orig")) 
                                && (newPath.toString().contains(".fits")) 
                                && !(newPath.toString().contains("__"))
                                && ready == true) {
                            System.out.println("New path created: " + path.toString() + "\\" + newPath.toString());                            
                            ffu = new FitsFileUpdateImpl(path.toString() + "\\" + newPath.toString(), meteodata, teledata, objectName, textArea, altFileName);
                            ffu.start();
                        }
                    }
                }

                if (!key.reset() || ready == false) {
                    break; // loop
                }
            }
            date = new Date();
            textArea.insert(sdf.format(date) + " " + ResourceBundle.getBundle("cz/muni/fi/astrocamera/ui/Bundle").getString("FolderWatchImpl.info.stop"), 0);
        } catch (IOException ex) {
            logger.error("IO Exception ", ex);
        } catch (InterruptedException ex) {
            logger.error("Interrupted ", ex);
        }

    }

    private void validate() throws IOException {
        Boolean isFolder = (Boolean) Files.getAttribute(path, "basic:isDirectory", NOFOLLOW_LINKS);
        if (!isFolder) {
            date = new Date();
            textArea.insert(sdf.format(date) + " " + ResourceBundle.getBundle("cz/muni/fi/astrocamera/ui/Bundle").getString("FolderWatchImpl.error.path1")
                    + path + ResourceBundle.getBundle("cz/muni/fi/astrocamera/ui/Bundle").getString("FolderWatchImpl.error.path2"), 0);
            throw new IllegalArgumentException("Path: " + path + " is not a folder");
        }
        if (meteodata == null) {
            throw new IllegalArgumentException("Meteodata is null.");
        }
        if (teledata == null) {
            throw new IllegalArgumentException("Teledata is null.");
        }
    }

    @Override
    public void stop() {
        logger.debug("stop()");
        setReady(false);
        byte data[] = new byte[]{(byte) 0x00};
        Path file = Paths.get(path.toString() + "\\end.f");
        try {
            Files.write(file, data);
            sleep(100);
            Files.delete(file);
        } catch (IOException ex) {
            logger.error("IO Exception when terminating thread. ", ex);
        } catch (InterruptedException ex) {
            logger.error("Interrupted. ", ex);
        }
    }

    public void start() {
        if (t == null) {
            ready = true;
            t = new Thread(this);
            t.start();
        } else {
            if (ready == false) {
                ready = true;
                t = new Thread(this);
                t.start();
            }
        }
    }

}
