package audiofingerprinter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class AudioFingerprinterTest {

    private AudioFingerprinter audioFingerprinter;
    private SongDatabase songDB;

    @BeforeEach
   public void setup(){
        songDB = new SongDatabase();

        audioFingerprinter = new AudioFingerprint(songDB); 

        songDB.setFingerprinter(audioFingerprinter);
        
    }

    @Test
    public void testDetermineKeyPoints(){
        double[][] rawData = new double[2][800]; // two time steps with frequencies 0-400hz
        double[] realPartsTimeZero = IntStream.range(0, 400).asDoubleStream().toArray();
        double[] realPartsTimeOne = IntStream.range(0, 400).map(i -> 400 - i).asDoubleStream().toArray();

        for(int i=0, j=0; i < rawData[0].length; i+=2, j++){
            rawData[0][i] = realPartsTimeZero[j];
            rawData[0][i+1] = 0; // Keep the imaginary part zero

            rawData[1][i] = realPartsTimeOne[j];
            rawData[1][i+1] = 0; // Keep the imaginary part zero
        }

        long[][] result = audioFingerprinter.determineKeyPoints(rawData);
        assertArrayEquals(new long[][]{{79, 119, 179, 299}, {40, 80, 120, 180}}, result);
    }

    @Test
    public void testHash(){
        long[] input = {10, 20, 30, 40};
        long hash = audioFingerprinter.hash(input);
        assertEquals(4003002010L, hash); // the L at the end makes it a long instead of an int literal
        long[] input2 = {11, 21, 31, 41};
        hash = audioFingerprinter.hash(input2);
        assertEquals(4003002010L, hash); // fuzz factor works.
        long[] input3 = {12, 22, 32, 42};
        hash = audioFingerprinter.hash(input3);
        assertEquals(4203202212L, hash);
    }

    @Test
    public void testRecognize(){

        // This test the overall algorithm. Note we are using raw byte files rather than mp3s because
        // loading the mp3s requires sampling which can cause the byte output, and thus the number of matches
        // to vary each run.
        try{
            URL musicDirectory = SimpleFingerprinter.class.getResource("/raw-byte-files");
            File path = new File(musicDirectory.toURI());
            File[] audioFiles = getRawFilesFromDirectory(path);
            System.out.println("Found "+audioFiles.length+" files.");
            for(int i=0; i < audioFiles.length; i++){
                byte[] rawAudioData = Files.readAllBytes(audioFiles[i].toPath());
                songDB.processAudioData(rawAudioData, audioFiles[i].getName());
            }

            URL song = SimpleFingerprinter.class.getResource("/raw-byte-files/CarolOfTheBells.raw");
            File fileIn = new File(song.toURI());
            byte[] rawAudioData = Files.readAllBytes(fileIn.toPath());
            List<String> results = audioFingerprinter.recognize(rawAudioData);
            assertTrue(results.get(0).startsWith("CarolOfTheBells")); 
            assertTrue(results.get(0).contains("1015")); //Should have 1015 matches.
            assertTrue(results.get(1).startsWith("JoyToTheWorld"));
            assertTrue(results.get(1).contains("4")); //Should have 4 matches.
            assertTrue(results.get(2).startsWith("AdagioInC"));
            assertTrue(results.get(2).contains("2")); //Should have 2 matches.
            assertTrue(results.get(3).startsWith("AllaWhatParody"));
            assertTrue(results.get(3).contains("1")); //Should have 1 match.
        } catch(URISyntaxException e){
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Returns an array of file objects containing the raw files that are found in the directory
     * @param directory to search
     * @return array of raw files from the directory.
     */
    private File[] getRawFilesFromDirectory(File directory){
        if (directory.isDirectory()) {

            return directory.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String filename) {
                    return filename.endsWith(".raw");
                }
            });
        }
        else {
            // if directory is not actually a directory or doesn't exist, return a zero length array.
            return new File[0];
        }

    }
}
