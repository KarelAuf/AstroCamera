package cz.muni.fi.astrocamera.ui;

import cz.muni.fi.astrocamera.coordinates.AstroObjectServiceImpl;
import cz.muni.fi.astrocamera.database.MeteoDatabaseService;
import cz.muni.fi.astrocamera.fits.FolderWatchImpl;
import cz.muni.fi.astrocamera.database.TeleDatabaseService;
import cz.muni.fi.astrocamera.coordinates.TelescopeControlImpl;
import cz.muni.fi.astrocamera.entity.AstronomicalObject;
import cz.muni.fi.astrocamera.entity.MeteoData;
import cz.muni.fi.astrocamera.entity.TeleData;
import cz.muni.fi.astrocamera.entity.Telescope;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.swing.JFileChooser;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.apache.log4j.Logger;

/**
 *
 * @author Karel Auf
 */
public class AstroCameraUI extends javax.swing.JFrame {

    private final static Logger logger = Logger.getLogger(AstroCameraUI.class);
    private final JFileChooser fcMeteo;
    private final JFileChooser fcTele;
    private final JFileChooser fcPhoto;
    private final JFileChooser fcCoord;
    private AstroObjectServiceImpl service;

    private final MeteoData meteodata;
    private final TeleData teledata;
    private final MeteoDatabaseService meteoDbServ;
    private final TeleDatabaseService teleDbServ;
    private final FolderWatchImpl fw;
    private final Telescope telescope;
    private final TelescopeControlImpl telescopeControl;
    private AstronomicalObject astroObject;
    private Path coordFilePath;
    private String serverIP;
    private int serverPort;
    private String serverPassword;
    private ResourceBundle bundle = ResourceBundle.getBundle("cz/muni/fi/astrocamera/ui/Bundle");
    private Date date;
    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    /**
     * Creates new form AstroCameraUI
     */
    public AstroCameraUI() {
        service = new AstroObjectServiceImpl();

        fcMeteo = new JFileChooser();
        fcMeteo.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        fcTele = new JFileChooser();
        fcTele.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        fcPhoto = new JFileChooser();
        fcPhoto.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        fcCoord = new JFileChooser();
        fcCoord.setFileSelectionMode(JFileChooser.FILES_ONLY);

        meteodata = new MeteoData();
        meteoDbServ = new MeteoDatabaseService();
        meteoDbServ.setMeteodata(meteodata);

        teledata = new TeleData();
        teleDbServ = new TeleDatabaseService();
        teleDbServ.setTeledata(teledata);

        fw = new FolderWatchImpl();
        fw.setMeteodata(meteodata);
        fw.setTeledata(teledata);
        

        telescope = new Telescope();

        telescopeControl = new TelescopeControlImpl();

        astroObject = new AstronomicalObject();
        loadLocale();
        initComponents();
        teleDbServ.setTeleProgressBar(teleProgressBar);
        meteoDbServ.setMeteoProgressBar(meteoProgressBar);
        fw.setAltFileName(AltFileNameCheckBox);
        loadProperties();
    }

    /**
     * Loads properties from file config.properties and set corresponding
     * variables to loaded values
     *
     * config.properties is placed in same folder as application
     */
    private void loadProperties() {
        Properties prop = new Properties();
        InputStream input = null;
        try {
            File f = new File("config.properties");
            input = new FileInputStream(f);
            prop.load(input);

            serverIP = prop.getProperty("ip");
            serverPort = Integer.parseInt(prop.getProperty("port"));
            serverPassword = prop.getProperty("password");

            fcPhoto.setCurrentDirectory(new File(prop.getProperty("photo")));
            File file = new File(prop.getProperty("photo"));
            fw.setReady(false);
            photoTextField.setText(file.getAbsolutePath());
            Path p = Paths.get(file.getAbsolutePath());
            fw.setPath(p);

            fcMeteo.setCurrentDirectory(new File(prop.getProperty("meteo")));
            file = new File(prop.getProperty("meteo"));
            meteoDbServ.setReady(false);
            meteoTextField.setText(file.getAbsolutePath());
            p = Paths.get(file.getAbsolutePath());
            meteoDbServ.setPath(p);

            fcTele.setCurrentDirectory(new File(prop.getProperty("tele")));
            file = new File(prop.getProperty("tele"));
            teleTextField.setText(file.getAbsolutePath());
            p = Paths.get(file.getAbsolutePath());
            teleDbServ.setPath(p);

            teledata.setLongitude(prop.getProperty("longitude"));
            teledata.setLatitude(prop.getProperty("latitude"));
            teledata.setElevation(prop.getProperty("elevation"));

            file = new File(prop.getProperty("coord"));
            fcCoord.setCurrentDirectory(file);
            p = Paths.get(file.getAbsolutePath());
            coordTextField.setText(file.getAbsolutePath());
            coordFilePath = p;

            if (Locale.getDefault().equals(new Locale("cs", "CZ"))) {
                languageComboBox.setSelectedIndex(1);
            } else {
                languageComboBox.setSelectedIndex(0);
            }

        } catch (Exception ex) {
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ex) {
                    logger.error("Error loading configuration from file: ", ex);
                    date = new Date();
                    logTextArea.insert(sdf.format(date) + " " + ResourceBundle.getBundle("cz/muni/fi/astrocamera/ui/Bundle").getString("AstroCameraUI.error.load"), 0);
                }
            }
        }
    }

    /**
     * Saves current settings of variables into configuration file
     * config.properties
     *
     * config.properties is placed in same folder as application
     */
    private void saveProperties() {
        try {
            Properties props = new Properties();
            props.setProperty("ip", serverIP);
            props.setProperty("port", Integer.toString(serverPort));
            props.setProperty("password", serverPassword);

            props.setProperty("photo", fw.getPath().toString());
            props.setProperty("meteo", meteoDbServ.getPath().toString());
            props.setProperty("tele", teleDbServ.getPath().toString());
            props.setProperty("coord", coordFilePath.toString());

            if (teledata.getLongitude() != null) {
                props.setProperty("longitude", teledata.getLongitude());
                props.setProperty("latitude", teledata.getLatitude());
                props.setProperty("elevation", teledata.getElevation());
            }
            Locale loc = Locale.getDefault();
            if (loc.equals(new Locale("cs", "CZ"))) {
                props.setProperty("localization", "Czech");
            } else {
                props.setProperty("localization", "English");
            }

            File f = new File("config.properties");
            OutputStream out = new FileOutputStream(f);
            props.store(out, null);
            out.close();
            System.out.println(f.getAbsolutePath());
        } catch (Exception ex) {
            logger.error("Error saving configuration to file: ", ex);
        }
    }

    private void loadLocale() {
        Properties prop = new Properties();
        InputStream input = null;
        try {
            File f = new File("config.properties");
            input = new FileInputStream(f);
            prop.load(input);
            if (prop.getProperty("localization").equals("Czech")) {
                Locale.setDefault(new Locale("cs", "CZ"));
            } else {
                Locale.setDefault(new Locale("en", "GB"));
            }
            ResourceBundle.clearCache();
        } catch (Exception ex) {
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ex) {
                    logger.error("Error loading configuration from file: ", ex);
                    date = new Date();
                    logTextArea.insert(sdf.format(date) + " " + ResourceBundle.getBundle("cz/muni/fi/astrocamera/ui/Bundle").getString("AstroCameraUI.error.load"), 0);
                }
            }
        }
    }

    /**
     * Action performed on pressing Load from file button
     *
     * searches selected file for coordinates for object object name is in
     * searchNameTextField results are placed in selectedNameTextField,
     * raTextField and decTextField
     *
     * @param e
     */
    private void loadButtonActionPerformed(java.awt.event.ActionEvent e) {
        if (searchNameTextField.getText().length() > 1) {
            astroObject.setName(searchNameTextField.getText());
            astroObject.setDec(null);
            astroObject.setRa(null);
            astroObject = service.loadObjectFromFile(coordFilePath.toFile(), astroObject);
            selectedNameTextField.setText(astroObject.getName());
            raTextField.setText(astroObject.getRa());
            decTextField.setText(astroObject.getDec());
        }
    }

    /**
     * Action performed when Search button is pressed Searches for astronomical
     * object selected in searchNameTextField and puts results in
     * selectedNameTextField, raTextField and decTextField
     *
     * @param e
     */
    private void searchButtonActionPerformed(java.awt.event.ActionEvent e) {
        astroObject.setName(searchNameTextField.getText());
        astroObject.setDec(null);
        astroObject.setRa(null);
        astroObject = service.getObjectOnline(astroObject);
        selectedNameTextField.setText(astroObject.getName());
        raTextField.setText(astroObject.getRa());
        decTextField.setText(astroObject.getDec());
    }

    /**
     * Action performed when retrieveButton is pressed gets current coordinates
     * from telescope and save them to selected file under name given in
     * saveToFileTextField
     *
     * @param e
     */
    private void retrieveButtonActionPerformed(java.awt.event.ActionEvent e) {
        telescope.setAddress(serverIP);
        telescope.setPort(serverPort);
        String record = saveToFileTextField.getText() + telescopeControl.retrieveData(telescope, logTextArea);
        if (record.length() > saveToFileTextField.getText().length() + 1) {
            try (FileWriter fileWriter = new FileWriter(coordFilePath.toString(), true);
                    BufferedWriter bw = new BufferedWriter(fileWriter);
                    PrintWriter out = new PrintWriter(bw)) {
                out.println();
                out.print(record);
            } catch (IOException ex) {
                logger.error("Error while writing into file with coordinates: ", ex);
                date = new Date();
                logTextArea.insert(sdf.format(date) + " " + ResourceBundle.getBundle("cz/muni/fi/astrocamera/ui/Bundle").getString("AstroCameraUI.error.save"), 0);
            }
        } else {
            logger.error("Error while retrieving data from Telescope");
        }
    }

    private void setObserver() {
        teledata.setObserver(observerTextField.getText());
    }

    /**
     * Action performed when Confirm button is pressed Sends data to telescope
     *
     * @param e
     */
    private void confirmButtonActionPerformed(java.awt.event.ActionEvent e) {
        telescope.setAddress(serverIP);
        telescope.setPort(serverPort);
        fw.setName(selectedNameTextField.getText());
        telescopeControl.sendData(telescope, serverPassword, raTextField.getText(), decTextField.getText(), logTextArea);

    }
    
    private void clearLogButtonActionPerformed(java.awt.event.ActionEvent e) {
        logTextArea.setText("");

    }

    /**
     * Action performed when Browse button is pressed next to Photo folder
     *
     * Opens file chooser and lets user select directory where are fits files
     * created
     *
     * @param e
     */
    private void photoBrowseButtonActionPerformed(java.awt.event.ActionEvent e) {
        int returnVal = fcPhoto.showOpenDialog(AstroCameraUI.this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fcPhoto.getSelectedFile();
            fw.setReady(false);
            //Handle folder 
            photoTextField.setText(file.getAbsolutePath());
            Path p = Paths.get(file.getAbsolutePath());
            fw.setPath(p);
            saveProperties();
        } else {
            //aborted open folder
        }
    }

    /**
     * Action performed when Browse button next to Meteo DB file location is
     * pressed
     *
     * Opens file chooser and lets user select directory where are meteo db
     * files
     *
     * @param e
     */
    private void meteoBrowseButtonActionPerformed(java.awt.event.ActionEvent e) {
        int returnVal = fcMeteo.showOpenDialog(AstroCameraUI.this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fcMeteo.getSelectedFile();
            meteoDbServ.setReady(false);
            //Handle file 
            meteoTextField.setText(file.getAbsolutePath());
            Path p = Paths.get(file.getAbsolutePath());
            meteoDbServ.setPath(p);
            saveProperties();
        } else {
            //aborted open file
        }
    }

    /**
     * Action performed when Browse button next to Tele DB file location is
     * pressed
     *
     * Opens file chooser and lets user select directory where are telemetric db
     * files
     *
     * @param e
     */
    private void teleBrowseButtonActionPerformed(java.awt.event.ActionEvent e) {
        int returnVal = fcTele.showOpenDialog(AstroCameraUI.this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fcTele.getSelectedFile();
            teleDbServ.setReady(false);
            //Handle file 
            teleTextField.setText(file.getAbsolutePath());
            Path p = Paths.get(file.getAbsolutePath());
            teleDbServ.setPath(p);
            saveProperties();
        } else {
            //aborted open file
        }
    }

    /**
     * Action performed when Browse button next to Coordinates file location is
     * pressed
     *
     * Opens file chooser and lets user select directory where are telemetric db
     * files
     *
     * @param e
     */
    private void coordBrowseButtonActionPerformed(java.awt.event.ActionEvent e) {
        int returnVal = fcCoord.showOpenDialog(AstroCameraUI.this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fcCoord.getSelectedFile();
            //Handle file 
            coordTextField.setText(file.getAbsolutePath());
            Path p = Paths.get(file.getAbsolutePath());
            coordFilePath = p;
            saveProperties();
        } else {
            //aborted open file
        }
    }

    /**
     * Action performed when Confirm and start button is pressed
     *
     * Starts monitoring process of selected DB folders and folder with FITS
     * files
     *
     * @param e
     */
    private void startButtonActionPerformed(java.awt.event.ActionEvent e) {
        if ((fw.getPath() != null) && (meteoDbServ.getPath() != null)) {
            fw.setTextArea(logTextArea);
            teleDbServ.setTextArea(logTextArea);
            meteoDbServ.setTextArea(logTextArea);
            meteoDbServ.start();
            teleDbServ.start();
            fw.start();
            saveProperties();
        }
    }

    /**
     * Action performed when Stop button is pressed
     *
     * Stops monitoring process of selected DB folders and folder with FITS
     * files
     *
     * @param e
     */
    private void stopButtonActionPerformed(java.awt.event.ActionEvent e) {
        fw.stop();
        teleDbServ.setReady(false);
        meteoDbServ.setReady(false);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        mainPanel = new javax.swing.JPanel();
        searchNameLabel = new javax.swing.JLabel();
        searchNameTextField = new javax.swing.JTextField();
        searchButton = new javax.swing.JButton();
        objectPanel = new javax.swing.JPanel();
        selectedNameLabel = new javax.swing.JLabel();
        raLabel = new javax.swing.JLabel();
        decLabel = new javax.swing.JLabel();
        confirmButton = new javax.swing.JButton();
        selectedNameTextField = new javax.swing.JTextField();
        raTextField = new javax.swing.JTextField();
        decTextField = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        logTextArea = new javax.swing.JTextArea();
        loadButton = new javax.swing.JButton();
        retrieveButton = new javax.swing.JButton();
        saveToFileLabel = new javax.swing.JLabel();
        saveToFileTextField = new javax.swing.JTextField();
        observerLabel = new javax.swing.JLabel();
        observerTextField = new javax.swing.JTextField();
        versionLabel = new javax.swing.JLabel();
        clearLogButton = new javax.swing.JButton();
        AltFileNameCheckBox = new javax.swing.JCheckBox();
        settingsPanel = new javax.swing.JPanel();
        photoLabel = new javax.swing.JLabel();
        meteoLabel = new javax.swing.JLabel();
        teleLabel = new javax.swing.JLabel();
        photoTextField = new javax.swing.JTextField();
        meteoTextField = new javax.swing.JTextField();
        teleTextField = new javax.swing.JTextField();
        photoBrowseButton = new javax.swing.JButton();
        meteoBrowseButton = new javax.swing.JButton();
        teleBrowseButton = new javax.swing.JButton();
        startButton = new javax.swing.JButton();
        teleProgressBar = new javax.swing.JProgressBar();
        meteoProgressBar = new javax.swing.JProgressBar();
        stopButton = new javax.swing.JButton();
        coordLabel = new javax.swing.JLabel();
        coordTextField = new javax.swing.JTextField();
        coordBrowseButton = new javax.swing.JButton();
        languageLabel = new javax.swing.JLabel();
        languageComboBox = new javax.swing.JComboBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("cz/muni/fi/astrocamera/ui/Bundle"); // NOI18N
        setTitle(bundle.getString("AstroCameraUI.title")); // NOI18N

        searchNameLabel.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        searchNameLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        searchNameLabel.setText(bundle.getString("AstroCameraUI.searchNameLabel.text")); // NOI18N

        searchNameTextField.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N

        searchButton.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        searchButton.setText(bundle.getString("AstroCameraUI.searchButton.text")); // NOI18N
        searchButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchButtonActionPerformed(evt);
            }
        });

        objectPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), bundle.getString("AstroCameraUI.objectPanel.border.title"), javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 1, 12))); // NOI18N

        selectedNameLabel.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        selectedNameLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        selectedNameLabel.setText(bundle.getString("AstroCameraUI.selectedNameLabel.text")); // NOI18N

        raLabel.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        raLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        raLabel.setText(bundle.getString("AstroCameraUI.raLabel.text")); // NOI18N

        decLabel.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        decLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        decLabel.setText(bundle.getString("AstroCameraUI.decLabel.text")); // NOI18N

        confirmButton.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        confirmButton.setText(bundle.getString("AstroCameraUI.confirmButton.text")); // NOI18N
        confirmButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                confirmButtonActionPerformed(evt);
            }
        });

        selectedNameTextField.setEditable(false);
        selectedNameTextField.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        selectedNameTextField.setText(bundle.getString("AstroCameraUI.selectedNameTextField.text")); // NOI18N

        raTextField.setEditable(false);
        raTextField.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        raTextField.setText(bundle.getString("AstroCameraUI.raTextField.text")); // NOI18N

        decTextField.setEditable(false);
        decTextField.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        decTextField.setText(bundle.getString("AstroCameraUI.decTextField.text")); // NOI18N

        javax.swing.GroupLayout objectPanelLayout = new javax.swing.GroupLayout(objectPanel);
        objectPanel.setLayout(objectPanelLayout);
        objectPanelLayout.setHorizontalGroup(
            objectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(objectPanelLayout.createSequentialGroup()
                .addGap(41, 41, 41)
                .addGroup(objectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(confirmButton)
                    .addGroup(objectPanelLayout.createSequentialGroup()
                        .addGroup(objectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(raLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(selectedNameLabel))
                        .addGap(18, 18, 18)
                        .addGroup(objectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(raTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(selectedNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(decLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(decTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(64, Short.MAX_VALUE))
        );
        objectPanelLayout.setVerticalGroup(
            objectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, objectPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(objectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(selectedNameLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(selectedNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(objectPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(raLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(raTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(decLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(decTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(confirmButton)
                .addGap(43, 43, 43))
        );

        logTextArea.setColumns(20);
        logTextArea.setRows(5);
        jScrollPane1.setViewportView(logTextArea);

        loadButton.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        loadButton.setText(bundle.getString("AstroCameraUI.loadButton.text")); // NOI18N
        loadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadButtonActionPerformed(evt);
            }
        });

        retrieveButton.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        retrieveButton.setText(bundle.getString("AstroCameraUI.retrieveButton.text")); // NOI18N
        retrieveButton.setMaximumSize(new java.awt.Dimension(221, 25));
        retrieveButton.setMinimumSize(new java.awt.Dimension(221, 25));
        retrieveButton.setPreferredSize(new java.awt.Dimension(221, 25));
        retrieveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                retrieveButtonActionPerformed(evt);
            }
        });

        saveToFileLabel.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        saveToFileLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        saveToFileLabel.setText(bundle.getString("AstroCameraUI.saveToFileLabel.text")); // NOI18N

        saveToFileTextField.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N

        observerLabel.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        observerLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        observerLabel.setText(bundle.getString("AstroCameraUI.observerLabel.text")); // NOI18N
        observerLabel.setMaximumSize(new java.awt.Dimension(53, 25));
        observerLabel.setMinimumSize(new java.awt.Dimension(53, 25));
        observerLabel.setPreferredSize(new java.awt.Dimension(53, 25));

        versionLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        versionLabel.setText(bundle.getString("AstroCameraUI.versionLabel.text")); // NOI18N

        clearLogButton.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        clearLogButton.setText(bundle.getString("AstroCameraUI.clearLogButton.text")); // NOI18N
        clearLogButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearLogButtonActionPerformed(evt);
            }
        });

        AltFileNameCheckBox.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        AltFileNameCheckBox.setText(bundle.getString("AstroCameraUI.AltFileNameCheckBox.text")); // NOI18N

        javax.swing.GroupLayout mainPanelLayout = new javax.swing.GroupLayout(mainPanel);
        mainPanel.setLayout(mainPanelLayout);
        mainPanelLayout.setHorizontalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, mainPanelLayout.createSequentialGroup()
                                .addGap(25, 25, 25)
                                .addComponent(observerLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 68, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addGroup(mainPanelLayout.createSequentialGroup()
                                        .addComponent(observerTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(AltFileNameCheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(clearLogButton))
                                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 467, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(mainPanelLayout.createSequentialGroup()
                                .addGap(40, 40, 40)
                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addGroup(mainPanelLayout.createSequentialGroup()
                                        .addComponent(objectPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(14, 14, 14))
                                    .addGroup(mainPanelLayout.createSequentialGroup()
                                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(saveToFileLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addComponent(searchNameLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                        .addGap(18, 18, 18)
                                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(searchNameTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                                            .addComponent(saveToFileTextField))
                                        .addGap(18, 18, 18)
                                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addGroup(mainPanelLayout.createSequentialGroup()
                                                .addComponent(searchButton)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                                .addComponent(loadButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                            .addComponent(retrieveButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))))
                        .addGap(0, 140, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(versionLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        mainPanelLayout.setVerticalGroup(
            mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(mainPanelLayout.createSequentialGroup()
                .addGap(45, 45, 45)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(searchNameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(searchNameTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(searchButton, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(loadButton, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(saveToFileLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(saveToFileTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(retrieveButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 68, Short.MAX_VALUE)
                .addComponent(objectPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 175, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(mainPanelLayout.createSequentialGroup()
                        .addGap(34, 34, 34)
                        .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(observerLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(mainPanelLayout.createSequentialGroup()
                                .addGap(1, 1, 1)
                                .addGroup(mainPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(observerTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(clearLogButton)))))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, mainPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(AltFileNameCheckBox, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 249, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(25, 25, 25)
                .addComponent(versionLabel)
                .addContainerGap())
        );

        observerTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                setObserver();
            }
            public void removeUpdate(DocumentEvent e) {
                setObserver();
            }
            public void insertUpdate(DocumentEvent e) {
                setObserver();
            }

        });

        jTabbedPane1.addTab(bundle.getString("AstroCameraUI.mainPanel.TabConstraints.tabTitle"), mainPanel); // NOI18N

        photoLabel.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        photoLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        photoLabel.setText(bundle.getString("AstroCameraUI.photoLabel.text")); // NOI18N

        meteoLabel.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        meteoLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        meteoLabel.setText(bundle.getString("AstroCameraUI.meteoLabel.text")); // NOI18N

        teleLabel.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        teleLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        teleLabel.setText(bundle.getString("AstroCameraUI.teleLabel.text")); // NOI18N

        photoTextField.setEditable(false);
        photoTextField.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        photoTextField.setText(bundle.getString("AstroCameraUI.photoTextField.text")); // NOI18N

        meteoTextField.setEditable(false);
        meteoTextField.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        meteoTextField.setText(bundle.getString("AstroCameraUI.meteoTextField.text")); // NOI18N

        teleTextField.setEditable(false);
        teleTextField.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        teleTextField.setText(bundle.getString("AstroCameraUI.teleTextField.text")); // NOI18N

        photoBrowseButton.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        photoBrowseButton.setText(bundle.getString("AstroCameraUI.photoBrowseButton.text")); // NOI18N
        photoBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                photoBrowseButtonActionPerformed(evt);
            }
        });

        meteoBrowseButton.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        meteoBrowseButton.setText(bundle.getString("AstroCameraUI.meteoBrowseButton.text")); // NOI18N
        meteoBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                meteoBrowseButtonActionPerformed(evt);
            }
        });

        teleBrowseButton.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        teleBrowseButton.setText(bundle.getString("AstroCameraUI.teleBrowseButton.text")); // NOI18N
        teleBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                teleBrowseButtonActionPerformed(evt);
            }
        });

        startButton.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        startButton.setText(bundle.getString("AstroCameraUI.startButton.text")); // NOI18N
        startButton.setMaximumSize(new java.awt.Dimension(129, 25));
        startButton.setMinimumSize(new java.awt.Dimension(129, 25));
        startButton.setPreferredSize(new java.awt.Dimension(129, 25));
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });

        teleProgressBar.setMinimum(0);
        teleProgressBar.setMaximum(2);

        meteoProgressBar.setMinimum(0);
        meteoProgressBar.setMaximum(2);

        stopButton.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        stopButton.setText(bundle.getString("AstroCameraUI.stopButton.text")); // NOI18N
        stopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });

        coordLabel.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        coordLabel.setText(bundle.getString("AstroCameraUI.coordLabel.text")); // NOI18N

        coordTextField.setEditable(false);
        coordTextField.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        coordTextField.setText(bundle.getString("AstroCameraUI.coordTextField.text")); // NOI18N

        coordBrowseButton.setText(bundle.getString("AstroCameraUI.coordBrowseButton.text")); // NOI18N
        coordBrowseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                coordBrowseButtonActionPerformed(evt);
            }
        });

        languageLabel.setFont(new java.awt.Font("Tahoma", 0, 12)); // NOI18N
        languageLabel.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        languageLabel.setText(bundle.getString("AstroCameraUI.languageLabel.text")); // NOI18N

        languageComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "English", "Czech" }));
        languageComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                languageComboBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout settingsPanelLayout = new javax.swing.GroupLayout(settingsPanel);
        settingsPanel.setLayout(settingsPanelLayout);
        settingsPanelLayout.setHorizontalGroup(
            settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(settingsPanelLayout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(coordLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(meteoLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(photoLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(teleLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(languageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(photoTextField)
                    .addComponent(meteoTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                    .addComponent(teleTextField)
                    .addComponent(coordTextField)
                    .addComponent(languageComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(settingsPanelLayout.createSequentialGroup()
                        .addComponent(startButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(stopButton, javax.swing.GroupLayout.PREFERRED_SIZE, 90, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(settingsPanelLayout.createSequentialGroup()
                        .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(photoBrowseButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(teleBrowseButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(meteoBrowseButton, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(meteoProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(teleProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(coordBrowseButton))
                .addContainerGap(100, Short.MAX_VALUE))
        );
        settingsPanelLayout.setVerticalGroup(
            settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(settingsPanelLayout.createSequentialGroup()
                .addGap(49, 49, 49)
                .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(photoLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(photoTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(photoBrowseButton, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(35, 35, 35)
                .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(settingsPanelLayout.createSequentialGroup()
                        .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(meteoLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(meteoTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(meteoBrowseButton, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(35, 35, 35)
                        .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(settingsPanelLayout.createSequentialGroup()
                                .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(teleLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(teleTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(teleBrowseButton, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(35, 35, 35)
                                .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(coordTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 25, Short.MAX_VALUE)
                                        .addComponent(coordBrowseButton, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(coordLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(35, 35, 35)
                                .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(languageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(languageComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addComponent(teleProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(meteoProgressBar, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(55, 55, 55)
                .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(startButton, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(stopButton, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(338, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab(bundle.getString("AstroCameraUI.settingsPanel.TabConstraints.tabTitle"), settingsPanel); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void languageComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_languageComboBoxActionPerformed
        if (languageComboBox.getSelectedIndex() == 1) {
            Locale.setDefault(new Locale("cs", "CZ"));
        } else {
            Locale.setDefault(new Locale("en", "GB"));
        }
        ResourceBundle.clearCache();
        saveProperties();

    }//GEN-LAST:event_languageComboBoxActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(AstroCameraUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(AstroCameraUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(AstroCameraUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(AstroCameraUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new AstroCameraUI().setVisible(true);
            }
        });

    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox AltFileNameCheckBox;
    private javax.swing.JButton clearLogButton;
    private javax.swing.JButton confirmButton;
    private javax.swing.JButton coordBrowseButton;
    private javax.swing.JLabel coordLabel;
    private javax.swing.JTextField coordTextField;
    private javax.swing.JLabel decLabel;
    private javax.swing.JTextField decTextField;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JComboBox languageComboBox;
    private javax.swing.JLabel languageLabel;
    private javax.swing.JButton loadButton;
    private javax.swing.JTextArea logTextArea;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JButton meteoBrowseButton;
    private javax.swing.JLabel meteoLabel;
    private javax.swing.JProgressBar meteoProgressBar;
    private javax.swing.JTextField meteoTextField;
    private javax.swing.JPanel objectPanel;
    private javax.swing.JLabel observerLabel;
    private javax.swing.JTextField observerTextField;
    private javax.swing.JButton photoBrowseButton;
    private javax.swing.JLabel photoLabel;
    private javax.swing.JTextField photoTextField;
    private javax.swing.JLabel raLabel;
    private javax.swing.JTextField raTextField;
    private javax.swing.JButton retrieveButton;
    private javax.swing.JLabel saveToFileLabel;
    private javax.swing.JTextField saveToFileTextField;
    private javax.swing.JButton searchButton;
    private javax.swing.JLabel searchNameLabel;
    private javax.swing.JTextField searchNameTextField;
    private javax.swing.JLabel selectedNameLabel;
    private javax.swing.JTextField selectedNameTextField;
    private javax.swing.JPanel settingsPanel;
    private javax.swing.JButton startButton;
    private javax.swing.JButton stopButton;
    private javax.swing.JButton teleBrowseButton;
    private javax.swing.JLabel teleLabel;
    private javax.swing.JProgressBar teleProgressBar;
    private javax.swing.JTextField teleTextField;
    private javax.swing.JLabel versionLabel;
    // End of variables declaration//GEN-END:variables
}
