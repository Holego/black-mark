package com.goshan.blackmark.util;

import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The dead man speaks through the bearer. Words come out of their mouth that they did not choose.
 * <p>
 * Substitution is by whole word, so the sentence stays readable - the bearer is possessed,
 * not unintelligible. The oaths are the salty kind a sailor swears by, which keeps this usable
 * on a public server.
 */
public final class PirateTongue {

    private static final Map<String, String> WORDS = new HashMap<>();

    private static final String[] OATHS_RU = {
            "Аррр!",
            "Йо-хо-хо!",
            "Тысяча чертей!",
            "Разрази меня гром!",
            "Клянусь якорем!",
            "Чтоб мне провалиться!",
            "Свистать всех наверх!",
            "Каракатица мне в глотку!",
            "Тьфу ты, морская соль!",
            "Якорь мне в печёнку!",
    };

    private static final String[] OATHS_EN = {
            "Arrr!",
            "Yo-ho-ho!",
            "Blast me barnacles!",
            "Shiver me timbers!",
            "By Davy Jones!",
            "Avast!",
            "Blow me down!",
            "Scupper that!",
    };

    static {
        // --- Russian -------------------------------------------------------
        WORDS.put("привет", "здорово");
        WORDS.put("здравствуйте", "доброго ветра");
        WORDS.put("пока", "попутного ветра");
        WORDS.put("да", "аррр");
        WORDS.put("нет", "ни в жисть");
        WORDS.put("друг", "салага");
        WORDS.put("друзья", "салаги");
        WORDS.put("чувак", "салага");
        WORDS.put("ребят", "салаги");
        WORDS.put("ребята", "салаги");
        WORDS.put("парни", "морские волки");
        WORDS.put("деньги", "дублоны");
        WORDS.put("золото", "золотишко");
        WORDS.put("алмаз", "самоцвет");
        WORDS.put("алмазы", "самоцветы");
        WORDS.put("еда", "провиант");
        WORDS.put("вода", "солёная водица");
        WORDS.put("дом", "порт");
        WORDS.put("база", "гавань");
        WORDS.put("иду", "держу курс");
        WORDS.put("идём", "держим курс");
        WORDS.put("пошли", "поднимай якорь");
        WORDS.put("смотри", "гляди в оба");
        WORDS.put("смотрите", "глядите в оба");
        WORDS.put("хорошо", "добро");
        WORDS.put("плохо", "худо");
        WORDS.put("быстро", "живо");
        WORDS.put("стой", "стоп машина");
        WORDS.put("помоги", "полундра");
        WORDS.put("помогите", "полундра");
        WORDS.put("что", "чаво");
        WORDS.put("где", "куды");
        WORDS.put("почему", "с какой стати");
        WORDS.put("спасибо", "премного благодарен");
        WORDS.put("умер", "пошёл на дно");
        WORDS.put("умру", "пойду на дно");
        WORDS.put("убил", "отправил к Дэви Джонсу");
        WORDS.put("враг", "вражина");
        WORDS.put("враги", "вражины");
        WORDS.put("лодка", "шлюпка");
        WORDS.put("корабль", "бриг");
        WORDS.put("сундук", "рундук");
        WORDS.put("меч", "сабля");
        WORDS.put("топор", "абордажный топор");
        WORDS.put("я", "я, старый морской волк,");

        // --- English -------------------------------------------------------
        WORDS.put("hello", "ahoy");
        WORDS.put("hi", "ahoy");
        WORDS.put("hey", "ahoy");
        WORDS.put("bye", "fair winds");
        WORDS.put("yes", "aye");
        WORDS.put("yeah", "aye");
        WORDS.put("no", "nay");
        WORDS.put("friend", "matey");
        WORDS.put("friends", "mateys");
        WORDS.put("guys", "mateys");
        WORDS.put("man", "matey");
        WORDS.put("you", "ye");
        WORDS.put("your", "yer");
        WORDS.put("yours", "yers");
        WORDS.put("my", "me");
        WORDS.put("is", "be");
        WORDS.put("are", "be");
        WORDS.put("am", "be");
        WORDS.put("money", "doubloons");
        WORDS.put("gold", "booty");
        WORDS.put("food", "grub");
        WORDS.put("home", "port");
        WORDS.put("base", "harbour");
        WORDS.put("stop", "avast");
        WORDS.put("look", "look sharp");
        WORDS.put("help", "all hands");
        WORDS.put("died", "went to Davy Jones");
        WORDS.put("dead", "food for the fishes");
        WORDS.put("boat", "dinghy");
        WORDS.put("ship", "brig");
        WORDS.put("chest", "sea chest");
        WORDS.put("sword", "cutlass");
    }

    /**
     * @param shout when true the dead man does not talk, he bellows
     */
    public static String speak(String input, RandomSource random, boolean shout) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String spoken = substitute(input);

        // An oath on the front, more often than not.
        if (random.nextFloat() < 0.55F) {
            spoken = oath(random, spoken) + ' ' + spoken;
        } else if (random.nextFloat() < 0.3F) {
            spoken = spoken + ' ' + oath(random, spoken);
        }

        if (shout) {
            spoken = spoken.toUpperCase(Locale.ROOT);
            if (!spoken.endsWith("!")) {
                spoken = spoken + "!!!";
            }
        }
        return spoken;
    }

    private static String oath(RandomSource random, String context) {
        String[] pool = hasCyrillic(context) ? OATHS_RU : OATHS_EN;
        return pool[random.nextInt(pool.length)];
    }

    public static boolean hasCyrillic(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 0x0400 && c <= 0x04FF) {
                return true;
            }
        }
        return false;
    }

    /**
     * Walks the text in runs of letters and non-letters, so punctuation survives and word
     * boundaries work the same in Cyrillic as in Latin.
     */
    private static String substitute(String input) {
        StringBuilder out = new StringBuilder(input.length() + 16);
        int i = 0;

        while (i < input.length()) {
            if (!Character.isLetter(input.charAt(i))) {
                out.append(input.charAt(i));
                i++;
                continue;
            }

            int start = i;
            while (i < input.length() && Character.isLetter(input.charAt(i))) {
                i++;
            }
            String word = input.substring(start, i);
            String replacement = WORDS.get(word.toLowerCase(Locale.ROOT));
            out.append(replacement == null ? word : matchCase(word, replacement));
        }
        return out.toString();
    }

    private static String matchCase(String original, String replacement) {
        if (original.length() > 1 && original.equals(original.toUpperCase(Locale.ROOT))) {
            return replacement.toUpperCase(Locale.ROOT);
        }
        if (Character.isUpperCase(original.charAt(0))) {
            return Character.toUpperCase(replacement.charAt(0)) + replacement.substring(1);
        }
        return replacement;
    }

    private PirateTongue() {
    }
}
