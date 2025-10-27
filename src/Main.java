
import audio_core.wave_script;
import audio_core.presets.sine;
import audio_core.audio_buffer;
import audio_core.sequencer;
import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;

import ui.TextWindowConsole;
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



    ui.console.setSpaceTaskFactory(() -> new Runnable(){
        boolean playing = true;
        public void play(){
        byte[] data = play_back.generate(SAMPLE_RATE);



        a_buf.play_bytes(data);

        }

        public void run(){

            for (int i =0; i<16;i++){
                System.out.println(!Thread.currentThread().isInterrupted());
                playing = !Thread.currentThread().isInterrupted();
                if (playing==false){
                    return;
                }
                play();
                seq.call_note_sequence(i);
            }

        }
    });

    SwingUtilities.invokeLater(() -> {

        int set = 0;
        int row = 0;


        ui.console.println("--- Welcome to the Custom Console ---");
        ui.console.println("Press the Up, Down, Left, or Right arrow keys to test input capture.");
        ui.console.println("Press ESC to stop the background simulation.");
        ui.console.println("-------------------------------------");

        // 🆕 Set the factory for the space task, now providing a Runnable

        ui.console.println("Press SPACE to start/stop the non-blocking Thread (runs every 500ms).");
        ui.console.println("-------------------------------------");

        // --- Demonstration of background process output (existing simulation log) ---
        new Thread(() -> {
            int count = 0;
            while (ui.console.isRunning()) {
                try {
                    Thread.sleep(10); // Wait 2 seconds
                    //ui.console.println(String.format("[LOG] Simulation step %d complete.", ++count));
                    ui.render_notes(set,row);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            ui.console.println("Simulation stopped.");
        }).start();
    });

    //.space_task = task;

    //task.run();



    //System.out.println("data");

    //for (byte value:data){
    //System.out.println(value);}
}
