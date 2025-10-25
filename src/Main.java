
import audio_core.wave_script;
import audio_core.Note;
import audio_core.presets.sine;

void main() {
    wave_script play_back = new sine();

    play_back.note_start(new Note(69));

    byte[] data = play_back.generate(16);
    //System.out.println("data");

    //for (byte value:data){
    //System.out.println(value);}
}
