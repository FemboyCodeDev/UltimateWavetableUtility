
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
        TextWindowConsole console = new TextWindowConsole("Custom Java Console - Arrow Key Input");

        console.println("--- Welcome to the Custom Console ---");
        console.println("Press the Up, Down, Left, or Right arrow keys to test input capture.");
        console.println("Press ESC to stop the background simulation.");
        console.println("-------------------------------------");

        // 🆕 Set the factory for the space task, now providing a Runnable
        console.setSpaceTaskFactory(() -> new Runnable() {
            private int counter = 0;
            @Override
            public void run() {
                // This code runs on the dedicated "SpaceTaskThread".
                try {
                    // The loop should continue *until* the thread is interrupted
                    while (!Thread.currentThread().isInterrupted()) {
                        // Non-blocking task logic
                        console.println("[SPACE TASK] Running non-blockingly! Count: " + ++counter);

                        // The pause should be inside the try block to catch the InterruptedException
                        // when the main thread calls thread.interrupt()
                        Thread.sleep(500); // Wait 500 milliseconds (0.5s)
                    }
                } catch (InterruptedException e) {
                    // This is the clean way to stop the thread when 'thread.interrupt()' is called.
                    // Re-interrupt the thread for higher-level interrupt handlers (optional for this demo)
                    Thread.currentThread().interrupt();
                    // Fall through to finally or just exit the method.
                    console.println("[SPACE TASK] Interrupted/Stopped gracefully.");
                }
            }
        });

        console.println("Press SPACE to start/stop the non-blocking Thread (runs every 500ms).");
        console.println("-------------------------------------");

        // --- Demonstration of background process output (existing simulation log) ---
        new Thread(() -> {
            int count = 0;
            while (console.isRunning()) {
                try {
                    Thread.sleep(2000); // Wait 2 seconds
                    console.println(String.format("[LOG] Simulation step %d complete.", ++count));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            console.println("Simulation stopped.");
        }).start();
    });

    //.space_task = task;

    //task.run();



    //System.out.println("data");

    //for (byte value:data){
    //System.out.println(value);}
}
