package audio_core;

import javax.sound.sampled.LineUnavailableException;

public class audio_buffer {
    public int SAMPLE_RATE;
    public AudioPlayer player;



    public audio_buffer(int SAMPLE_RATE) throws LineUnavailableException {
        this.SAMPLE_RATE = SAMPLE_RATE;
        this.player = new AudioPlayer(this.SAMPLE_RATE);
        player.openLine();
    }
    public void play_bytes(byte[] bytes){
        player.playBytes(bytes);
    }
}
