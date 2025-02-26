


public class SongRecognizer implements Comparable<SongRecognizer> {
    private String songName;
    private int matchCount;
    
    public SongRecognizer(String songName, int matchCount) {
        this.songName = songName;
     this.matchCount = matchCount;
        }
    
    public String getSongName() {
        return songName;
        }
    
    public int getMatchCount() {
        return matchCount;
        }
    
    @Override
    public int compareTo(SongRecognizer other) {
        return Integer.compare(other.matchCount, this.matchCount);
        }
    
    @Override
    public String toString() {
        return songName + " (matches: " + matchCount + ")";
        }
    }
    
