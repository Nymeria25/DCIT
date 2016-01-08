package distributed_system_rpc;

import java.util.ArrayList;
import java.util.Arrays;

/**
 *
 * @author Lavinia
 */
public class Dictionary {
    public Dictionary() {        
        words_ = new ArrayList<>(Arrays.asList("flower", "dreamer", "like",
                "people", "pies", "world", "crazy", "go", "sometimes", "slow",
                "turn", "care", "halfway", "soak", "magic", "story", "waters",
                "ocean", "rock", "yes", "true", "rules", "straw", "marry",
                "trumpet", "plastic", "unstable", "fingerprint", "card", "name",
                "better", "roses", "blue", "teller", "adjusted", "ask",
                "escape", "debatable", "play", "harmony", "sweet", "supernatural",
                "chameleon", "future", "look", "wild", "away", "miles", "floor",
                "hazy", "nothing", "walk", "mister", "other", "stand", "near",
                "space", "mention", "wire"));
        size_ = words_.size();
        sentence_ = "";
    }
    
    public String getRandomWord() {
        int index = 0 + (int) (Math.random() * ((size_ - 1) + 1));
        String word = words_.get(index);
        appendString(word);
        return word;
    }
    
    private void appendString(String word) {
        sentence_ += word;
    }
    
    private ArrayList<String> words_;
    private int size_;
    private String sentence_;
}
