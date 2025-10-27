
import audio_core.wave_script;
import audio_core.presets.sine;
import audio_core.audio_buffer;
import audio_core.sequencer;
import javax.sound.sampled.LineUnavailableException;

import ui.text_ui;



//TODO: User interface
//TODO: Documentation
//TODO: Orginisation
//TODO: Saving and loading
void main() throws LineUnavailableException {
    int SAMPLE_RATE = 44100;
    wave_script play_back = new sine();

    //play_back.note_start(new Note(69));
    audio_buffer a_buf = new audio_buffer(SAMPLE_RATE);

    sequencer seq = new sequencer(play_back);


    //TextWindowConsole console = new TextWindowConsole("Custom Java Console - Arrow Key Input");
    text_ui ui = new text_ui();
    //text_ui.setup(console);

    seq.sequence[2][1] = 69;
    seq.sequence_active[2][1] = true;
    seq.sequence[3][1] = 69;
    seq.sequence_active[3][1] = true;
    seq.sequence_active[3][2] = true;
    seq.sequence[3][2] = 69+12;

    ui.notes[0]= seq.sequence;

    ui.console.setSpaceTaskFactory(() -> new TimerTask(){
        public void play(){
        byte[] data = play_back.generate(SAMPLE_RATE);



        a_buf.play_bytes(data);

        }

        public void run(){
            for (int i =0; i<16;i++){
                play();
                seq.call_note_sequence(i);
            }
        }
    });

    //.space_task = task;

    //task.run();



    //System.out.println("data");

    //for (byte value:data){
    //System.out.println(value);}
}
