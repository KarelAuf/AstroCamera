package cz.muni.fi.astrocamera.coordinates;

import cz.muni.fi.astrocamera.entity.AstronomicalObject;
import java.io.File;

/**
 *
 * @author Karel Auf
 */
public interface AstroObjectService {

    /**
     * Gets Right Ascension (RA) and Declination (Dec) for single Astronomical
     * Object
     *
     * @param astroObject AstronomicalObject with filled name
     * @return AstronomicalObject with filled name, RA and DEC or only name in
     * case of any error
     */
    public AstronomicalObject getObjectOnline(AstronomicalObject astroObject);

    /**
     * Gets Right Ascension (RA) and Declination (Dec) for given name
     *
     * @param file file with coordinates in format name#RA#Dec#\n
     * @param name name of searched object
     * @return AstronomicalObject with filled name and coordinates if found
     * otherwise only with filled name
     */
    public AstronomicalObject loadObjectFromFile(File file, AstronomicalObject astroObject);

}
