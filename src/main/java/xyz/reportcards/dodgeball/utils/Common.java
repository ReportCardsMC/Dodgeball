package xyz.reportcards.dodgeball.utils;

import java.util.Random;

public class Common {

    private static final String[] WORDS = {"Alpha", "Beta", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel", "India", "Juliet", "Kilo", "Lima", "Mike", "November", "Oscar"};
    private static final String[] CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".split("");
    private static final Random RANDOM = new Random();

    /**
     * Generates a random word from the WORDS array
     * @return The generated word
     */
    public static String randomWord() {
        return WORDS[RANDOM.nextInt(WORDS.length)];
    }

    /**
     * Generates a random ID using a random word mixed with a random string of characters
     * @param length The length of the random string of characters
     * @return The generated ID
     */
    public static String generateID(int length) {
        String word = randomWord();
        StringBuilder randomString = new StringBuilder();
        for (int i = 0; i < length; i++) {
            randomString.append(CHARACTERS[RANDOM.nextInt(CHARACTERS.length)]);
        }
        return word + "-" + randomString;
    }

    public static String convertSecondsToTimespan(int seconds) {
        int hours = seconds / 3600;
        int remainder = seconds - hours * 3600;
        int mins = remainder / 60;
        int secs = remainder - mins * 60;
        return (hours > 0 ? hours + "h " : "") + (mins > 0 ? mins + "m " : "") + secs + "s";
    }

}
