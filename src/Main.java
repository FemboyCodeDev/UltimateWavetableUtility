
import audio_core.wave_script;
import audio_core.Note;
import audio_core.presets.sine;
import audio_core.audio_buffer;

import javax.sound.sampled.LineUnavailableException;

void main() throws LineUnavailableException {
    int SAMPLE_RATE = 44100;
    wave_script play_back = new sine();

    play_back.note_start(new Note(69));
    audio_buffer a_buf = new audio_buffer(SAMPLE_RATE);



    TimerTask task = new TimerTask(){
        public void play(){
        byte[] data = play_back.generate(SAMPLE_RATE);



        a_buf.play_bytes(data);}

        public void run(){
            for (int i =0; i<16;i++){
                play();
            }
        }
    };

    task.run();



    //System.out.println("data");

    //for (byte value:data){
    //System.out.println(value);}
}
