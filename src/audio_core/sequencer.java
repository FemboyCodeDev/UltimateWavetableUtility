package audio_core;
import audio_core.Note;
public class sequencer {
    wave_script gen_script;
    int[] notes_playing = new int[4];
    byte[] velocity = new byte[4];
    public int[][] sequence = new int[16][4];
    public byte[][] sequence_velocity = new byte[16][4];
    public sequencer(wave_script script){
        this.gen_script = script;
    }
    public void call_note_sequence(int t){
        for (int i=0; i<4;i++){
        if (notes_playing[i] != sequence[t][i]){
            int note_index = gen_script.get_note_index(new Note(notes_playing[i]));
            if (note_index!=-1) {
                gen_script.remove_note(note_index);
            }
            gen_script.note_start(new Note(sequence[t][i],sequence_velocity[t][i]));
            notes_playing[i] = sequence[t][i];

        }

        }
        System.out.println(notes_playing);
    }
}
