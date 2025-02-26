package audiofingerprinter;

import java.io.File;
import java.util.*;

public class AudioFingerprint implements AudioFingerprinter {
private SongDatabase songDB;
private static final int FUZ_FACTOR = 2;



    public AudioFingerprint(SongDatabase songDB){
        this.songDB = songDB;
    }

    @Override
    public SongDatabase getSongDB() {
      return this.songDB;
    }

    
    public List<String> recognize(byte[] audioData) {
       Map<Integer,Map<Long,Integer>> matchCounts = new HashMap<>();
       double[][] freqData = songDB.convertToFrequencyDomain(audioData);
       long[][] keyPoints = determineKeyPoints(freqData);
       for (int i = 0; i < keyPoints.length; i++) {
        long[] points = keyPoints[i];
        long hash = hash(points);
        List<DataPoint> dataPoints = songDB.getMatchingPoints(hash);
        if(dataPoints != null){
            for (DataPoint point : dataPoints) {
                long offSet = point.getTime() - i;
                int songID = point.getSongId();
                Map<Long, Integer> innMap = matchCounts.computeIfAbsent(songID, k -> new HashMap<>());
                if(innMap == null){
                    innMap = matchCounts.get(songID);
                }
                innMap.put(offSet, innMap.getOrDefault(offSet, 0) + 1);
            }
        }
       }
       List<SongRecognizer> results = new ArrayList<>();
       for(Map.Entry<Integer,Map<Long, Integer>> entry : matchCounts.entrySet()){
        int songID = entry.getKey();
        int match = 0;
        for(int count: entry.getValue().values()){
            if(count > match){
                match = count;
            }  
        }
        String name = songDB.getSongName(songID);
        results.add(new SongRecognizer(name, match));
       }
       Collections.sort(results);
       List<String> sortedResults = new ArrayList<>();
       for (SongRecognizer result : results) {
        sortedResults.add(result.toString());
       }
       return sortedResults;
    }

    @Override
    public List<String> recognize(File fileIn) {
       return recognize(songDB.getRawData(fileIn));
    }

    @Override
    public long[][] determineKeyPoints(double[][] results) {
        long[][] keyPoints = new long[results.length][4];
        for (int i = 0; i < results.length; i++) {
            double[] time = results[i];
            double[] mag = new double[time.length/2];
            for (int freq= 0; freq < mag.length; freq++) {
                double re = time[2*freq];
                double im = time[2*freq+1];
                 mag[freq] = Math.log(Math.sqrt(re * re + im * im) + 1);
            }
            keyPoints[i] = new long[]{
                getMaxFreq(mag, 40, 80),
                getMaxFreq(mag, 80, 120),
                getMaxFreq(mag, 120, 180),
                getMaxFreq(mag, 180, 300),
            };

        }
        return keyPoints;
    }

    private long getMaxFreq(double[] mag, int minFreq, int maxFreq){
        double maxMag = Double.NEGATIVE_INFINITY;
        int bestFreq = minFreq;
        for (int i = minFreq; i < maxFreq; i++) {
            if(i < mag.length && mag[i] > maxMag){
                maxMag = mag[i];
                bestFreq = i;
            }
        }
        return bestFreq;
    }

    private long hash(long p1, long p2, long p3, long p4) {
        return (p4 - (p4 % FUZ_FACTOR)) * 100000000 + (p3 - (p3 % FUZ_FACTOR))
                * 100000 + (p2 - (p2 % FUZ_FACTOR)) * 100
                + (p1 - (p1 % FUZ_FACTOR));
    }

    public long hash(long[] points) {
       if(points.length != 4){
        throw new IllegalArgumentException("Points array must be at least of length 4");
       }
       return hash(points[0], points[1], points[2], points[3]);
    }
    
}
