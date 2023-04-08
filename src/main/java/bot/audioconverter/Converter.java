package bot.audioconverter;

import java.io.*;

public class Converter {

    public File convertOgaToWav(File ogaFile) throws Exception {
        File wavFile = File.createTempFile("fromOga23", ".wav");
        ProcessBuilder processBuilder = new ProcessBuilder(
                "ffmpeg", "-y",
                "-i", ogaFile.getAbsolutePath(),
                "-acodec", "pcm_s16le",
                "-ac", "1",
                "-ar", "44100",
                "-f", "wav", wavFile.getAbsolutePath()
        );

        Process process = processBuilder.start();
        int exitValue = process.waitFor();

        //ffmpeg -y -i file_11.oga -acodec pcm_s16le -ac 1 -ar 44100 -f wav temp.wav

        if (exitValue != 0) {
            throw new Exception("Failed to convert oga to wav");
        }
        return wavFile;
    }

}